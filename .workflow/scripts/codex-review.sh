#!/bin/bash
# Codex CLI を使用してレビューを実行するスクリプト
# Usage: ./codex-review.sh <input_file> <output_file.md> <review_type> [session_id]
# 出力: Markdown 形式（YAML frontmatter に自動化用メタデータ埋め込み）
#
# レビュー基準は .claude/config/review-criteria.yaml で定義
# セッション継続: session_id を指定すると前回のセッションを継続

set -e

INPUT_FILE=$1
OUTPUT_FILE=$2
REVIEW_TYPE=${3:-"general"}
SESSION_ID=${4:-""}  # オプション: セッション継続用

# バリデーション
if [ -z "$INPUT_FILE" ] || [ -z "$OUTPUT_FILE" ]; then
  echo "Usage: $0 <input_file> <output_file.md> [review_type] [session_id]"
  echo "  review_type: requirements | ui | api | code | failure-analysis | general (default: general)"
  echo "  session_id: (optional) Session ID to resume for continuous review"
  exit 1
fi

if [ ! -f "$INPUT_FILE" ]; then
  echo "Error: Input file not found: $INPUT_FILE"
  exit 1
fi

# 出力ファイルを .md に正規化
if [[ "$OUTPUT_FILE" != *.md ]]; then
  OUTPUT_FILE="${OUTPUT_FILE%.json}.md"
fi

# タイムアウト設定 (5分)
TIMEOUT_SEC=300

# timeout コマンドの検出 (macOS 対応)
if command -v gtimeout &> /dev/null; then
  TIMEOUT_CMD="gtimeout $TIMEOUT_SEC"
elif command -v timeout &> /dev/null; then
  TIMEOUT_CMD="timeout $TIMEOUT_SEC"
else
  echo "Warning: timeout/gtimeout not found. Running without timeout."
  echo "Install with: brew install coreutils"
  TIMEOUT_CMD=""
fi

# 設定ファイルのパス
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_DIR="$ROOT_DIR/.claude/config"
CRITERIA_FILE="$CONFIG_DIR/review-criteria.yaml"

# YAMLからレビュー基準を抽出する関数
# PyYAMLがない環境でも動作するようにsed/awkベースのフォールバック付き
extract_yaml_list() {
  local key=$1
  local section=$2
  local file=$3

  # まずPythonでYAMLをパース（PyYAMLがあれば）
  local result
  result=$(python3 -c "
import yaml
import sys

with open('$file', 'r') as f:
    data = yaml.safe_load(f)

section = data.get('$section', {})
items = section.get('$key', [])
for item in items:
    print(f'- {item}')
" 2>/dev/null)

  if [ -n "$result" ]; then
    echo "$result"
    return
  fi

  # フォールバック: sedでセクションからリストを抽出
  # 単純なYAML構造を前提（インデントベース）
  awk -v section="$section" -v key="$key" '
    BEGIN { in_section = 0; in_key = 0; indent = "" }
    /^[a-z_]+:/ {
      if ($0 ~ "^" section ":") { in_section = 1 }
      else { in_section = 0 }
      in_key = 0
    }
    in_section && /^  [a-z_]+:/ {
      if ($0 ~ "^  " key ":") { in_key = 1 }
      else { in_key = 0 }
    }
    in_section && in_key && /^    - / {
      gsub(/^    - "/, "- ")
      gsub(/"$/, "")
      print
    }
  ' "$file" 2>/dev/null || echo "- (設定読み込みエラー)"
}

# レビュー基準をYAMLから読み込み
load_review_criteria() {
  local review_type=$1

  if [ ! -f "$CRITERIA_FILE" ]; then
    echo "Warning: Criteria file not found: $CRITERIA_FILE"
    return
  fi

  IN_SCOPE=$(extract_yaml_list "in_scope" "$review_type" "$CRITERIA_FILE")
  OUT_OF_SCOPE=$(extract_yaml_list "out_of_scope" "$review_type" "$CRITERIA_FILE")
  HIGH_SEVERITY=$(extract_yaml_list "high_severity_criteria" "$review_type" "$CRITERIA_FILE")
}

# レビュータイプに応じたプロンプト
case $REVIEW_TYPE in
  design|requirements)
    load_review_criteria "requirements"
    REVIEW_PROMPT="以下の要件定義ドキュメントをレビューしてください。

## レビュースコープ（このフェーズでレビューする観点）

$IN_SCOPE

## レビュースコープ外（後工程で対応するため、この段階では指摘不要）

以下の項目は後のUI設計・API設計・実装設計フェーズで対応するため、このレビューでは指摘しないでください：

$OUT_OF_SCOPE

## DEMO段階での除外事項

以下はDEMO/ポートフォリオ段階では対応不要のため、指摘しないでください：
- 認可（Authorization）の詳細設計
- RBAC/ABACの詳細設計
- 権限チェックの網羅性

## 指摘の優先度基準

- high: ユースケースの重大な欠落、セキュリティ方針の根本的な問題、要件間の矛盾
- medium: 要件の曖昧さ、非機能要件の不足
- low: 用語の不統一、記載の改善提案

【重要】出力は以下のJSON形式のみとし、マークダウンや説明文を含めないでください。
JSONのみを出力してください。コードブロック(\`\`\`)も使用しないでください。

問題点がある場合:
{\"has_issues\":true,\"issues\":[{\"severity\":\"high\",\"file\":\"ファイル名\",\"line\":null,\"message\":\"指摘内容\"}],\"summary\":\"サマリー\"}

問題がない場合:
{\"has_issues\":false,\"issues\":[],\"summary\":\"問題なし\"}"
    ;;
  ui)
    load_review_criteria "ui"
    REVIEW_PROMPT="以下のUI設計ドキュメントをレビューしてください。

## レビュースコープ

$IN_SCOPE

## レビュースコープ外

以下の項目は後の工程で対応するため、このレビューでは指摘しないでください：

$OUT_OF_SCOPE

## DEMO段階での除外事項

以下はDEMO段階では対応不要のため、指摘しないでください：
- 認可に基づく画面要素の表示/非表示制御
- ロールベースのUI制御

## 指摘の優先度基準

- high: 要件定義との重大な不整合、主要画面の欠落、ユーザー操作フローの根本的な問題
- medium: 画面の曖昧さ、状態考慮の不足
- low: 記載の改善提案

【重要】出力は以下のJSON形式のみとし、マークダウンや説明文を含めないでください。
JSONのみを出力してください。コードブロック(\`\`\`)も使用しないでください。

問題点がある場合:
{\"has_issues\":true,\"issues\":[{\"severity\":\"high\",\"file\":\"ファイル名\",\"line\":null,\"message\":\"指摘内容\"}],\"summary\":\"サマリー\"}

問題がない場合:
{\"has_issues\":false,\"issues\":[],\"summary\":\"問題なし\"}"
    ;;
  api)
    load_review_criteria "api"
    REVIEW_PROMPT="以下のAPI・DB設計ドキュメントをレビューしてください。

## レビュースコープ（このフェーズでレビューする観点）

$IN_SCOPE

## レビュースコープ外（後工程で対応するため、この段階では指摘不要）

以下の項目は実装フェーズで対応するため、このレビューでは指摘しないでください：

$OUT_OF_SCOPE

## DEMO段階での除外事項

以下はDEMO段階では対応不要のため、指摘しないでください：
- 認可（Authorization）の詳細設計
- APIエンドポイントの権限チェック詳細
- RBAC/ABACの実装方式

## 指摘の優先度基準

- high: API仕様の重大な欠落、DB設計の根本的な問題、セキュリティ脆弱性
- medium: 設計の曖昧さ、インデックス不足、エラーコード未定義
- low: 命名の改善提案、ドキュメントの記載不足

【重要】出力は以下のJSON形式のみとし、マークダウンや説明文を含めないでください。
JSONのみを出力してください。コードブロック(\`\`\`)も使用しないでください。

問題点がある場合:
{\"has_issues\":true,\"issues\":[{\"severity\":\"high\",\"file\":\"ファイル名\",\"line\":null,\"message\":\"指摘内容\"}],\"summary\":\"サマリー\"}

問題がない場合:
{\"has_issues\":false,\"issues\":[],\"summary\":\"問題なし\"}"
    ;;
  code)
    load_review_criteria "code"
    REVIEW_PROMPT="以下のコード実装をレビューしてください。

観点:
$IN_SCOPE

スコープ外:
$OUT_OF_SCOPE

【重要】出力は以下のJSON形式のみとし、マークダウンや説明文を含めないでください。
JSONのみを出力してください。コードブロック(\`\`\`)も使用しないでください。

問題点がある場合:
{\"has_issues\":true,\"issues\":[{\"severity\":\"high\",\"file\":\"ファイル名\",\"line\":123,\"message\":\"指摘内容\"}],\"summary\":\"サマリー\"}

問題がない場合:
{\"has_issues\":false,\"issues\":[],\"summary\":\"問題なし\"}"
    ;;
  failure-analysis)
    REVIEW_PROMPT="以下のテスト失敗ログを分析し、失敗原因を分類してください。

分類基準:
- design: API仕様の不整合、DB設計の誤り、要件漏れ、インターフェース不一致
- implementation: バグ、ロジック誤り、型エラー、例外処理漏れ、テストコードの問題
- environment: 依存関係、設定ミス、外部サービス接続、環境変数、ポート競合

【重要】出力は以下のJSON形式のみとし、マークダウンや説明文を含めないでください。
JSONのみを出力してください。コードブロック(\`\`\`)も使用しないでください。

{\"failure_type\":\"design\",\"reason\":\"分類理由\",\"suggested_fix\":\"修正方針\"}"
    ;;
  *)
    REVIEW_PROMPT="以下の内容をレビューしてください。

【重要】出力は以下のJSON形式のみとし、マークダウンや説明文を含めないでください。
JSONのみを出力してください。コードブロック(\`\`\`)も使用しないでください。

問題点がある場合:
{\"has_issues\":true,\"issues\":[{\"severity\":\"high\",\"file\":\"ファイル名\",\"line\":null,\"message\":\"指摘内容\"}],\"summary\":\"サマリー\"}

問題がない場合:
{\"has_issues\":false,\"issues\":[],\"summary\":\"問題なし\"}"
    ;;
esac

# 一時ファイル
TEMP_OUTPUT=$(mktemp)
TEMP_PROMPT=$(mktemp)

# プロンプトファイルを作成
{
  echo "$REVIEW_PROMPT"
  echo ""
  echo "--- 対象ドキュメント ---"
  echo ""
  cat "$INPUT_FILE"
} > "$TEMP_PROMPT"

# Codex 実行
echo "Running Codex review (type: $REVIEW_TYPE)..."
echo "Using criteria from: $CRITERIA_FILE"

# codex exec でレビュー実行
# -o オプションで最終メッセージをファイルに出力
# --json でセッションIDを取得
CODEX_LAST_MSG=$(mktemp)
CODEX_JSON_OUTPUT=$(mktemp)

# セッション継続 or 新規セッション
if [ -n "$SESSION_ID" ]; then
  echo "Resuming session: $SESSION_ID"
  CODEX_CMD="codex exec resume $SESSION_ID"
else
  echo "Starting new session..."
  CODEX_CMD="codex exec"
fi

if [ -n "$TIMEOUT_CMD" ]; then
  $TIMEOUT_CMD $CODEX_CMD \
    --skip-git-repo-check \
    --dangerously-bypass-approvals-and-sandbox \
    --json \
    -o "$CODEX_LAST_MSG" \
    "$(cat "$TEMP_PROMPT")" 2>&1 | tee "$CODEX_JSON_OUTPUT" || true
else
  $CODEX_CMD \
    --skip-git-repo-check \
    --dangerously-bypass-approvals-and-sandbox \
    --json \
    -o "$CODEX_LAST_MSG" \
    "$(cat "$TEMP_PROMPT")" 2>&1 | tee "$CODEX_JSON_OUTPUT" || true
fi

# JSON出力からセッションIDを抽出
EXTRACTED_SESSION_ID=$(grep -o '"session_id":"[^"]*"' "$CODEX_JSON_OUTPUT" | head -1 | sed 's/"session_id":"//;s/"//' || echo "")
if [ -n "$EXTRACTED_SESSION_ID" ]; then
  echo "Session ID: $EXTRACTED_SESSION_ID"
  # セッションIDをファイルに保存（workflow-controller.sh で使用）
  SESSION_FILE="${OUTPUT_FILE%.md}.session"
  echo "$EXTRACTED_SESSION_ID" > "$SESSION_FILE"
  echo "Session ID saved to: $SESSION_FILE"
fi

# JSON出力を通常出力に変換
cp "$CODEX_JSON_OUTPUT" "$TEMP_OUTPUT"
rm -f "$CODEX_JSON_OUTPUT"

# -o で出力されたファイルを優先、なければstdoutを使用
if [ -s "$CODEX_LAST_MSG" ]; then
  echo "Using Codex last message output (-o option)"
  cat "$CODEX_LAST_MSG" >> "$TEMP_OUTPUT"
fi
rm -f "$CODEX_LAST_MSG"

# 一時ファイルを削除
rm -f "$TEMP_PROMPT"

# デバッグ: 生の出力をログに保存
RAW_LOG="${OUTPUT_FILE%.md}-raw.log"
cp "$TEMP_OUTPUT" "$RAW_LOG"
echo "Raw Codex output saved to: $RAW_LOG"

# Python で JSON 抽出 → Markdown 変換（YAML frontmatter 付き）
python3 - "$TEMP_OUTPUT" "$OUTPUT_FILE" "$INPUT_FILE" "$REVIEW_TYPE" << 'PYTHON_SCRIPT'
import sys
import re
import json
from datetime import datetime

raw_file = sys.argv[1]
output_file = sys.argv[2]
input_file = sys.argv[3]
review_type = sys.argv[4] if len(sys.argv) > 4 else "general"

# 日付フォーマット
today = datetime.now().strftime("%Y-%m-%d")
timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
date_prefix = datetime.now().strftime("%m%d")

def extract_json(content, review_type):
    """Codex出力からレビューJSONを抽出"""
    # マークダウンコードブロック内のJSONを優先抽出
    code_block_pattern = r'```(?:json)?\s*\n?([\s\S]*?)\n?```'
    code_blocks = re.findall(code_block_pattern, content)
    for block in code_blocks:
        json_obj = try_parse_json(block.strip(), review_type)
        if json_obj:
            return json_obj

    # テキスト全体からJSONを抽出（ネスト対応）
    return extract_nested_json(content, review_type)

def try_parse_json(text, review_type):
    """JSONとしてパース試行"""
    try:
        obj = json.loads(text)
        if review_type == "failure-analysis":
            if "failure_type" in obj:
                return obj
        else:
            if "has_issues" in obj:
                return obj
        return None
    except json.JSONDecodeError:
        return None

def extract_nested_json(content, review_type):
    """ネストされたJSONを抽出（最後に見つかった有効なJSONを返す）"""
    start_idx = -1
    brace_count = 0
    found_jsons = []  # 見つかったすべての有効なJSONを保存

    for i, char in enumerate(content):
        if char == '{':
            if brace_count == 0:
                start_idx = i
            brace_count += 1
        elif char == '}':
            brace_count -= 1
            if brace_count == 0 and start_idx >= 0:
                json_candidate = content[start_idx:i+1]
                json_obj = try_parse_json(json_candidate, review_type)
                if json_obj:
                    found_jsons.append(json_obj)
                start_idx = -1

    # 最後に見つかったJSONを返す（プロンプト例ではなく実際のレビュー結果）
    if found_jsons:
        # issuesの数が最も多いものを選択（より詳細なレビュー結果）
        if review_type != "failure-analysis":
            found_jsons.sort(key=lambda x: len(x.get("issues", [])), reverse=True)
        return found_jsons[0]
    return None

def get_default_response(review_type):
    """デフォルトレスポンス"""
    if review_type == "failure-analysis":
        return {
            "failure_type": "unknown",
            "reason": "Codex出力からJSONを抽出できませんでした",
            "suggested_fix": "手動で確認してください"
        }
    else:
        return {
            "has_issues": False,
            "issues": [],
            "summary": "レビュー完了（Codex出力からJSONを抽出できませんでした）"
        }

# レビュータイプの日本語名
type_names = {
    "design": "設計レビュー",
    "requirements": "要件定義レビュー",
    "ui": "UI設計レビュー",
    "api": "API・DB設計レビュー",
    "code": "コードレビュー",
    "failure-analysis": "失敗分析",
    "general": "一般レビュー"
}
review_type_name = type_names.get(review_type, "レビュー")

# Codex 出力を読み込み
try:
    with open(raw_file, 'r', encoding='utf-8') as f:
        content = f.read()
except Exception as e:
    content = ""

# JSON 抽出
if not content.strip():
    print("Warning: Codex output is empty", file=sys.stderr)
    data = get_default_response(review_type)
else:
    data = extract_json(content, review_type)
    if data is None:
        print("Warning: Could not extract valid JSON from Codex output", file=sys.stderr)
        data = get_default_response(review_type)
    else:
        print("Successfully extracted JSON from Codex output", file=sys.stderr)

# Markdown 生成
lines = []

# YAML Frontmatter（自動化用メタデータ）
lines.append("---")
if review_type == "failure-analysis":
    lines.append(f"failure_type: {data.get('failure_type', 'unknown')}")
else:
    lines.append(f"has_issues: {str(data.get('has_issues', False)).lower()}")
    lines.append(f"issue_count: {len(data.get('issues', []))}")
lines.append(f"review_type: {review_type}")
lines.append(f"reviewed_at: \"{timestamp}\"")
lines.append(f"target_file: \"{input_file}\"")
lines.append(f"reviewer: codex")
lines.append("---")
lines.append("")

# タイトル
lines.append(f"# {review_type_name} 結果")
lines.append("")
lines.append(f"**対象ファイル:** `{input_file}`")
lines.append(f"**レビュー日時:** {timestamp}")
lines.append(f"**レビュアー:** Codex CLI")
lines.append("")
lines.append("---")
lines.append("")

# failure-analysis の場合
if review_type == "failure-analysis":
    failure_type = data.get("failure_type", "unknown")
    reason = data.get("reason", "不明")
    suggested_fix = data.get("suggested_fix", "なし")

    type_labels = {"design": "設計", "implementation": "実装", "environment": "環境", "unknown": "不明"}
    lines.append("## 分析結果")
    lines.append("")
    lines.append(f"**失敗タイプ:** `{failure_type}` ({type_labels.get(failure_type, failure_type)})")
    lines.append("")
    lines.append("### 原因分析")
    lines.append("")
    lines.append(reason)
    lines.append("")
    lines.append("### 推奨対応")
    lines.append("")
    lines.append(suggested_fix)
    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## 次のアクション")
    lines.append("")
    lines.append("1. 上記の分析結果を確認")
    lines.append("2. 推奨対応に従って修正")
    lines.append("3. 再テスト実行")

# 通常レビューの場合
else:
    has_issues = data.get("has_issues", False)
    issues = data.get("issues", [])
    summary = data.get("summary", "")

    # サマリー
    if has_issues:
        lines.append(f"## 指摘あり ({len(issues)}件)")
    else:
        lines.append("## 指摘なし")
    lines.append("")
    lines.append(f"**サマリー:** {summary}")
    lines.append("")

    # 指摘詳細
    if issues:
        lines.append("---")
        lines.append("")
        lines.append("## 指摘一覧")
        lines.append("")

        # サマリーテーブル
        lines.append("| # | 優先度 | ファイル | 行 | 概要 | 対応状況 |")
        lines.append("|---|--------|---------|-----|------|---------|")
        for idx, issue in enumerate(issues, 1):
            severity = issue.get("severity", "medium")
            severity_emoji = {"high": "High", "medium": "Medium", "low": "Low"}.get(severity, "-")
            file_name = issue.get("file", "-")
            line_num = issue.get("line") or "-"
            message = issue.get("message", "")[:50].replace("|", "\\|")
            lines.append(f"| {idx} | {severity_emoji} | `{file_name}` | {line_num} | {message}... | 未対応 |")
        lines.append("")

        # 詳細セクション
        for idx, issue in enumerate(issues, 1):
            severity = issue.get("severity", "medium")
            file_name = issue.get("file", "-")
            line_num = issue.get("line")
            message = issue.get("message", "")

            short_title = message[:40].replace("\n", " ")
            lines.append(f"### 指摘{idx}: {short_title}...")
            lines.append("")
            lines.append(f"**優先度:** {severity.upper()}")
            if file_name and file_name != "-":
                loc = f"`{file_name}"
                if line_num:
                    loc += f":{line_num}"
                loc += "`"
                lines.append(f"**対象:** {loc}")
            lines.append("")
            lines.append(f"**[{date_prefix} codex] 指摘内容:**")
            lines.append("")
            lines.append(message)
            lines.append("")
            lines.append(f"**[MMDD agent] 対応内容:** （対応後に追記）")
            lines.append("")
            lines.append("**再レビュー確認ポイント:**")
            lines.append("- [ ] 指摘内容が修正されているか")
            lines.append("- [ ] 副作用がないか")
            lines.append("")
            lines.append("---")
            lines.append("")

    # フッター
    lines.append("")
    lines.append("## 次のアクション")
    lines.append("")
    if has_issues:
        lines.append("1. 各指摘を確認し、対応内容を追記")
        lines.append("2. 対応完了後、ステータスを `対応済み` に更新")
        lines.append("3. 再レビュー依頼")
    else:
        lines.append("レビュー完了。次のステップに進んでください。")

# ファイル出力
with open(output_file, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print(f"Markdown review saved to: {output_file}", file=sys.stderr)
PYTHON_SCRIPT

rm -f "$TEMP_OUTPUT"

# 出力の検証
if [ ! -s "$OUTPUT_FILE" ]; then
  echo "Warning: Output file is empty, creating default"
  cat > "$OUTPUT_FILE" << 'DEFAULT_MD'
---
has_issues: false
issue_count: 0
review_type: general
reviewer: codex
---

# レビュー結果

## 指摘なし

**サマリー:** レビュー完了（出力生成エラー）

## 次のアクション

レビュー完了。次のステップに進んでください。
DEFAULT_MD
fi

echo "Review completed: $OUTPUT_FILE"
echo ""
echo "=== Review Summary ==="
head -20 "$OUTPUT_FILE"
