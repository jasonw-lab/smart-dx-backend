#!/usr/bin/env python3
"""
AI Review Cycle - Claude と Codex の連携レビュースクリプト

使い方:
    python scripts/ai_review_cycle.py <レビュー名> "<対象ファイルパターン>"

例:
    python scripts/ai_review_cycle.py batch5-e2e "app/src/test/java/com/smartdx/app/*Test.java"

環境変数:
    MAX_ROUNDS: 最大レビューラウンド数 (デフォルト: 3)
"""

import subprocess
import sys
import os
import json
import re
import time
from datetime import datetime
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import Optional


# =============================================================================
# Configuration
# =============================================================================

@dataclass
class Config:
    review_name: str
    target_files: str
    max_rounds: int = 3
    project_root: Path = Path(__file__).parent.parent
    review_dir: Path = None
    timestamp: str = None

    def __post_init__(self):
        self.timestamp = datetime.now().strftime("%m%d_%H%M")
        self.review_dir = self.project_root / "review"
        self.review_dir.mkdir(exist_ok=True)

    @property
    def review_file(self) -> Path:
        return self.review_dir / f"review_{self.timestamp}_{self.review_name}.md"

    @property
    def session_file(self) -> Path:
        return self.project_root / f".ai-review-session-{self.timestamp}.json"


@dataclass
class SessionState:
    codex_session_id: Optional[str] = None
    claude_session_id: Optional[str] = None
    current_round: int = 0
    review_file: str = ""

    def save(self, path: Path):
        with open(path, "w") as f:
            json.dump(asdict(self), f, indent=2)

    @classmethod
    def load(cls, path: Path) -> "SessionState":
        if path.exists():
            with open(path) as f:
                return cls(**json.load(f))
        return cls()


# =============================================================================
# Logging
# =============================================================================

class Colors:
    RED = "\033[0;31m"
    GREEN = "\033[0;32m"
    YELLOW = "\033[1;33m"
    BLUE = "\033[0;34m"
    CYAN = "\033[0;36m"
    NC = "\033[0m"


def log_info(msg: str):
    print(f"{Colors.BLUE}[INFO]{Colors.NC} {msg}")


def log_success(msg: str):
    print(f"{Colors.GREEN}[SUCCESS]{Colors.NC} {msg}")


def log_warn(msg: str):
    print(f"{Colors.YELLOW}[WARN]{Colors.NC} {msg}")


def log_error(msg: str):
    print(f"{Colors.RED}[ERROR]{Colors.NC} {msg}")


def log_step(msg: str):
    print(f"{Colors.CYAN}[STEP]{Colors.NC} {msg}")


# =============================================================================
# Review File Management
# =============================================================================

def init_review_file(config: Config) -> None:
    """レビューファイルを初期化"""
    content = f"""# AI連携レビュー: {config.review_name}

## メタ情報
- 作成日時: {datetime.now().strftime('%Y-%m-%d %H:%M')}
- 対象ファイル: {config.target_files}
- 最大ラウンド: {config.max_rounds}
- セッションファイル: {config.session_file}

---

"""
    config.review_file.write_text(content, encoding="utf-8")
    log_success(f"Review file created: {config.review_file}")


def count_issues(review_file: Path) -> dict:
    """レビューファイルから指摘数をカウント"""
    if not review_file.exists():
        return {"high": 0, "high_fixed": 0, "total": 0}

    content = review_file.read_text(encoding="utf-8")

    high_count = len(re.findall(r"重大度:\s*High", content))
    high_fixed = len(re.findall(r"\|\s*High.*✅", content))
    total = len(re.findall(r"重大度:\s*(High|Medium|Low)", content))

    return {
        "high": high_count,
        "high_fixed": high_fixed,
        "high_remaining": high_count - high_fixed,
        "total": total
    }


# =============================================================================
# AI Execution
# =============================================================================

def run_codex(prompt: str, session_id: Optional[str], project_root: Path) -> tuple[str, Optional[str]]:
    """Codex を実行し、出力とセッションIDを返す"""

    os.chdir(project_root)

    if session_id:
        log_info(f"Resuming Codex session: {session_id}")
        cmd = ["codex", "resume", session_id]
        # resume の場合は stdin でプロンプトを渡す
        result = subprocess.run(
            cmd,
            input=prompt,
            capture_output=True,
            text=True
        )
    else:
        log_info("Starting new Codex session")
        cmd = ["codex", "exec", prompt]
        result = subprocess.run(cmd, capture_output=True, text=True)

    output = result.stdout + result.stderr
    print(output)

    # セッションIDを抽出（Codex の出力形式に依存）
    new_session_id = None
    session_match = re.search(r'session[:\s]+([a-zA-Z0-9_-]+)', output)
    if session_match:
        new_session_id = session_match.group(1)
        log_info(f"Codex session ID: {new_session_id}")

    return output, new_session_id or session_id


def run_claude(prompt: str, session_id: Optional[str], project_root: Path) -> tuple[str, Optional[str]]:
    """Claude を実行し、出力とセッションIDを返す"""

    os.chdir(project_root)

    if session_id:
        log_info(f"Resuming Claude session: {session_id}")
        cmd = ["claude", "--resume", session_id, "-p", prompt]
    else:
        log_info("Starting new Claude session")
        cmd = ["claude", "-p", prompt, "--output-format", "json"]

    result = subprocess.run(cmd, capture_output=True, text=True)
    output = result.stdout + result.stderr

    # JSON出力の場合はパースして表示
    if not session_id:
        try:
            # 複数行のJSON出力から最後の完全なJSONを取得
            json_lines = [line for line in output.split('\n') if line.strip().startswith('{')]
            if json_lines:
                for line in json_lines:
                    try:
                        data = json.loads(line)
                        if "result" in data:
                            print(data.get("result", ""))
                        if "session_id" in data:
                            session_id = data["session_id"]
                            log_info(f"Claude session ID: {session_id}")
                    except json.JSONDecodeError:
                        continue
            else:
                print(output)
        except Exception:
            print(output)
    else:
        print(output)

    return output, session_id


# =============================================================================
# Review Cycle
# =============================================================================

def build_codex_prompt(config: Config, round_num: int) -> str:
    """Codex 用のレビュープロンプトを生成"""
    timestamp = datetime.now().strftime("%y%m%d %H:%M")
    return f"""{config.review_file} を読んで、以下のファイルをレビューしてください。

対象: {config.target_files}

レビュー観点:
1. テストカバレッジは十分か
2. エッジケースは考慮されているか
3. セキュリティテストは十分か
4. コード品質（命名、構造、重複）
5. テスト実行の前提条件は明確か

指摘フォーマット（必ずこの形式で {config.review_file} に追記）:
### 指摘N. タイトル
{timestamp} codex

重大度: High / Medium / Low

対象: `path/to/file:line`

詳細: ...

推奨: ...

---

Round {round_num} のレビューを実施し、指摘があれば {config.review_file} に追記してください。
指摘がなければ「指摘なし」と記載してください。"""


def build_claude_prompt(config: Config, round_num: int) -> str:
    """Claude 用の対応プロンプトを生成"""
    timestamp = datetime.now().strftime("%Y/%m/%d %H:%M")
    return f"""{config.review_file} を読んで、未対応の指摘（特に High）を対応してください。

対応ルール:
1. 指摘ごとにコード修正を実施
2. 対応完了後、{config.review_file} に対応状況を追記（以下フォーマット）:

## {timestamp} claude 対応完了

### 対応状況サマリー
| # | 指摘内容 | 重大度 | 対応状況 |
|---|---------|-------|---------|
| 1 | ... | High | ✅ 対応完了 |

### 各指摘の対応詳細
#### 1. xxx ✅
- 変更内容: ...
- 変更ファイル: `path/to/file:line`

---

High 指摘を優先して対応してください。
対応不要の場合は理由を記載してください。"""


def run_review_cycle(config: Config) -> None:
    """レビューサイクルを実行"""

    print()
    log_info("=" * 50)
    log_info(f"AI Review Cycle: {config.review_name}")
    log_info(f"Target: {config.target_files}")
    log_info(f"Max Rounds: {config.max_rounds}")
    log_info(f"Project: {config.project_root}")
    log_info(f"Session file: {config.session_file}")
    log_info("=" * 50)
    print()

    # 初期化
    init_review_file(config)
    state = SessionState(review_file=str(config.review_file))
    state.save(config.session_file)

    for round_num in range(1, config.max_rounds + 1):
        print()
        log_info(f"========== ROUND {round_num} / {config.max_rounds} ==========")
        print()

        state.current_round = round_num

        # Codex レビュー
        log_step(f"=== Round {round_num}: Codex レビュー開始 ===")
        codex_prompt = build_codex_prompt(config, round_num)
        _, state.codex_session_id = run_codex(
            codex_prompt,
            state.codex_session_id,
            config.project_root
        )
        state.save(config.session_file)

        time.sleep(2)  # API制限対策

        # 指摘チェック
        issues = count_issues(config.review_file)
        log_info(f"High issues: total={issues['high']}, fixed={issues['high_fixed']}, remaining={issues['high_remaining']}")

        if issues["total"] == 0:
            log_success("No issues found. Review cycle completed!")
            break

        if issues["high_remaining"] == 0 and round_num == 1:
            log_success("No High issues found in initial review!")

        # Claude 対応
        log_step(f"=== Round {round_num}: Claude 指摘対応開始 ===")
        claude_prompt = build_claude_prompt(config, round_num)
        _, state.claude_session_id = run_claude(
            claude_prompt,
            state.claude_session_id,
            config.project_root
        )
        state.save(config.session_file)

        time.sleep(2)

        # 再チェック
        issues = count_issues(config.review_file)
        if issues["high_remaining"] == 0:
            log_success(f"All High issues resolved after round {round_num}!")

            # 最終確認レビュー
            if round_num < config.max_rounds - 1:
                log_info("Running final verification review...")
                codex_prompt = build_codex_prompt(config, round_num + 1)
                _, state.codex_session_id = run_codex(
                    codex_prompt,
                    state.codex_session_id,
                    config.project_root
                )
                state.save(config.session_file)

                final_issues = count_issues(config.review_file)
                if final_issues["high_remaining"] == 0:
                    break
            else:
                break

    # 完了メッセージ
    print()
    log_info("=" * 50)
    log_success("Review cycle completed!")
    log_info(f"Review file: {config.review_file}")
    log_info(f"Session file: {config.session_file}")
    print()
    log_info("To resume later:")
    if state.claude_session_id:
        log_info(f"  Claude: claude --resume {state.claude_session_id}")
    if state.codex_session_id:
        log_info(f"  Codex:  codex resume {state.codex_session_id}")
    log_info("=" * 50)
    print()

    # 最終サマリー
    print("--- Final Review Summary ---")
    if config.review_file.exists():
        content = config.review_file.read_text(encoding="utf-8")
        for line in content.split("\n"):
            if re.match(r"^### 指摘|重大度:|対応状況", line):
                print(line)
    else:
        print("No issues recorded")
    print("----------------------------")


# =============================================================================
# Main
# =============================================================================

def main():
    if len(sys.argv) < 3:
        print(__doc__)
        print("Error: Missing arguments")
        print("Usage: python ai_review_cycle.py <review-name> <target-files>")
        sys.exit(1)

    review_name = sys.argv[1]
    target_files = sys.argv[2]
    max_rounds = int(os.environ.get("MAX_ROUNDS", "3"))

    config = Config(
        review_name=review_name,
        target_files=target_files,
        max_rounds=max_rounds
    )

    try:
        run_review_cycle(config)
    except KeyboardInterrupt:
        print()
        log_warn("Interrupted by user")
        sys.exit(1)
    except Exception as e:
        log_error(f"Error: {e}")
        raise


if __name__ == "__main__":
    main()
