#!/bin/bash
# Claude Code + Codex CLI ワークフロー制御スクリプト
# Usage: ./workflow-controller.sh [step]
#   step: design | implement | test-backend | test-frontend | all (default: all)
#
# 設計ワークフロー: .workflow/DESIGN-WORKFLOW.md
# レビュー基準: .claude/config/review-criteria.yaml

set -e

# スクリプトのルートディレクトリを取得 (cd しても壊れないように)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

WORKFLOW_DIR="$ROOT_DIR/.workflow"
SCRIPTS_DIR="$SCRIPT_DIR"
DESIGN_OUTPUT_DIR="$ROOT_DIR/docs/design"

# 設定
MAX_REVIEW_ITERATIONS=3
MAX_TOTAL_RETRIES=3
DESIGN_TIMEOUT=600      # 10分
IMPLEMENT_TIMEOUT=1800  # 30分
TEST_TIMEOUT=600        # 10分

# timeout コマンドの検出 (macOS 対応)
if command -v gtimeout &> /dev/null; then
  TIMEOUT_CMD="gtimeout"
elif command -v timeout &> /dev/null; then
  TIMEOUT_CMD="timeout"
else
  echo "Warning: timeout/gtimeout not found. Running without timeout."
  echo "Install with: brew install coreutils"
  TIMEOUT_CMD=""
fi

# 色付き出力
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# Markdown レビューファイルから has_issues を取得
# YAML frontmatter の has_issues: false なら成功 (return 0)
check_no_issues() {
  local review_file=$1

  if [ ! -f "$review_file" ]; then
    return 1
  fi

  # YAML frontmatter から has_issues を抽出
  local has_issues
  has_issues=$(sed -n '/^---$/,/^---$/p' "$review_file" | grep -E '^has_issues:' | sed 's/has_issues: *//' | tr -d ' ')

  if [ "$has_issues" = "false" ]; then
    return 0
  else
    return 1
  fi
}

# Markdown レビューファイルから failure_type を取得
get_failure_type() {
  local review_file=$1

  if [ ! -f "$review_file" ]; then
    echo "unknown"
    return
  fi

  # YAML frontmatter から failure_type を抽出
  local failure_type
  failure_type=$(sed -n '/^---$/,/^---$/p' "$review_file" | grep -E '^failure_type:' | sed 's/failure_type: *//' | tr -d ' ')

  echo "${failure_type:-unknown}"
}

# ワークフローディレクトリ初期化
init_workflow() {
  mkdir -p "$WORKFLOW_DIR"
  log_info "Workflow directory initialized: $WORKFLOW_DIR"
}

# Step 1: 設計フェーズ (3段階: 要件定義 → UI設計 → API設計)
design_phase() {
  log_info "=== Step 1: Design Phase ==="
  log_info "設計は3段階に分かれています:"
  log_info "  1.1 要件定義 - 機能要件・非機能要件"
  log_info "  1.2 UI設計 - 画面設計・操作フロー"
  log_info "  1.3 API・DB設計 - エンドポイント・テーブル定義"
  echo ""

  # 1.1 要件定義
  if ! design_requirements; then
    return 1
  fi

  # 1.2 UI設計
  if ! design_ui; then
    return 1
  fi

  # 1.3 API設計
  if ! design_api; then
    return 1
  fi

  log_info "All design phases completed!"
  return 0
}

# 機能名を取得 (環境変数または引数から)
FEATURE_NAME="${FEATURE_NAME:-お気に入り物件}"

# 1.1 要件定義
design_requirements() {
  log_info "--- Step 1.1: Requirements Definition ---"
  log_info "Feature: $FEATURE_NAME"

  # 成果物出力先: docs/design/RD-{機能名}.md
  local rd_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md"
  local reference_file="$ROOT_DIR/docs/design/RD-不動産物件マッチング検索システム.md"

  # 要件定義書を自動生成
  if [ ! -f "$rd_file" ]; then
    log_info "Generating requirements document with Claude..."
    log_info "Output: $rd_file"

    claude -p "
${FEATURE_NAME}機能の要件定義書を作成してください。

参考フォーマット: $reference_file

以下の構成で作成:
1. 概要（目的、背景）
2. ステークホルダー
3. 機能要件
4. 非機能要件

重要: API設計やUI設計の詳細は含めないでください。

出力先: $rd_file
" --allowedTools "Read,Write,Glob" > "$WORKFLOW_DIR/claude-rd.log" 2>&1

    if [ ! -f "$rd_file" ]; then
      log_error "Failed to generate requirements document."
      cat "$WORKFLOW_DIR/claude-rd.log"
      return 1
    fi
  fi

  # Codex レビュー → Claude 修正 ループ
  # 要件定義フェーズは "requirements" タイプでレビュー
  review_and_fix "$rd_file" "requirements" "requirements"
}

# 1.2 UI設計
design_ui() {
  log_info "--- Step 1.2: UI Design ---"
  log_info "Feature: $FEATURE_NAME"

  # 成果物出力先: docs/design/ui/SCR-{機能名}.md
  local ui_file="$DESIGN_OUTPUT_DIR/ui/SCR-${FEATURE_NAME}.md"
  local reference_file="$ROOT_DIR/docs/design/ui/SCR-003-property-search.md"
  local rd_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md"

  # UI設計書を自動生成
  if [ ! -f "$ui_file" ]; then
    log_info "Generating UI design document with Claude..."
    log_info "Output: $ui_file"

    claude -p "
${FEATURE_NAME}機能のUI設計書を作成してください。

参考フォーマット: $reference_file
要件定義書: $rd_file

以下の構成で作成:
1. 画面概要（目的、対象ユーザー、アクセス経路）
2. 画面イメージ（テキスト形式のワイヤーフレーム）
3. 画面構成要素

出力先: $ui_file
" --allowedTools "Read,Write,Glob" > "$WORKFLOW_DIR/claude-ui.log" 2>&1

    if [ ! -f "$ui_file" ]; then
      log_error "Failed to generate UI design document."
      cat "$WORKFLOW_DIR/claude-ui.log"
      return 1
    fi
  fi

  # Codex レビュー → Claude 修正 ループ
  review_and_fix "$ui_file" "ui" "ui"
}

# 1.3 API・DB設計
design_api() {
  log_info "--- Step 1.3: API & DB Design ---"
  log_info "Feature: $FEATURE_NAME"

  # 成果物出力先: docs/design/api/API-{機能名}.md
  local api_file="$DESIGN_OUTPUT_DIR/api/API-${FEATURE_NAME}.md"
  local reference_api="$ROOT_DIR/docs/design/api/LST-QRY-01-物件検索.md"
  local reference_db="$ROOT_DIR/docs/design/db/property_business_schema_v0.4.sql"
  local rd_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md"
  local ui_file="$DESIGN_OUTPUT_DIR/ui/SCR-${FEATURE_NAME}.md"

  # API・DB設計書を自動生成
  if [ ! -f "$api_file" ]; then
    log_info "Generating API & DB design document with Claude..."
    log_info "Output: $api_file"

    claude -p "
${FEATURE_NAME}機能のAPI・DB設計書を作成してください。

参考フォーマット（API）: $reference_api
参考フォーマット（DB）: $reference_db
要件定義書: $rd_file
UI設計書: $ui_file

以下の構成で作成:

## API設計
1. 概要（目的、関連ドキュメント）
2. エンドポイント一覧
3. 各APIの詳細
   - 基本情報（メソッド、パス、認証）
   - リクエスト（ヘッダー、パラメータ、ボディ）
   - レスポンス（成功、エラー）
4. エラーコード体系

## DB設計
1. テーブル定義（カラム、型、制約、デフォルト値）
2. インデックス設計
3. 外部キー・参照整合性
4. DDL（CREATE TABLE文）

出力先: $api_file
" --allowedTools "Read,Write,Glob" > "$WORKFLOW_DIR/claude-api.log" 2>&1

    if [ ! -f "$api_file" ]; then
      log_error "Failed to generate API & DB design document."
      cat "$WORKFLOW_DIR/claude-api.log"
      return 1
    fi
  fi

  # Codex レビュー → Claude 修正 ループ
  review_and_fix "$api_file" "api" "api"
}

# 前工程の問題検出 (レビュー結果から前工程への影響をチェック)
check_upstream_issues() {
  local review_file=$1
  local current_phase=$2  # ui or api

  if [ ! -f "$review_file" ]; then
    return 1
  fi

  local upstream_keywords=""
  local upstream_phase=""
  local upstream_file=""

  case $current_phase in
    ui)
      upstream_keywords="要件定義|要件の|機能要件|非機能要件|ユースケース"
      upstream_phase="requirements"
      upstream_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md"
      ;;
    api)
      upstream_keywords="要件定義|要件の|UI設計|画面設計|操作フロー|表示項目"
      upstream_phase="requirements/ui"
      upstream_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md $DESIGN_OUTPUT_DIR/ui/SCR-${FEATURE_NAME}.md"
      ;;
  esac

  if [ -z "$upstream_keywords" ]; then
    return 1
  fi

  # 指摘内容に前工程関連のキーワードが含まれているかチェック
  local issues_section
  issues_section=$(sed -n '/^## 指摘一覧/,/^## 次のアクション/p' "$review_file" 2>/dev/null || echo "")

  if echo "$issues_section" | grep -qE "$upstream_keywords"; then
    log_warn "=========================================="
    log_warn "  前工程の設計書に影響する指摘を検出"
    log_warn "=========================================="
    log_warn ""
    log_warn "現在のフェーズ: $current_phase"
    log_warn "影響する可能性のある前工程: $upstream_phase"
    log_warn "前工程ファイル: $upstream_file"
    log_warn ""
    log_warn "指摘内容を確認し、必要に応じて前工程の設計書を修正してください。"
    log_warn ""
    log_warn "前工程を再実行する場合:"
    log_warn "  FEATURE_NAME=\"$FEATURE_NAME\" $0 requirements"
    log_warn "  FEATURE_NAME=\"$FEATURE_NAME\" $0 ui"
    log_warn ""
    log_warn "=========================================="
    return 0
  fi

  return 1
}

# 共通: レビュー → 修正 ループ (完全自動化)
review_and_fix() {
  local doc_file=$1
  local doc_type=$2
  local review_type=$3

  local review_md_dir="$ROOT_DIR/.review"
  mkdir -p "$review_md_dir"

  local timestamp
  timestamp=$(date +%Y%m%d-%H%M%S)
  local review_md_file="$review_md_dir/${timestamp}_${doc_type}_review.md"

  # レビュー履歴ファイルの初期化
  {
    echo "# ${doc_type} レビュー履歴"
    echo ""
    echo "対象ファイル: \`$doc_file\`"
    echo "開始日時: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "機能名: $FEATURE_NAME"
    echo ""
    echo "---"
    echo ""
  } > "$review_md_file"

  # セッションID管理（同一フェーズ内でセッション継続）
  local session_id=""
  local session_file="$WORKFLOW_DIR/session-$doc_type.id"

  for i in $(seq 1 $MAX_REVIEW_ITERATIONS); do
    log_info "$doc_type review iteration $i / $MAX_REVIEW_ITERATIONS"

    local review_file="$WORKFLOW_DIR/review-$doc_type-$i.md"

    # 2回目以降はセッションIDを読み込み
    if [ $i -gt 1 ] && [ -f "$session_file" ]; then
      session_id=$(cat "$session_file")
      log_info "Resuming session: $session_id"
    fi

    # Codex レビュー実行（セッションID付き）
    bash "$SCRIPTS_DIR/codex-review.sh" \
      "$doc_file" \
      "$review_file" \
      "$review_type" \
      "$session_id"

    # 1回目実行後、セッションIDを保存
    if [ $i -eq 1 ]; then
      local extracted_session="${review_file%.md}.session"
      if [ -f "$extracted_session" ]; then
        cp "$extracted_session" "$session_file"
        log_info "Session ID stored for reuse: $(cat "$session_file")"
      fi
    fi

    # レビュー結果を確認
    if [ ! -f "$review_file" ]; then
      log_error "Review file not created: $review_file"
      append_review_error "$review_md_file" "$i" "レビューファイルが作成されませんでした"
      return 1
    fi

    # レビュー結果を MD に追記
    append_review_result "$review_md_file" "$review_file" "$i"

    # 指摘なしなら完了（YAML frontmatter から has_issues を取得）
    if check_no_issues "$review_file"; then
      log_info "$doc_type design approved!"
      append_review_success "$review_md_file"
      return 0
    fi

    log_warn "Issues found in $doc_type design."

    # 前工程への影響チェック (ui, api フェーズのみ)
    # Note: || true で戻り値を無視 (set -e でスクリプトが終了しないように)
    if [ "$doc_type" = "ui" ] || [ "$doc_type" = "api" ]; then
      check_upstream_issues "$review_file" "$doc_type" || true
    fi

    # 3回目で High 指摘がない場合は対応終了
    if [ $i -eq 3 ]; then
      # High 優先度の指摘があるかチェック
      if ! grep -q "High" "$review_file" 2>/dev/null; then
        log_info "No High severity issues in round 3. Marking as complete."
        append_review_success "$review_md_file"
        return 0
      fi
    fi

    # 最後のイテレーションなら失敗 → 人間介入用レポート出力
    if [ $i -eq $MAX_REVIEW_ITERATIONS ]; then
      log_error "$doc_type design exceeded max iterations"
      generate_intervention_report "$review_md_file" "$doc_file" "$doc_type" "$review_file"
      return 1
    fi

    # Claude で自動修正
    log_info "Auto-fixing with Claude..."

    # イテレーションに応じた対応優先度を決定
    local required_severity="low"
    local severity_note=""
    if [ $i -eq 1 ]; then
      required_severity="low"
      severity_note="初回: Low まで全対応"
    elif [ $i -eq 2 ]; then
      required_severity="low"
      severity_note="2回目: Low まで対応"
    else
      required_severity="low"
      severity_note="3回目: High なしなら処理完了"
    fi
    log_info "Severity policy: $severity_note"

    # Markdown ファイルから指摘内容を抽出（指摘セクション全体）
    # Note: head -n -1 は macOS で動作しないため sed '$d' を使用
    local issues
    issues=$(sed -n '/^## 指摘一覧/,/^## 次のアクション/p' "$review_file" | sed '$d')

    # 修正内容を MD に追記
    {
      echo "### 自動修正 (Iteration $i)"
      echo ""
      echo "Claude による自動修正を実行中..."
      echo ""
    } >> "$review_md_file"

    # 今日の日付を取得 (MMDD形式)
    local today_mmdd
    today_mmdd=$(date +%m%d)

    claude -p "
以下のレビュー指摘に基づいて $doc_file を修正してください。

## 対応方針（イテレーション $i / $MAX_REVIEW_ITERATIONS）

$severity_note

- 対応必須: $required_severity 以上
- Low 優先度の指摘は対応不要（スキップ可）
- 認可（Authorization）関連の指摘は DEMO 段階では対応不要

レビュー指摘:
$issues

## 作業手順

1. $doc_file を読み込む
2. 対応必須の優先度（$required_severity 以上）の指摘に対応する修正を行う
3. 修正内容を $doc_file に上書き保存する
4. レビューファイル $review_file の各指摘セクションにある対応状況を更新する:

   対応した指摘:
   **[$today_mmdd claude] 対応内容:**
   [修正内容の説明]
   - 追加/変更したセクション名
   - 具体的な修正内容

   対応しない指摘（Low優先度など）:
   **[$today_mmdd claude] 対応内容:** 対応不要（$severity_note）

5. サマリーテーブルの「対応状況」列も更新する:
   - 対応済み → 「対応済み」
   - 対応不要 → 「対応不要」

## 重要
- 必ず両方のファイルを更新すること
- 対応内容は具体的に記載すること（「修正しました」だけは不可）
- 優先度に応じて対応要否を判断すること
" --allowedTools "Read,Write,Edit" > "$WORKFLOW_DIR/claude-fix-$doc_type-$i.log" 2>&1

    {
      echo "修正完了。再レビューを実行します。"
      echo ""
      echo "---"
      echo ""
    } >> "$review_md_file"

    log_info "Fix applied. Re-reviewing..."
    sleep 2
  done

  return 1
}

# レビュー結果を MD に追記
append_review_result() {
  local md_file=$1
  local review_file=$2  # 今は .md ファイル
  local iteration=$3

  # YAML frontmatter から has_issues を取得
  local has_issues
  has_issues=$(sed -n '/^---$/,/^---$/p' "$review_file" | grep -E '^has_issues:' | sed 's/has_issues: *//' | tr -d ' ')
  has_issues=${has_issues:-false}

  # サマリーを抽出（**サマリー:** の後の内容）
  local summary
  summary=$(grep -E '^\*\*サマリー:\*\*' "$review_file" | sed 's/\*\*サマリー:\*\* *//' || echo "No summary")

  {
    echo "## Iteration $iteration"
    echo ""
    echo "**レビュー結果:** $([ "$has_issues" = "true" ] && echo "❌ 指摘あり" || echo "✅ 問題なし")"
    echo ""
    echo "**サマリー:** $summary"
    echo ""

    if [ "$has_issues" = "true" ]; then
      echo "### 指摘事項"
      echo ""

      # Markdown ファイルから指摘一覧テーブルと詳細を抽出
      # Note: head -n -1 は macOS で動作しないため sed '$d' を使用
      if [ -f "$review_file" ]; then
        sed -n '/^| # | 優先度/,/^## 次のアクション/p' "$review_file" | sed '$d'
      fi
      echo ""
    fi

    echo "---"
    echo ""
  } >> "$md_file"

  # レビューファイルを .review/ にもコピー
  if [ -f "$review_file" ]; then
    local detail_filename
    detail_filename=$(basename "$review_file")
    cp "$review_file" "$ROOT_DIR/.review/$detail_filename"
    log_info "Review detail copied: .review/$detail_filename"
  fi
}

# 成功時の追記
append_review_success() {
  local md_file=$1

  {
    echo "---"
    echo ""
    echo "## 結果: ✅ 承認"
    echo ""
    echo "完了日時: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
  } >> "$md_file"

  log_info "Review history saved: $md_file"
}

# エラー時の追記
append_review_error() {
  local md_file=$1
  local iteration=$2
  local error_msg=$3

  {
    echo "## Iteration $iteration"
    echo ""
    echo "**エラー:** $error_msg"
    echo ""
  } >> "$md_file"
}

# 人間介入用レポート生成
generate_intervention_report() {
  local md_file=$1
  local doc_file=$2
  local doc_type=$3
  local last_review_file=$4

  local intervention_file="$ROOT_DIR/.review/INTERVENTION_REQUIRED_$(date +%Y%m%d-%H%M%S).md"

  {
    echo "# ⚠️ 人間介入が必要です"
    echo ""
    echo "**生成日時:** $(date '+%Y-%m-%d %H:%M:%S')"
    echo "**機能名:** $FEATURE_NAME"
    echo "**設計タイプ:** $doc_type"
    echo ""
    echo "---"
    echo ""
    echo "## 状況"
    echo ""
    echo "自動レビュー修正が最大回数 ($MAX_REVIEW_ITERATIONS 回) に達しましたが、"
    echo "まだ未解決の指摘があります。"
    echo ""
    echo "## 対象ファイル"
    echo ""
    echo "- 設計書: \`$doc_file\`"
    echo "- レビュー履歴: \`$md_file\`"
    echo "- 最終レビュー結果: \`$last_review_file\`"
    echo ""
    echo "## 未解決の指摘"
    echo ""

    # Markdown ファイルから指摘テーブルを抽出
    sed -n '/^| # | 優先度/,/^$/p' "$last_review_file" | head -20

    echo ""
    echo "## 推奨アクション"
    echo ""
    echo "1. 上記の未解決指摘を確認"
    echo "2. \`$doc_file\` を手動で修正"
    echo "3. 以下のコマンドで再実行:"
    echo ""
    echo "\`\`\`bash"
    echo "# 既存ファイルを保持して再実行"
    echo "FEATURE_NAME=\"$FEATURE_NAME\" bash .claude/scripts/workflow-controller.sh design"
    echo "\`\`\`"
    echo ""
    echo "## レビュー詳細"
    echo ""
    echo "最終レビュー結果:"
    echo ""
    cat "$last_review_file"
    echo ""
    echo "## 設計書の現在の内容"
    echo ""
    echo "\`\`\`markdown"
    head -100 "$doc_file"
    echo ""
    echo "... (省略)"
    echo "\`\`\`"
  } > "$intervention_file"

  # 元のレビュー履歴にもリンク追加
  {
    echo "---"
    echo ""
    echo "## 結果: ❌ 最大回数超過"
    echo ""
    echo "**人間介入が必要です**"
    echo ""
    echo "詳細: \`$intervention_file\`"
    echo ""
  } >> "$md_file"

  log_error "=========================================="
  log_error "  INTERVENTION REQUIRED"
  log_error "=========================================="
  log_error ""
  log_error "レビュー修正が最大回数に達しました。"
  log_error ""
  log_error "詳細レポート:"
  log_error "  $intervention_file"
  log_error ""
  log_error "レビュー履歴:"
  log_error "  $md_file"
  log_error ""
  log_error "対象ファイル:"
  log_error "  $doc_file"
  log_error ""
  log_error "=========================================="
}

# 実装サマリーを自動生成
generate_impl_summary() {
  log_info "Generating implementation summary..."

  local summary_file="$WORKFLOW_DIR/impl-summary.md"

  {
    echo "# 実装サマリー"
    echo ""
    echo "## 設計書"
    echo ""
    cat "$WORKFLOW_DIR/design.md"
    echo ""
    echo "## 変更ファイル一覧"
    echo ""
    echo '```'
    git -C "$ROOT_DIR" diff --name-only HEAD 2>/dev/null || echo "(uncommitted changes)"
    echo '```'
    echo ""
    echo "## 変更内容 (diff)"
    echo ""
    echo '```diff'
    git -C "$ROOT_DIR" diff HEAD 2>/dev/null | head -500 || echo "(no diff available)"
    echo '```'
  } > "$summary_file"

  log_info "Implementation summary generated: $summary_file"
}

# Step 2: 実装フェーズ
implement_phase() {
  log_info "=== Step 2: Implementation Phase ==="

  # impl-summary.md がなければ自動生成
  if [ ! -f "$WORKFLOW_DIR/impl-summary.md" ]; then
    log_warn "Implementation summary not found. Generating..."
    generate_impl_summary
  fi

  # Codex レビュー → Claude 修正 ループ
  for i in $(seq 1 $MAX_REVIEW_ITERATIONS); do
    log_info "Code review iteration $i / $MAX_REVIEW_ITERATIONS"

    # 最新の差分でサマリーを更新
    generate_impl_summary

    bash "$SCRIPTS_DIR/codex-review.sh" \
      "$WORKFLOW_DIR/impl-summary.md" \
      "$WORKFLOW_DIR/review-impl-$i.md" \
      "code"

    if check_no_issues "$WORKFLOW_DIR/review-impl-$i.md"; then
      log_info "Implementation approved!"
      return 0
    fi

    log_warn "Issues found. Please fix and re-run."
    log_warn "Review file: $WORKFLOW_DIR/review-impl-$i.md"

    if [ $i -lt $MAX_REVIEW_ITERATIONS ]; then
      log_info "Waiting for fixes..."
      read -p "Press Enter after fixing implementation..."
    fi
  done

  log_error "Implementation phase exceeded max iterations"
  return 1
}

# Step 3: Backend E2E テスト (サブシェルで実行)
backend_e2e() {
  log_info "=== Step 3: Backend E2E Test ==="

  local test_log="$WORKFLOW_DIR/test-backend.log"
  local exit_code=0

  # サブシェルで実行 (cd が親シェルに影響しない)
  (
    cd "$ROOT_DIR/apps/backend"
    if [ -n "$TIMEOUT_CMD" ]; then
      $TIMEOUT_CMD $TEST_TIMEOUT mvn test -Dtest=*E2ETest 2>&1
    else
      mvn test -Dtest=*E2ETest 2>&1
    fi
  ) | tee "$test_log" || exit_code=$?

  if [ $exit_code -eq 0 ]; then
    log_info "Backend E2E tests passed!"
    return 0
  else
    log_error "Backend E2E tests failed"
    return 1
  fi
}

# Step 4: Frontend E2E テスト (サブシェルで実行)
frontend_e2e() {
  log_info "=== Step 4: Frontend E2E Test ==="

  local test_log="$WORKFLOW_DIR/test-frontend.log"
  local exit_code=0

  # サブシェルで実行 (cd が親シェルに影響しない)
  (
    cd "$ROOT_DIR/apps/frontend"
    if [ -n "$TIMEOUT_CMD" ]; then
      $TIMEOUT_CMD $TEST_TIMEOUT npm run test:e2e 2>&1
    else
      npm run test:e2e 2>&1
    fi
  ) | tee "$test_log" || exit_code=$?

  if [ $exit_code -eq 0 ]; then
    log_info "Frontend E2E tests passed!"
    return 0
  else
    log_error "Frontend E2E tests failed"
    return 1
  fi
}

# 失敗原因を分析 (Codex に判定させる)
analyze_failure() {
  local test_log=$1
  local analysis_file="$WORKFLOW_DIR/failure-analysis.md"

  log_info "Analyzing failure cause..."

  bash "$SCRIPTS_DIR/codex-review.sh" \
    "$test_log" \
    "$analysis_file" \
    "failure-analysis"

  # failure_type: "design" | "implementation" | "environment"
  local failure_type
  failure_type=$(get_failure_type "$analysis_file")

  log_info "Failure type: $failure_type"
  echo "$failure_type"
}

# メインループ
run_all() {
  init_workflow

  local from_step="design"  # "design" or "implement"

  for retry in $(seq 1 $MAX_TOTAL_RETRIES); do
    log_info "=== Workflow Attempt $retry / $MAX_TOTAL_RETRIES (from: $from_step) ==="

    # Step 1: 設計 (from_step が design の場合のみ)
    if [ "$from_step" = "design" ]; then
      if ! design_phase; then
        log_error "Design phase failed"
        continue
      fi
    fi

    # Step 2: 実装
    if ! implement_phase; then
      log_error "Implementation phase failed"
      from_step="implement"
      continue
    fi

    # Step 3: Backend E2E
    if ! backend_e2e; then
      failure_type=$(analyze_failure "$WORKFLOW_DIR/test-backend.log")
      if [ "$failure_type" = "design" ]; then
        log_warn "Design issue detected, retrying from Step 1..."
        from_step="design"
      else
        log_warn "Implementation/environment issue, retrying from Step 2..."
        from_step="implement"
      fi
      continue
    fi

    # Step 4: Frontend E2E
    if ! frontend_e2e; then
      failure_type=$(analyze_failure "$WORKFLOW_DIR/test-frontend.log")
      if [ "$failure_type" = "design" ]; then
        log_warn "Design issue detected, retrying from Step 1..."
        from_step="design"
      else
        log_warn "Implementation/environment issue, retrying from Step 2..."
        from_step="implement"
      fi
      continue
    fi

    log_info "=== Workflow completed successfully! ==="
    exit 0
  done

  log_error "=== Workflow failed after $MAX_TOTAL_RETRIES attempts ==="
  exit 1
}

# エントリポイント
# Usage: FEATURE_NAME="機能名" ./workflow-controller.sh [step]
case ${1:-all} in
  requirements)
    # 要件定義のみ
    if [ -z "$FEATURE_NAME" ]; then
      echo "Usage: FEATURE_NAME=\"機能名\" $0 requirements"
      echo ""
      echo "Example:"
      echo "  FEATURE_NAME=\"お気に入り物件\" $0 requirements"
      exit 1
    fi
    init_workflow
    design_requirements
    ;;
  ui)
    # UI設計のみ (要件定義が存在することを確認)
    if [ -z "$FEATURE_NAME" ]; then
      echo "Usage: FEATURE_NAME=\"機能名\" $0 ui"
      echo ""
      echo "Example:"
      echo "  FEATURE_NAME=\"お気に入り物件\" $0 ui"
      exit 1
    fi
    init_workflow
    local rd_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md"
    if [ ! -f "$rd_file" ]; then
      log_warn "要件定義書が見つかりません: $rd_file"
      log_warn "先に requirements を実行してください:"
      log_warn "  FEATURE_NAME=\"$FEATURE_NAME\" $0 requirements"
      echo ""
      read -p "要件定義なしでUI設計を続行しますか? (y/N): " confirm
      if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        exit 1
      fi
    fi
    design_ui
    ;;
  api)
    # API・DB設計のみ (要件定義とUI設計が存在することを確認)
    if [ -z "$FEATURE_NAME" ]; then
      echo "Usage: FEATURE_NAME=\"機能名\" $0 api"
      echo ""
      echo "Example:"
      echo "  FEATURE_NAME=\"お気に入り物件\" $0 api"
      exit 1
    fi
    init_workflow
    local rd_file="$DESIGN_OUTPUT_DIR/RD-${FEATURE_NAME}.md"
    local ui_file="$DESIGN_OUTPUT_DIR/ui/SCR-${FEATURE_NAME}.md"
    missing_files=""
    if [ ! -f "$rd_file" ]; then
      missing_files="$missing_files\n  - 要件定義: $rd_file"
    fi
    if [ ! -f "$ui_file" ]; then
      missing_files="$missing_files\n  - UI設計: $ui_file"
    fi
    if [ -n "$missing_files" ]; then
      log_warn "前工程の設計書が見つかりません:"
      echo -e "$missing_files"
      log_warn ""
      log_warn "先に前工程を実行してください:"
      log_warn "  FEATURE_NAME=\"$FEATURE_NAME\" $0 requirements"
      log_warn "  FEATURE_NAME=\"$FEATURE_NAME\" $0 ui"
      echo ""
      read -p "前工程なしでAPI設計を続行しますか? (y/N): " confirm
      if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        exit 1
      fi
    fi
    design_api
    ;;
  design)
    # 全設計フェーズ (要件定義 → UI設計 → API設計)
    if [ -z "$FEATURE_NAME" ]; then
      echo "Usage: FEATURE_NAME=\"機能名\" $0 design"
      echo ""
      echo "Example:"
      echo "  FEATURE_NAME=\"お気に入り物件\" $0 design"
      exit 1
    fi
    init_workflow
    design_phase
    ;;
  implement)
    init_workflow
    implement_phase
    ;;
  test-backend)
    init_workflow
    backend_e2e
    ;;
  test-frontend)
    init_workflow
    frontend_e2e
    ;;
  all)
    if [ -z "$FEATURE_NAME" ]; then
      echo "Usage: FEATURE_NAME=\"機能名\" $0 all"
      exit 1
    fi
    run_all
    ;;
  *)
    echo "Usage: FEATURE_NAME=\"機能名\" $0 [requirements|ui|api|design|implement|test-backend|test-frontend|all]"
    echo ""
    echo "Design phases (individual):"
    echo "  requirements  - 要件定義のみ"
    echo "  ui            - UI設計のみ (要件定義後に実行推奨)"
    echo "  api           - API・DB設計のみ (UI設計後に実行推奨)"
    echo ""
    echo "Combined steps:"
    echo "  design        - 全設計フェーズ (requirements → ui → api)"
    echo "  implement     - 実装フェーズ"
    echo "  test-backend  - Backend E2E テスト"
    echo "  test-frontend - Frontend E2E テスト"
    echo "  all           - 全ワークフロー"
    echo ""
    echo "Examples:"
    echo "  FEATURE_NAME=\"お気に入り物件\" $0 requirements  # 要件定義のみ"
    echo "  FEATURE_NAME=\"お気に入り物件\" $0 ui            # UI設計のみ"
    echo "  FEATURE_NAME=\"お気に入り物件\" $0 api           # API設計のみ"
    echo "  FEATURE_NAME=\"お気に入り物件\" $0 design        # 全設計"
    echo "  FEATURE_NAME=\"お気に入り物件\" $0 all           # Full workflow"
    exit 1
    ;;
esac
