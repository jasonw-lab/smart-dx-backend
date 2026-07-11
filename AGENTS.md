# Agents

> 本ファイルは **AI コーディングエージェント**向けのガイドです。プロジェクトの全体像、技術スタック、ビルド・テスト手順、コーディング規約、セキュリティ上の注意、レビュールールをまとめています。
> 人間向けの概要は [README.md](README.md)、詳細な開発ルールは [CLAUDE.md](CLAUDE.md) を参照してください。
> 全プロジェクト共通ルールは `~/ai-rules/*`（例: `~/ai-rules/ai-common.md`）を参照してください。本ファイルは `smart-dx-backend` 固有ルールを定義します。
> ドキュメントは日本語が中心です。コメント・ドキュメント・レビュー記録も日本語で記述してください。

---

## プロジェクト概要 (Project Overview)

`smart-dx-backend` は、複数業界向け DX プロダクト群（不動産 DX / 小売 DX / 今後追加予定）の共通バックエンド基盤となる **monorepo** です。

- **目的**: 行レベル分離型マルチテナント機構を共通ライブラリ化し、複数 BE サービスを統一された SaaS 基盤として運用する。
- **アーキテクチャ**: **Modular Monolith**（ソースはドメイン単位の独立モジュール、デプロイは単一 fat jar）。詳細は [docs/adr/006-modular-monolith-deployment.md](docs/adr/006-modular-monolith-deployment.md)。
- **関連リポジトリ**:
  - [smart-property-dx2](https://github.com/ross-dev2024/smart-property-dx2) — 不動産ドメイン（FE + docs）
  - [smart-retail-dx](https://github.com/ross-dev2024/smart-retail-dx) — 小売ドメイン（FE + docs）

### 技術スタック

| 領域 | 技術 | バージョン |
|------|------|-----------|
| 言語 | Java | 21 |
| フレームワーク | Spring Boot | 3.3.6 |
| ORM | MyBatis-Plus | 3.5.15 |
| DB | MySQL | 8.0 |
| キャッシュ | Redis | 7 |
| 検索 | OpenSearch | 2.11 |
| ビルド | Maven | — |
| コンテナ | Docker / Docker Compose | — |
| API ドキュメント | Knife4j / SpringDoc | — |
| ユーティリティ | Lombok, MapStruct, Hutool | — |

### ディレクトリ構成

```
smart-dx-backend/
├── apps/backend/
│   ├── pom.xml                         # 親 POM（BOM・プラグイン管理）
│   ├── services/                       # ドメインモジュール（ライブラリ化）
│   │   ├── property-be/                # 不動産ドメイン（マルチテナント運用）
│   │   ├── system-be/                  # システム管理・認証（auth-be は統合済み）
│   │   ├── retail-be/                  # 小売ドメイン（1 テナント固定運用）
│   │   └── _template/                  # 新サービス追加用テンプレート
│   ├── libs/                           # 共通ライブラリ
│   │   ├── smart-be-core/              # 共通ユーティリティ・例外・レスポンス
│   │   ├── smart-be-security/          # JWT / 認証フィルタ
│   │   ├── smart-be-tenant/            # マルチテナント機構
│   │   ├── smart-be-redis/             # テナント対応 Redis key prefix
│   │   ├── smart-be-opensearch/        # OpenSearch 共通クライアント
│   │   └── smart-be-observability/     # ログ/MDC
│   └── app/                            # 統合 bootstrap モジュール
│       ├── src/main/java/com/smartdx/app/SmartDxApplication.java
│       ├── src/main/resources/application.yml
│       └── Dockerfile
├── docs/                               # ADR / アーキテクチャ / 運用ドキュメント
├── platform/                           # Docker Compose / nginx / MySQL init
└── .github/workflows/                  # CI 定義
```

### Modular Monolith について

- `services/*-be` は **独立した Maven モジュール**としてソース管理されるが、**それぞれ単独の Spring Boot アプリではない**。
- 各サービスには `@SpringBootApplication` は存在せず、`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` によって `app` から自動構成される。
- `app` モジュールが全ドメインを依存に取り込み、**単一の fat jar (`smart-dx-app.jar`)** を生成する。
- 本番は **1 コンテナ / 1 JVM / port 8080** で全ドメインを提供する。
- nginx は `localhost:80` で受け、`/api/*` を `smart-dx-be:8080` にリバースプロキシする。

### マルチテナント

- **方式**: 行レベル分離型（各テーブルに `tenant_id` カラムを持ち、`WHERE tenant_id = ?` で分離）。
- **実装**: `libs/smart-be-tenant` で一元化。`TenantContextFilter` → `TenantContextHolder` → MyBatis-Plus `TenantLineHandler` の流れで自動付与。
- **tenant マスタの正本**: `system-be` の `sys_tenant` のみ。他ドメインは libs 経由で状態を参照する。
- **運用形態**:
  - `property-be`: 複数テナント運用（`force-default: false`）
  - `retail-be`: 1 テナント固定運用（`force-default: true` / デフォルト tenant_id=1）
- 詳細: [docs/architecture/multi-tenant.md](docs/architecture/multi-tenant.md)

---

## エージェント役割

### backend-service-agent
- 各 `services/*-be` の実装
- entity / repository(mapper) / service / controller / worker
- `openapi.yaml` の維持
- 単体・E2E テスト

### libs-agent
- `libs/smart-be-*` の設計・実装
- semver 管理、破壊的変更時の deprecate 運用
- `smart-be-tenant` のメンテナンス

### infra-agent
- docker-compose、nginx routing、MySQL 初期化
- JVM / メモリチューニング
- CI/CD（`.github/workflows/`）

### docs-agent
- ADR、`new-service-guide.md`、`runbook.md`、ドメインリポとの相互リンク維持

### qa-agent
- E2E 確認、メモリ使用量計測、起動確認
- ロールバック手順検証
- マルチテナント分離検証

### ドメインドキュメント参照
各ドメインの DB 設計・仕様書は該当ドメインプロジェクトで管理する:
- **system**: `docs/db/smart_dx_db.sql`（`sys_*` テーブル）
- **property**: `smart-property-dx2/docs/design/db/property_business_schema_v0.4.sql`
- **retail**: `smart-retail-dx/docs/design/db/`

---

## ビルド・実行コマンド

### 前提
- JDK 21
- Maven 3.9+
- Docker / Docker Compose（ミドルウェア起動・Testcontainers 用）

### ミドルウェア起動

```bash
cd platform
docker-compose up -d mysql redis opensearch
```

初回は MySQL init スクリプトにより `smart_dx_db` と `retail_db` が作成される。system / property テーブルは `docs/db/smart_dx_db.sql` を手動で流す（Flyway は統合アプリでは無効化されているため）。

### フルビルド

```bash
cd apps/backend
mvn clean compile          # コンパイルのみ
mvn clean package          # 全モジュール jar 生成
mvn clean verify           # テスト込み
```

### 統合アプリの実行（ローカル開発）

```bash
cd apps/backend
mvn -pl app spring-boot:run
```

- ポート: `8080`
- API エントリポイント: `http://localhost:8080/api/...`
- ドキュメント: `http://localhost:8080/doc.html`

### Docker 実行（本番相当）

```bash
cd platform
docker-compose up -d
```

- コンテナ: `smart-dx-be`（`apps/backend/app/Dockerfile` 使用）
- nginx: `localhost:80`
- BE 直接: `localhost:8080`

注意:
- `apps/backend/Dockerfile` は旧個別サービス用の残骸であり、現在は使用しない。有効なのは `apps/backend/app/Dockerfile` である。
- `platform/scripts/start-all.sh` はミドルウェアと nginx のみ起動する。`smart-dx-be` コンテナを起動する場合は `docker-compose up -d smart-dx-be` または `docker-compose up -d` を使用する。

---

## テスト

### 単体テスト

```bash
cd apps/backend
mvn test
```

### 統合 / E2E テスト

- `app/src/test/java/com/smartdx/app/` に統合テストがある:
  - `AuthE2ETest`
  - `MultiTenantIntegrationTest`
  - `TenantIsolationSecurityTest`
- `services/property-be/src/test/java/com/smartdx/property/` にドメイン E2E テストがある。
- これらは **Testcontainers** を使用する。Docker が起動している必要がある。
- `app/pom.xml` では macOS + OrbStack 用に Docker socket パスを設定している。Docker Desktop 使用時は環境変数で上書きが必要な場合がある。

```bash
cd apps/backend
mvn -pl app verify
```

### 手動 E2E スクリプト

```bash
./scripts/e2e-test.sh http://localhost:8080
```

- デフォルトの `BASE_URL` は `http://localhost:8081` となっているが、統合アプリでは `http://localhost:8080`（または nginx 経由 `http://localhost`）を指定する。

### CI

- `.github/workflows/libs-ci.yml` — libs 変更時に全共通ライブラリを `verify`
- `.github/workflows/property-be-ci.yml` — property-be 変更時にビルド・テスト、jar アーティファクトをアップロード
- 現時点で `system-be`、`retail-be` 専用の CI はない。追加時は `.github/workflows/{domain}-be-ci.yml` を作成する。

---

## コーディング規約

### 命名規則

| 対象 | パターン | 例 |
|------|---------|-----|
| サービスモジュール | `{domain}-be` | `property-be`, `retail-be` |
| ライブラリモジュール | `smart-be-{機能}` | `smart-be-core`, `smart-be-tenant` |
| サービスパッケージ | `com.smartdx.{domain}` | `com.smartdx.property` |
| ライブラリパッケージ | `com.smartdx.{libname}` | `com.smartdx.tenant` |
| DB 名 | `{domain}_db` | `smart_dx_db`, `retail_db` |
| テナントカラム | `tenant_id` | 全テーブル共通 |
| Redis Key | `{service}:{tenant_id}:{業務key}` | `property:1:search:condition:user-123` |
| Docker イメージ | `smart-dx/{service}:{version}` | `smart-dx/smart-dx-app:1.0.0` |

- 詳細: [docs/naming-convention.md](docs/naming-convention.md)

### 禁止事項

- `common` というディレクトリ・パッケージは作成しない。
- サービス間の直接 Java クラス参照は禁止。連携は libs 経由、REST API、または Spring Event とする。
- libs に業務ロジック・特定ドメインの DTO を置かない。
- libs の破壊的変更は、可能な限り 1 リリースの deprecate 期間を置く。
- ドメインリポジトリ側に BE ソースコードを置かない。
- `tenant_id` を含まないテーブル・クエリは書かない。

### モジュール構成ルール

- 各サービスは `controller/`, `service/`, `mapper/`, `entity/`, `dto/` or `model/`, `config/` に分ける。
- 新規ドメイン追加時は `services/_template/` をコピーし、[docs/new-service-guide.md](docs/new-service-guide.md) に従う。
- 新規ドメインは `app/pom.xml` に 1 行依存追加が必要。
- `SmartDxApplication` の `@MapperScan` に新しい mapper パッケージを追加する必要がある。

### AutoConfiguration

- libs / services は Spring Boot の AutoConfiguration メカニズムで読み込まれる。
- 構成クラスは `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` に登録する。
- `UnifiedSecurityConfig`（`app`）が単一の `SecurityFilterChain` を提供するため、各サービスで独自の `@EnableWebSecurity` は避ける。

---

## セキュリティ考慮事項

### 認証・認可

- JWT シークレットは全ドメイン共通。本番では **必ず環境変数 `JWT_SECRET_KEY` で強力な秘密鍵を設定**する。
- 未認証許可パスは `security.ignore-urls` / `security.unsecured-urls` で一元管理（例: `/api/v1/auth/**`, `/api/captcha/**`, `/actuator/health`）。
- `TokenAuthenticationFilter` で JWT を検証し、`SecurityContext` に格納する。

### マルチテナント分離

- 全テーブルに `tenant_id` カラムを設け、インデックスは `(tenant_id, ...)` の複合インデックスを基本とする。
- MyBatis-Plus の `TenantLineHandler` が SELECT/UPDATE/DELETE に自動で `WHERE tenant_id = ?` を付与する。
- `TenantContextHolder` は `TransmittableThreadLocal` を使用し、非同期処理でも値が伝播する。
- システムテーブル等、テナントフィルタを一時的に無視する必要がある場合:

```java
TenantContextHolder.setIgnoreTenant(true);
try {
    // システムテーブル操作
} finally {
    TenantContextHolder.setIgnoreTenant(false);
}
```

- 定期的にクロステナントアクセスがないことをテストで確認する。

### 設定・秘匿情報

- 秘匿情報は `.env` で管理し、リポジトリにコミットしない（`.gitignore` に `.env` を登録済み）。
- `EnvLoggingConfig` は起動時に環境変数をログ出力するが、`PASSWORD`, `SECRET`, `KEY`, `TOKEN`, `CREDENTIAL` を含む値はマスクする。
- 本番では nginx 前段に TLS 終端を配置すること。

---

## デプロイ・運用

### Docker Compose

- `platform/docker-compose.yml` で以下を定義:
  - `mysql`（port 3306）
  - `redis`（port 6379）
  - `opensearch`（port 9200 / 9600）
  - `smart-dx-be`（port 8080, `mem_limit: 768m`）
  - `nginx`（port 80）

### メモリ目標

- 統合アプリ全体で **512〜768MB** を目標とする。
- `app/Dockerfile` では `-Xmx512m -Xms256m -XX:+UseG1GC` 等を設定。
- 計測: `./platform/scripts/memory-check.sh`
- 詳細: [docs/ops/memory-tuning.md](docs/ops/memory-tuning.md)

### 運用コマンド

```bash
# 全サービス停止
cd platform && docker-compose down

# ログ確認
docker-compose logs -f smart-dx-be

# ヘルスチェック
curl http://localhost/actuator/health
```

- 詳細な手順: [docs/ops/runbook.md](docs/ops/runbook.md)

---

## 共通ルール: AI 間レビュー

### レビュー実施 (`/review`)

1. **ファイル命名規則**
   - `docs/.review/review_MMDD_対象名.{reviewer}.md`
   - reviewer: AI 識別子（codex, claude, opus など）

2. **指摘フォーマット**
   ```markdown
   ### 指摘1. {タイトル}
   YYMMDD HH:MM {reviewer}

   重大度: High / Medium / Low

   {詳細}

   推奨:
   - ...
   ```

3. **重大度基準**
   - **High**: ビルド失敗、セキュリティ、データ整合性
   - **Medium**: テスト失敗、API 契約違反、ベストプラクティス
   - **Low**: スタイル、ドキュメント、軽微な改善

### 指摘対応 (`/review-respond`)

1. **対応完了時の記録義務**
   - 指摘対応完了後、元のレビューファイルに対応状況を追記する
   - 日時、対応者、各指摘の対応状況を記録する

2. **記入フォーマット**
   ```markdown
   ## YYYY/MM/DD HH:MM {resolver} 対応完了

   ### 対応状況サマリー
   | # | 指摘内容 | 重大度 | 対応状況 |
   |---|---------|-------|---------|
   | 1 | ... | High | ✅ 対応完了 |
   | 2 | ... | Medium | ⚠️ 要対応（別タスク） |

   ### 各指摘の対応詳細
   #### 指摘1. xxx ✅
   - 変更内容: ...
   - 変更ファイル: ...

   ### 残課題
   - ...
   ```

3. **ステータス表記**
   - ✅ 対応完了
   - ⚠️ 要対応（別タスク）
   - ❌ 対応不要（理由を明記）

### スキルファイル
- `.claude/skills/review.md` — レビュー実施手順
- `.claude/skills/review-respond.md` — 指摘対応手順
