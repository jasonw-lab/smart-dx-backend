# smart-dx-backend 開発ルール

## 共通ルール
- 全プロジェクト共通ルールは `~/ai-rules/*`（例: `~/ai-rules/ai-common.md`）を参照する。
- 本ファイルは `smart-dx-backend` 固有の責務、アーキテクチャ、運用ルールを定義する。
- 共通ルールと本ファイルが衝突する場合は、ユーザーの明示指示を最優先し、その次に本ファイルのプロジェクト固有ルールを優先する。

## このリポの責務
- 全BEサービスの実装
- 共通ライブラリ (libs/)
- BE側のインフラ (docker-compose, nginx, mysql init)
- BEに関するドキュメント (architecture, ADR, ops)

## アーキテクチャ
**Modular Monolith**: ソースは独立モジュール、デプロイは統合 (ADR-006)。
- 各ドメインモジュール (property-be, system-be, retail-be) はソースレベルで独立
- `app/` が全モジュールを統合し、単一 fat jar (smart-dx-app.jar) としてビルド
- 本番: 1コンテナ / 1 JVM / port 8080 で全ドメインを提供
- 開発: 各モジュールで `mvn spring-boot:run` による個別起動も可能
- libs/ をjar依存で共有
- 全サービスがマルチテナント対応 (libs/smart-be-tenant を利用)

## マルチテナント
- 行レベル分離型 (各テーブルに tenant_id カラム)
- libs/smart-be-tenant で機構を統一
- **tenant マスタの正本は system-be (`sys_tenant`) に一本化** (retail-be は tenant マスタを持たない)
- `TenantStatusChecker` 実装は system-be のみ (`SystemTenantStatusChecker`)
- property-be: 複数テナント運用
- retail-be: 1テナント固定運用 (`sys_tenant` の default テナント id=1 を参照、DEMO用途)
- 詳細: docs/architecture/multi-tenant.md

## サービス間ルール
- サービス間の直接Javaクラス参照は禁止 (libs経由のみ)
- DBスキーマ: smart_dx_db (property/auth/system統合) / retail_db (DEMO用)
- 全テーブルに tenant_id カラム必須
- サービス間通信は REST or Redis Streams、tenant_id をHeader伝搬

## 禁止事項
- libsに業務ロジック・特定ドメインのDTOを置かない
- libsの破壊的変更は1リリースのdeprecate期間を置く
- 「common」というディレクトリ・パッケージは作らない
- ドメインリポ側にBEソースコードを置かない
- tenant_id を含まないテーブル・クエリは書かない

## ドメイン知識
- property: apps/backend/docs/property/ (→ smart-property-dx2/docs へのシンボリックリンク)
  - design/: API仕様、UI仕様、DB設計
  - adr/: アーキテクチャ決定記録
  - AGENTS.md: 自律実行ポリシー
- retail: smart-retail-dx/docs/domain

## DB スキーマ管理
- **system (sys_* テーブル)**: docs/db/smart_dx_db.sql
- **property (property_* テーブル)**: smart-property-dx2/docs/design/db/property_business_schema_v0.4.sql
- **retail**: smart-retail-dx/docs/design/db/ (該当あれば)
- Flyway migration はドメイン固有テーブルには使用しない (既存DBとの互換性維持)

## メモリ目標
- 統合アプリ (smart-dx-app): 512〜768MB (全ドメイン合計)
- 詳細は docs/ops/memory-tuning.md, docs/adr/006-modular-monolith-deployment.md

## 新BEサービス追加
1. `services/<domain>-be/` に新規モジュール作成
2. `app/pom.xml` に依存追加 (1行)
3. `SmartDxApplication` の `@MapperScan` に mapper パッケージ追加
4. 詳細: docs/new-service-guide.md, docs/adr/006-modular-monolith-deployment.md

## 命名規則
- パッケージ: com.smartdx.{domain} (サービス), com.smartdx.{libname} (ライブラリ)
- DB: smart_dx_db (統合DB), retail_db (DEMO専用)
- Redis Key: {service}:{tenant_id}:{業務key}
- JWT Secret: 全サービス共通 (env: JWT_SECRET_KEY)
- 詳細: docs/naming-convention.md

## レビュールール

### レビュー実施時
- レビューファイルは `docs/.review/review_MMDD_対象名.{reviewer}.md` で作成
  - 例: `docs/.review/review_0519_task1-task3.codex.md`
  - reviewer: codex, claude, opus など AI識別子
- 指摘フォーマット:
  ```markdown
  ### 指摘1. タイトル
  YYMMDD HH:MM {reviewer}

  重大度: High / Medium / Low

  詳細...

  推奨:
  - ...
  ```

### 指摘対応時
- 対応完了後、**必ず**元のレビューファイルに対応状況を追記する
- 記入フォーマット:
  ```markdown
  ## YYYY/MM/DD HH:MM {resolver} 対応完了

  ### 対応状況サマリー
  | # | 指摘内容 | 重大度 | 対応状況 |
  |---|---------|-------|---------|
  | 1 | ... | High | ✅ 対応完了 |
  | 2 | ... | Medium | ⚠️ 要対応（別タスク） |

  ### 各指摘の対応詳細
  #### 1. xxx ✅
  - 変更内容: ...
  - 変更ファイル: ...
  ```
- ステータス表記: ✅ 対応完了 / ⚠️ 要対応 / ❌ 対応不要

### スキル
- `/sc:review` - コードレビュー実施
- `/sc:review-respond` - レビュー指摘対応
