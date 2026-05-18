# smart-dx-backend 構築プロンプト

## 1. 背景と目的

### 背景
- 個人開発で複数業界向けのDXプロダクトを展開中
  - `smart-property-dx2` (不動産DX、**先行稼働中、マルチテナント対応済み**)
  - `smart-retail-dx` (小売DX、**実装完了済み、マルチテナント未対応**)
  - 今後、他業界ドメインの追加を想定
- 現状、各ドメインリポにBE実装が同梱されており、VPS上で複数のSpring Boot/JVMコンテナが稼働 → **メモリ逼迫**
- 転職活動でのポートフォリオ用途として、**マルチドメイン設計力・マルチテナント基盤・基盤思考**を可視化したい

### 目的
1. **VPSメモリ削減** — 複数BEを軽量に並べる構成にする
2. **将来のBEサービス追加に耐える** — 新ドメイン追加が低コストで可能
3. **マルチテナント基盤の統一** — 全サービスがマルチテナント前提のコードベースで動く
4. **ポートフォリオとしての見栄え** — 「Smart DXプロダクト群の設計者」「マルチテナントSaaS基盤の設計者」というブランディング

### 非目標 (今回やらないこと)
- DB統合 (MySQLは現状維持)
- Redis統合 (現状維持)
- 大規模なフロントエンド改修
- BEを1つのSpring Bootアプリに統合する「モノリス化」
- retail-be の本格的マルチテナント運用 (DEMOのため1テナント固定で運用)

### 今回の起点と方針
- **`smart-property-dx2` のBEコードをベースにして `smart-dx-backend` を作る**
- `smart-property-dx2` のマルチテナント機構を `libs/smart-be-tenant` として共通化
- `smart-retail-dx` (実装完了済み) を取り込む際に、**コードはマルチテナント対応に統一、ただし運用は1テナント固定** (DEMO用途)
- 全Phaseを4回でAIエージェントに依頼可能な形に集約

---

## 2. マルチテナント設計方針 (重要)

### 採用方式
- **行レベル分離型** (Row-Level Multi-Tenancy)
- 同一MySQLインスタンス、同一テーブルに `tenant_id` カラムを持ち、`WHERE tenant_id = ?` で分離
- テーブルは1つ、DBは各サービス分離 (`property_db` / `retail_db`)

### テナント識別
- 現状の `smart-property-dx2` の仕組みを踏襲 (JWT claim / HTTPヘッダ等、現状実装に合わせる)
- `TenantContext` (ThreadLocal) で1リクエスト内に保持
- JPA Filter / MyBatis Interceptor で自動的に `WHERE tenant_id = ?` 付与

### サービスごとの運用方針

| サービス | コード | 運用 |
|---|---|---|
| property-be | マルチテナント対応 | 複数テナント運用 (smart-property-dx2 現状維持) |
| retail-be | **マルチテナント対応に統一** | **1テナント固定** (`default` で運用、DEMO用途) |
| 将来サービス | マルチテナント対応 | サービスごとに必要に応じて運用 |

### 設計判断の理由
1. **全サービスで同じ前提** — コードベース・libsの設計が統一でき、保守性が高い
2. **将来の拡張性** — retail を本格マルチテナント運用したいとき、運用切替だけで対応可
3. **ポートフォリオ訴求** — 「マルチテナントSaaS基盤を統一設計した」というアピールが可能
4. **実装コスト最小** — retail のテーブルに `tenant_id` を追加し、固定値 `default` で運用するだけ

### ADRに記録
- `docs/adr/005-multi-tenant-strategy.md` にこの判断を記録すること

---

## 3. 全体アーキテクチャ方針

### 基本方針

> **「BEは1リポジトリ(monorepo)に集約、ただし各サービスは独立Spring Boot」**
> **「全サービスがマルチテナント対応のlibsを共有」**

- 各BEサービスは独立したSpring Bootアプリとしてビルド・デプロイ
- 共通機能は `libs/` 配下のjarライブラリとして共有
- マルチテナント機構は `libs/smart-be-tenant` として一元化
- 各サービスを軽量化(JVMチューニング + 将来的にGraalVM native image)し、複数並べてもVPS容量に収める
- ドメインリポからはBE実装を切り出し、フロント+ドキュメントに専念

### サービス間ルール
- サービス間の直接Javaクラス参照は禁止 (`libs/`経由のみ)
- DBスキーマはサービスごとに分離 (`property_db` / `retail_db`)
- 全テーブルに `tenant_id` カラムを持つ
- サービス間通信は REST (同期) または Redis Streams (非同期)
- Redis key prefix は `{service}:{tenant_id}:*` 形式

### 依存方向ルール
```
{service}-be → libs/*  (OK)
libs/* → {service}-be  (NG)
{serviceA}-be → {serviceB}-be (NG: 直接参照禁止)
```

---

## 4. リポジトリ構成

### リポジトリ全体像

| リポジトリ名 | 役割 |
|---|---|
| `smart-property-dx2` | 不動産ドメインのFE + docs (BE実装は持たない) |
| `smart-retail-dx` | 小売ドメインのFE + docs (BE実装は持たない) |
| **`smart-dx-backend`** | **BE基盤monorepo (本リポ)** — 全BEサービス・共通libs・infra |

### 命名規則 (重要)

| パターン | 役割 | 例 |
|---|---|---|
| `smart-{業界}-dx` | 業界特化ドメインリポ (FE + docs) | `smart-property-dx2`, `smart-retail-dx` |
| `smart-dx-{基盤名}` | 横断的な共通基盤 | `smart-dx-backend` |

**`-dx` の位置で役割を区別**する構造とする。READMEで明文化する。

---

## 5. `smart-dx-backend` ディレクトリ構成

```
smart-dx-backend/
├─ apps/
│  └─ backend/
│     ├─ pom.xml                          ← parent pom
│     │
│     ├─ services/
│     │  ├─ property-be/                  ← Phase 2 で作成
│     │  │  ├─ src/main/java/com/smartdx/property/
│     │  │  │  ├─ PropertyApplication.java
│     │  │  │  ├─ controller/
│     │  │  │  ├─ service/
│     │  │  │  ├─ repository/
│     │  │  │  ├─ entity/
│     │  │  │  ├─ dto/
│     │  │  │  ├─ mapper/
│     │  │  │  └─ worker/
│     │  │  ├─ src/main/resources/
│     │  │  │  ├─ application.yml
│     │  │  │  └─ db/migration/           ← Flyway (property_db用)
│     │  │  ├─ pom.xml
│     │  │  ├─ Dockerfile
│     │  │  ├─ openapi.yaml               ← API契約 (SSOT)
│     │  │  └─ README.md
│     │  │
│     │  ├─ retail-be/                    ← Phase 3 で作成
│     │  └─ _template/                    ← 新サービス追加用テンプレ
│     │
│     └─ libs/
│        ├─ smart-be-core/                ← 例外/Response/Util
│        ├─ smart-be-security/            ← JWT/認証共通
│        ├─ smart-be-tenant/              ← マルチテナント機構 ⭐
│        ├─ smart-be-redis/               ← Redis client (tenant prefix統合)
│        ├─ smart-be-opensearch/          ← OpenSearch client共通
│        └─ smart-be-observability/       ← log/metrics/tracing
│
├─ docs/
│  ├─ README.md
│  ├─ naming-convention.md
│  ├─ architecture/
│  │  ├─ overview.md
│  │  ├─ multi-tenant.md                  ← マルチテナント設計詳細 ⭐
│  │  ├─ service-communication.md
│  │  ├─ shared-libs.md
│  │  └─ deployment.md
│  ├─ services/
│  │  ├─ property-be.md
│  │  └─ retail-be.md
│  ├─ new-service-guide.md
│  ├─ adr/
│  │  ├─ 001-monorepo-vs-polyrepo.md
│  │  ├─ 002-why-shared-libs.md
│  │  ├─ 003-memory-optimization.md
│  │  ├─ 004-naming-convention.md
│  │  └─ 005-multi-tenant-strategy.md     ← マルチテナント判断記録 ⭐
│  └─ ops/
│     ├─ memory-tuning.md
│     └─ runbook.md
│
├─ platform/
│  ├─ docker-compose.yml
│  ├─ docker-compose.dev.yml
│  ├─ docker-compose.rollback.yml
│  ├─ nginx/
│  │  └─ nginx.conf
│  ├─ mysql/
│  │  └─ init/                            ← property_db / retail_db のDDL
│  └─ scripts/
│     ├─ start-all.sh
│     ├─ memory-check.sh
│     └─ clone-related-repos.sh
│
├─ .github/workflows/
│  ├─ property-be-ci.yml
│  ├─ retail-be-ci.yml
│  └─ libs-ci.yml
│
├─ CLAUDE.md
├─ AGENTS.md
└─ README.md
```

---

## 6. 共通ライブラリ設計

### libs粒度の指針

| ライブラリ | 入れるもの | 入れないもの |
|---|---|---|
| `smart-be-core` | `ApiResponse`, `BusinessException`, `ErrorCode`, `PageResponse`, 日時/JSON util | 業務ロジック、ドメインモデル |
| `smart-be-security` | JWT生成/検証、`@CurrentUser`, `SecurityFilterChain` base | ユーザーentity、テナント解決ロジック |
| `smart-be-tenant` ⭐ | `TenantContext` (ThreadLocal)、`TenantResolver` (JWT/Header)、`TenantInterceptor`、JPA `@Filter` または MyBatis Interceptor、`@TenantAware` アノテーション、設定切替機構 | 業務ロジック、固有テナントID |
| `smart-be-redis` | RedisTemplate設定、key prefix強制機構 (テナントprefix統合可) | 業務用key定義 |
| `smart-be-opensearch` | client bean、retry policy | index名、mapping |
| `smart-be-observability` | Micrometer設定、log MDC (tenant_id含む)、tracing | アプリ固有metric |

### `smart-be-tenant` の主要API (smart-property-dx2 の実装をベースに抽出)

```java
// テナントID保持
TenantContext.setTenantId("default");
String tenantId = TenantContext.getTenantId();
TenantContext.clear();

// JPA: エンティティに @Filter を付与
@Entity
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Property { ... }

// HTTP Interceptor: リクエストからtenant_id解決
// → JWT claim or X-Tenant-Id header から取得
// → TenantContext にセット
// → 処理後 clear

// 設定切替
// application.yml:
// smart-be-tenant:
//   enabled: true
//   default-tenant: default      # 固定運用時はこのIDで強制
//   force-default: false         # true なら全リクエストを default扱い
```

### libs管理ルール
- semverで版管理 (`0.3.0` 等)、`SNAPSHOT` 依存はしない
- 破壊的変更は1リリースのdeprecate期間を置く
- libsに業務ロジック・特定ドメインのDTOは置かない
- 「common」というディレクトリは作らない (粒度が崩れる原因)

---

## 7. パッケージ・DB・Redis・OpenSearch設計

### パッケージ
- 各サービス: `com.smartdx.{domain}` (例: `com.smartdx.property`)
- libs: `com.smartdx.{libname}` (例: `com.smartdx.core`, `com.smartdx.tenant`)

### MySQL
- 同一MySQLインスタンス内でデータベース(schema)を分離
- `property_db` / `retail_db` / 将来追加分
- 各サービスは自分のDBにのみ接続
- **全テーブルに `tenant_id VARCHAR(50) NOT NULL` カラムを持つ**
- インデックスは `(tenant_id, ...)` 複合インデックスを基本とする
- Flywayマイグレーションは各サービスの `src/main/resources/db/migration/` で管理

### Redis
- 共有Redisインスタンス
- **key prefix形式**: `{service}:{tenant_id}:{業務key}` (例: `property:tenant-A:search:condition:userId-123`)
- retail は固定 `default` テナントで運用 → `retail:default:cart:userId-456`
- `libs/smart-be-redis` で `smart-be-tenant` と連携し、prefix自動付与

### OpenSearch
- 共有OpenSearch
- index名はサービス・テナント単位ではなくサービス単位 (例: `properties`, `retail-products`)
- ドキュメント内に `tenant_id` フィールドを持ち、検索時に必ず `tenant_id` filterを付与
- `libs/smart-be-opensearch` でquery builderにtenant自動付与機構を提供

---

## 8. メモリ削減戦略

### 目標
- 1コンテナあたり **150〜250MB**

### 適用順
1. **JVMチューニング**: `-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m`
2. **Spring Boot軽量化**: `spring.main.lazy-initialization=true`、不要starter除去
3. **(将来) GraalVM native image**: Spring Boot 3で 80〜120MB / 起動0.1秒

### 計測
- `platform/scripts/memory-check.sh` でBefore/After計測
- 結果を `docs/ops/memory-tuning.md` に記録

---

## 9. API・サービス間連携

### API URL方針
- 既存URLをなるべく維持
- Nginxで振り分け: `/api/property/*` → `property-be`、`/api/retail/*` → `retail-be`

### API契約 (SSOT)
- 各サービスの `services/{name}-be/openapi.yaml` を唯一の正
- FE側はCI経由で取得して TypeScript型生成

### テナントID伝搬
- 現状の smart-property-dx2 の方式を踏襲 (JWT claim 等)
- retail-be も同じ方式で実装、ただし固定値 `default` が常に来るようにFE側で指定 or BE側で強制
- サービス間REST通信時もtenant_idをHeaderで伝搬

---

## 10. ドキュメント配置ルール

| 内容 | 置き場所 |
|---|---|
| 業務ルール、ドメイン知識 | ドメインリポ `docs/domain/` |
| API仕様 (OpenAPI yaml) | smart-dx-backend `services/{name}-be/openapi.yaml` (SSOT) |
| DBスキーマ、マイグレーション | smart-dx-backend `services/{name}-be/src/main/resources/db/` |
| BE実装の説明 | smart-dx-backend `docs/services/{name}-be.md` |
| FE実装の説明 | ドメインリポ `docs/frontend/` |
| デプロイ・運用手順 | smart-dx-backend `docs/ops/` |
| アーキテクチャ全体図 | smart-dx-backend `docs/architecture/` (正) |
| マルチテナント設計 | smart-dx-backend `docs/architecture/multi-tenant.md` |
| 設計判断記録 (ADR) | smart-dx-backend `docs/adr/` |

---

## 11. CLAUDE.md / AGENTS.md

### `smart-dx-backend/CLAUDE.md`
```markdown
# smart-dx-backend 開発ルール

## このリポの責務
- 全BEサービスの実装
- 共通ライブラリ (libs/)
- BE側のインフラ (docker-compose, nginx, mysql init)
- BEに関するドキュメント (architecture, ADR, ops)

## アーキテクチャ
各BEサービスは独立Spring Boot。libs/ をjar依存で共有。
全サービスがマルチテナント対応 (libs/smart-be-tenant を利用)。

## マルチテナント
- 行レベル分離型 (各テーブルに tenant_id カラム)
- libs/smart-be-tenant で機構を統一
- property-be: 複数テナント運用
- retail-be: 1テナント固定運用 (default、DEMO用途)
- 詳細: docs/architecture/multi-tenant.md

## サービス間ルール
- サービス間の直接Javaクラス参照は禁止 (libs経由のみ)
- DBスキーマはサービスごとに分離 (property_db / retail_db)
- 全テーブルに tenant_id カラム必須
- サービス間通信は REST or Redis Streams、tenant_id をHeader伝搬

## 禁止事項
- libsに業務ロジック・特定ドメインのDTOを置かない
- libsの破壊的変更は1リリースのdeprecate期間を置く
- 「common」というディレクトリ・パッケージは作らない
- ドメインリポ側にBEソースコードを置かない
- tenant_id を含まないテーブル・クエリは書かない

## ドメイン知識
- property: https://github.com/{org}/smart-property-dx2/tree/main/docs/domain
- retail: https://github.com/{org}/smart-retail-dx/tree/main/docs/domain

## メモリ目標
- 1サービスあたり 150〜250MB
- 詳細は docs/ops/memory-tuning.md

## 新BEサービス追加
docs/new-service-guide.md 参照
```

### `smart-dx-backend/AGENTS.md`
```markdown
# Agents

## backend-service-agent
担当: 各 services/*-be の実装、entity / repository / service / controller / worker、API契約 (openapi.yaml) の維持、テスト

## libs-agent
担当: libs/smart-be-* の設計・実装、semver管理、破壊的変更時のdeprecate運用、smart-be-tenant のメンテ

## infra-agent
担当: docker-compose、nginx routing、MySQL初期化、メモリチューニング、CI/CD

## docs-agent
担当: ADR、new-service-guide.md、runbook.md、multi-tenant.md、ドメインリポへの相互リンク維持

## qa-agent
担当: E2E確認、メモリ使用量計測、起動確認、ロールバック手順検証、マルチテナント分離検証
```

### ドメインリポの CLAUDE.md
```markdown
# smart-property-dx2 (または smart-retail-dx) 開発ルール

## このリポの責務
- このドメインのフロントエンド
- ドメイン知識ドキュメント
- BEは別リポ (smart-dx-backend) を参照

## 重要: BE実装はこのリポにはない
BE修正が必要な場合は smart-dx-backend リポで作業すること。
このリポの apps/backend/ にBEソースコードを置かないこと。

## 関連リポ
- BE実装: https://github.com/{org}/smart-dx-backend
- 並列ドメイン: https://github.com/{org}/{もう一方のドメインリポ}
```

---

## 12. ドメインリポ側の変更 (property-dx2 / retail-dx 共通)

### 削除するもの
- `apps/backend/` 配下のBEソースコード (`src/`, `pom.xml`, `Dockerfile` 等)
- BE用のCI/CD設定
- BE用のdocker-compose設定

### 残すもの
- `apps/frontend/` (FE実装)
- `docs/domain/` (ドメイン知識)
- `docs/frontend/` (FE実装の説明)
- `platform/` のうちFE用設定のみ

### 追加するもの
- `apps/backend/README.md` (smart-dx-backend へのリンク)
- `CLAUDE.md` の更新

### `apps/backend/README.md` テンプレ
```markdown
# {Domain} Backend

このドメインのバックエンドは別リポジトリで管理されています。

## 実装場所
https://github.com/{org}/smart-dx-backend/tree/main/apps/backend/services/{name}-be

## API仕様 (OpenAPI)
https://github.com/{org}/smart-dx-backend/blob/main/apps/backend/services/{name}-be/openapi.yaml

## ドメイン知識 (このリポ)
[docs/domain/](../../docs/domain/)

## ローカル起動方法
smart-dx-backend リポをclone後:
\`\`\`bash
cd smart-dx-backend/platform
docker-compose up {name}-be mysql redis
\`\`\`
```

---

## 13. 実装フェーズ計画 (全4 Phase)

> **このプロンプトは、Phase 1 〜 Phase 4 をAIエージェントに順に依頼することを想定。各Phaseを1回のプロンプトとして投げる。**

### 全体像

| Phase | 内容 | 起点リポ | 完了状態 |
|---|---|---|---|
| Phase 1 | 事前調査 + smart-dx-backend 骨組み作成 | (新規作成) | docker-compose で middleware 起動可、libs骨組み完成 |
| Phase 2 | property-be 切り出し + libs本実装 (smart-be-tenant 含む) | smart-property-dx2 | property-be 動作、libs完成 |
| Phase 3 | retail-be 取り込み + マルチテナント対応化 | smart-retail-dx | retail-be 動作 (1テナント固定運用) |
| Phase 4 | ドメインリポクリーンアップ + 並走切替 + ポートフォリオ整備 | 両ドメインリポ | 本番切替完了、ドキュメント整備済み |

---

### Phase 1: 事前調査 + smart-dx-backend 骨組み作成

**目的**: 依存衝突を事前に潰し、空のmonorepoとして `docker-compose up` が成立する状態を作る

**作業内容**:
1. **依存衝突調査**
   - `smart-property-dx2/apps/backend/` と `smart-retail-dx/apps/backend/` の `mvn dependency:tree` を取得
   - Spring Boot / Spring / Jackson / Lombok / MapStruct 等のバージョン差を一覧化
   - **smart-property-dx2 のマルチテナント実装方式を調査** (どのライブラリ・どの仕組みを使っているか)
   - 結果を `docs/adr/000-dependency-audit.md` に記録

2. **smart-dx-backend リポ作成**
   - GitHub上にリポ作成 (名前: `smart-dx-backend`)
   - ローカルclone

3. **ディレクトリ骨組み作成** (本ドキュメント5章の構成)
   - `apps/backend/pom.xml` (parent pom、Java版・Spring Boot版を定義)
   - `apps/backend/libs/` 配下に **6つのlibs骨組み** (空のpom.xml + パッケージのみ)
     - `smart-be-core` / `smart-be-security` / **`smart-be-tenant`** / `smart-be-redis` / `smart-be-opensearch` / `smart-be-observability`
   - `apps/backend/services/_template/` テンプレ (空のSpring Boot skeleton)
   - `docs/` 配下の主要ディレクトリ作成
   - `platform/` 配下の docker-compose / nginx 雛形

4. **基本ドキュメント作成**
   - `README.md` (本ドキュメント15章のテンプレ使用)
   - `CLAUDE.md` (本ドキュメント11章のテンプレ使用)
   - `AGENTS.md` (本ドキュメント11章のテンプレ使用)
   - `docs/naming-convention.md`
   - `docs/adr/001-monorepo-vs-polyrepo.md`
   - `docs/adr/002-why-shared-libs.md`
   - `docs/adr/004-naming-convention.md`
   - **`docs/adr/005-multi-tenant-strategy.md`** (本ドキュメント2章の判断を記録)
   - **`docs/architecture/multi-tenant.md`** (マルチテナント設計の詳細)

**完了条件**:
- ✅ `smart-dx-backend` リポが作成済み、初期commit済み
- ✅ ディレクトリ構成が5章通りに作成済み (libs **6つ**)
- ✅ `docker-compose up` でmysql/redis/opensearch等のミドルウェアが起動
- ✅ ADR 4本 + naming-convention.md + multi-tenant.md + README + CLAUDE.md + AGENTS.md 作成済み
- ✅ `mvn -f apps/backend/pom.xml compile` が成功 (libsの空骨組みがビルド可能)
- ✅ smart-property-dx2 のマルチテナント実装方式の調査結果が記録されている

---

### Phase 2: property-be 切り出し + libs本実装 (smart-be-tenant 含む)

**目的**: `smart-property-dx2` のBEコードを `smart-dx-backend/services/property-be` に移植し、libs (特に `smart-be-tenant`) を実装、動作させる

**作業内容**:
1. **property-be 移植**
   - `smart-property-dx2/apps/backend/src/` を `smart-dx-backend/apps/backend/services/property-be/src/` にコピー
   - パッケージを `com.smartdx.property` に統一 (必要なら一括リネーム)
   - `pom.xml` を parent pom 配下に組み込み
   - `application.yml` を整理
   - `Dockerfile` 作成

2. **libs本実装** (property-be から切り出せるものを抽出)
   - `smart-be-core`: ApiResponse、BusinessException、ErrorCode、PageResponse、util類
   - `smart-be-security`: JWT/認証共通
   - **`smart-be-tenant` (最重要)**:
     - smart-property-dx2 の現状のマルチテナント実装を抽出してlibs化
     - `TenantContext` (ThreadLocal)
     - `TenantResolver` (JWT claim / X-Tenant-Id header から解決)
     - `TenantInterceptor` (HTTP Interceptor)
     - JPA `@Filter` 設定 or MyBatis Interceptor (smart-property-dx2 の実装方式に合わせる)
     - `@TenantAware` アノテーション
     - `application.yml` 設定切替 (`smart-be-tenant.enabled`, `default-tenant`, `force-default`)
   - `smart-be-redis`: RedisTemplate設定、tenant_id統合のkey prefix機構
   - `smart-be-opensearch`: client bean、query builderにtenant filter自動付与
   - `smart-be-observability`: log MDC (tenant_id 含む)、Micrometer設定
   - **重要**: libsには業務ロジック・property固有のDTOを入れない

3. **property-be が libs に依存する形に修正**
   - 元のutilクラス・マルチテナント機構を削除し、libsに切り替え
   - import文を一括修正
   - 動作確認(マルチテナント分離が機能していること)

4. **DBスキーマ・Flyway**
   - MySQL初期化スクリプト (`platform/mysql/init/`) に `CREATE DATABASE property_db` を追加
   - Flyway migrationを `services/property-be/src/main/resources/db/migration/` に移動
   - `application.yml` の datasource URL を `property_db` に向ける

5. **OpenAPI yaml作成**
   - `services/property-be/openapi.yaml` を作成

6. **JVMチューニング適用**
   - `Dockerfile` で `-Xmx256m -Xms128m -XX:+UseSerialGC` 等を設定
   - `application.yml` で `spring.main.lazy-initialization=true`

7. **docker-compose で property-be を起動可能に**
   - `platform/docker-compose.yml` に property-be サービスを追加
   - mem_limit設定

**完了条件**:
- ✅ property-be が smart-dx-backend 内でビルド成功
- ✅ `docker-compose up` で property-be + mysql + redis + opensearch が起動
- ✅ property-be の主要API (物件検索、類似物件、登録等) が動作
- ✅ **マルチテナント分離が機能している** (テナントA のデータは テナントB から見えない)
- ✅ libs 6つが実装済みで property-be が依存している
- ✅ libsに業務ロジックが混入していないこと
- ✅ メモリ使用量を計測し `docs/ops/memory-tuning.md` に記録
- ✅ `services/property-be/openapi.yaml` 作成済み
- ✅ `docs/architecture/multi-tenant.md` が実装に即した内容に更新済み

---

### Phase 3: retail-be 取り込み + マルチテナント対応化

**目的**: `smart-retail-dx` のBEコードを `smart-dx-backend/services/retail-be` に移植し、マルチテナント対応に統一 (1テナント固定運用)

**作業内容**:
1. **retail-be 移植**
   - `smart-retail-dx/apps/backend/src/` を `smart-dx-backend/apps/backend/services/retail-be/src/` にコピー
   - パッケージを `com.smartdx.retail` に統一
   - `pom.xml` を parent pom 配下に組み込み
   - `Dockerfile` 作成

2. **libs適用** (Phase 2 で作ったlibsをそのまま利用)
   - `smart-be-core`, `smart-be-security`, `smart-be-tenant`, `smart-be-redis`, `smart-be-opensearch`, `smart-be-observability` を依存に追加
   - 元のretailのutil・例外・Response類をlibsに切り替え

3. **マルチテナント対応化 (最重要作業)**
   - **全テーブルに `tenant_id VARCHAR(50) NOT NULL DEFAULT 'default'` カラムを追加**
   - Flyway migration スクリプトを作成 (既存テーブル変更用、`ALTER TABLE` 文)
   - **既存データに `tenant_id = 'default'` を一括update**
   - 全インデックスを `(tenant_id, ...)` 複合に再作成
   - 全Entity に `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")` を追加 (または MyBatis Interceptor 適用)
   - Repository層のクエリにtenant filterが自動適用されることを確認
   - Redis key を `retail:{tenant_id}:*` 形式に修正 (`smart-be-redis` 経由)
   - OpenSearch document に `tenant_id` フィールド追加、検索に filter 追加

4. **1テナント固定運用設定**
   - `application.yml` で:
     ```yaml
     smart-be-tenant:
       enabled: true
       default-tenant: default
       force-default: true   # 全リクエストを default テナントで処理
     ```
   - これにより、FE側がtenant_idを送らなくても全て `default` で処理される

5. **DBスキーマ・Flyway**
   - MySQL初期化スクリプトに `CREATE DATABASE retail_db` を追加
   - Flyway migrationを `services/retail-be/src/main/resources/db/migration/` に移動
   - `application.yml` の datasource URL を `retail_db` に向ける

6. **OpenAPI yaml作成**
   - `services/retail-be/openapi.yaml` を作成

7. **JVMチューニング適用**
   - Phase 2 と同じ設定

8. **docker-compose / nginx 追加**
   - `platform/docker-compose.yml` に retail-be サービス追加
   - `platform/nginx/nginx.conf` に `/api/retail/*` → `retail-be` の routing 追加

9. **ドキュメント更新**
   - `docs/services/retail-be.md` 作成
   - `docs/architecture/overview.md` を retail-be 追加版に更新
   - `docs/adr/006-retail-multi-tenant-migration.md` 作成 (retail のマルチテナント化判断の記録)

**完了条件**:
- ✅ retail-be が smart-dx-backend 内でビルド成功
- ✅ `docker-compose up` で property-be + retail-be が共存稼働
- ✅ retail-be の主要API (商品/在庫/注文等) が動作
- ✅ **全retailテーブルに `tenant_id` カラムがある**、既存データは全て `tenant_id = 'default'`
- ✅ **force-default 設定で1テナント固定運用が動作している**
- ✅ Redis key が `retail:default:*` 形式になっている
- ✅ メモリ使用量を計測し `docs/ops/memory-tuning.md` に追記
- ✅ ドキュメント (services/retail-be.md, ADR 006) 作成済み

---

### Phase 4: ドメインリポクリーンアップ + 並走切替 + ポートフォリオ整備

**目的**: ドメインリポからBEを削除、本番並走 → 切替、メモリ削減効果確認、ポートフォリオとして仕上げる

**作業内容**:
1. **ドメインリポ (smart-property-dx2 / smart-retail-dx) のBEコード削除**
   - 各リポで、旧BEイメージにタグ付け保存 (`docker tag ...:latest ...:pre-merge`)
   - `apps/backend/src/`, `pom.xml`, `Dockerfile` 等を削除
   - BE用のCI/CD設定削除
   - BE用のdocker-compose設定削除
   - `apps/backend/README.md` 作成 (本ドキュメント12章のテンプレ)
   - `CLAUDE.md` を更新 (本ドキュメント11章のテンプレ)
   - ルート `README.md` で BEは別リポであることを明記

2. **smart-dx-backend 側の最終ドキュメント整備**
   - `docs/services/property-be.md` を最終化 (smart-property-dx2 の docs/domain を参照)
   - `docs/services/retail-be.md` を最終化
   - `docs/architecture/overview.md` を最終化 (全体図含む)
   - `docs/adr/003-memory-optimization.md` 作成 (Phase 2-3 の計測結果を反映)
   - `docs/ops/runbook.md` 作成 (起動・停止・トラブルシューティング)
   - `docs/new-service-guide.md` 完成 (新BEサービス追加手順)

3. **ロールバック手順整備**
   - `platform/docker-compose.rollback.yml` 作成 (旧 `*:pre-merge` イメージを使う構成)
   - ロールバック手順を `docs/ops/runbook.md` に記載
   - 実際に切り戻し → 切り戻しできることを検証

4. **CI/CD整備**
   - `.github/workflows/property-be-ci.yml` 作成 (path filterで `services/property-be/**` のみbuild)
   - `.github/workflows/retail-be-ci.yml` 作成
   - `.github/workflows/libs-ci.yml` 作成 (libs変更時に全サービスbuild)

5. **VPS上で並走運用**
   - 旧コンテナを止めず、smart-dx-backend を**別ポート**で起動
   - Nginxで一部APIだけ新サービスに流す (カナリアリリース)
   - 数日運用してエラー・遅延を監視

6. **完全切替**
   - Nginxルーティングを全API smart-dx-backend に切替
   - 旧コンテナ停止
   - 問題発生時は `docker-compose.rollback.yml` で即座に切り戻し

7. **メモリ計測 (Before/After)**
   - VPS全体のメモリ使用量を Before / After で記録
   - 各コンテナのメモリ使用量を `memory-check.sh` で計測
   - 結果を `docs/ops/memory-tuning.md` に追記

8. **(任意) GraalVM native image PoC**
   - property-be / retail-be を Spring Boot 3 + native image化を試す
   - 結果を `docs/adr/003-memory-optimization.md` に追記

9. **ポートフォリオ整備**
   - `smart-dx-backend/README.md` の冒頭価値訴求文を整える (本ドキュメント15章)
   - GitHubプロフィールのPinned設定 (本ドキュメント15章)
   - アーキテクチャ図 (`docs/architecture/overview.md`) を画像化
   - `docs/architecture/multi-tenant.md` の図も整備

**完了条件**:
- ✅ smart-property-dx2 / smart-retail-dx から BEコードが完全削除
- ✅ 各ドメインリポの apps/backend/README.md が smart-dx-backend へのリンクのみ
- ✅ VPS上で smart-dx-backend が本番稼働、旧コンテナは停止
- ✅ メモリ削減効果が数値で記録されている (Before/After)
- ✅ ロールバック手順が文書化され、検証済み
- ✅ CI/CD が設定され、PRでビルドが走る
- ✅ README/Pinned/アーキテクチャ図がポートフォリオとして整っている
- ✅ ドキュメント類 (ADR 6本以上、multi-tenant.md, runbook.md, new-service-guide.md) が完成

---

## 14. リスクと対策

| リスク | 対策 |
|---|---|
| 依存ライブラリのバージョン衝突 | Phase 1 で事前調査、parent pom で版固定 |
| libs肥大化・業務ロジック混入 | libs粒度ルール明文化、PRレビュー時にチェック |
| サービス間の直接参照 | Maven moduleで依存方向を強制、ビルド時に検知 |
| マルチテナント分離漏れ | Phase 2/3 で必ず分離テストを実施 (tenant A から B のデータが見えないこと) |
| retail データ移行ミス | Phase 3 で `tenant_id = 'default'` の一括update前にバックアップ取得 |
| Single Point of Failure | サービスごとにmem_limit設定 |
| デプロイ結合 | サービスごとに独立Dockerイメージ |
| API契約のFE/BEズレ | OpenAPI yamlをSSOTとし、FE側で型自動生成 |
| ロールバック失敗 | docker-compose.rollback.yml、旧イメージタグ保持 |
| force-default 解除し忘れ | retail-be で本格マルチテナント運用に切り替える際の手順を runbook に明記 |

---

## 15. ポートフォリオ視点の見せ方

### GitHub プロフィール Pinned
```
⭐ smart-dx-backend         BE基盤monorepo + マルチテナント基盤
⭐ smart-property-dx2       不動産DXプロダクト
⭐ smart-retail-dx          小売DXプロダクト
```

### `smart-dx-backend/README.md` 冒頭 (例)
```markdown
# Smart DX Backend

> 複数業界向けDXプロダクトの共通BE基盤monorepo。
> 行レベル分離型マルチテナント機構を共通ライブラリ化し、
> 全サービスで統一されたSaaS基盤として運用。
> Spring Boot multi-module + JVM軽量化で
> VPS上に複数BEサービスを並べる構成を実現。

## このリポの役割
Smart DX シリーズ (不動産DX / 小売DX / 今後追加予定) の
バックエンドサービスを集約した monorepo。

## 関連リポジトリ
- [smart-property-dx2](link) - 不動産ドメイン (FE + docs、複数テナント運用)
- [smart-retail-dx](link) - 小売ドメイン (FE + docs、1テナント固定運用)

## 設計思想
- マルチドメインを単一monorepoで運用、共通libsで DRY 維持
- 全サービスがマルチテナント対応 (libs/smart-be-tenant)
- 各サービスは独立Spring Boot、デプロイ単位を分離
- VPS制約を意識した軽量化 (1サービス 150-250MB目標)
- 詳細: [docs/architecture/overview.md](link) / [docs/architecture/multi-tenant.md](link)
```

### 面接での説明テンプレ
> 「VPSのメモリ制約に対応するため、各BEアプリをJVMチューニングで軽量化し、共通機能は共有ライブラリmoduleに切り出してDRYを保ちました。
> 特にマルチテナント機構は `smart-be-tenant` として共通化し、不動産ドメインは複数テナント、小売ドメインは1テナント固定として、**同一コードベースで運用形態を切り替え可能な設計**としています。
> 将来のBEサービス追加を見据え、サービス間はDB非共有・REST/メッセージング連携の方針で疎結合を維持。
> リポジトリ構成も、業界特化ドメインは `smart-{業界}-dx`、横断基盤は `smart-dx-{役割}` という命名規則で役割を明示しています。」

---

## 16. このプロンプトの使い方

このmarkdownは、Claude Code / Codex / その他AIエージェントへの指示書として使用する。

### 推奨される実行単位
**全体を1回で投げず、Phase 1〜4 をそれぞれ別プロンプトとして投げる**。

例:
- 1回目: 「添付の smart-dx-backend-prompt.md を読み、**Phase 1** のみを実施してください」
- 2回目 (Phase 1完了後): 「**Phase 2** を実施してください。特に `smart-be-tenant` の抽出に注意」
- 3回目 (Phase 2完了後): 「**Phase 3** を実施してください。retail のマルチテナント化に細心の注意」
- 4回目 (Phase 3完了後): 「**Phase 4** を実施してください」

各Phase完了時に人手レビューを挟むこと。

### Phase完了確認
各Phaseの「完了条件」を満たしているか、次のPhase開始前に必ず確認する。
特に Phase 2 完了時の「マルチテナント分離テスト」と Phase 3 完了時の「force-default 動作確認」は必須。

### 重要な事前情報
Phase 1 開始前に、AIエージェントに以下を明確に伝えること:
- smart-property-dx2 はマルチテナント対応済み
- smart-retail-dx はマルチテナント未対応 (実装完了済み)
- 統合後、retail-be はコード上マルチテナント対応するが運用は1テナント固定
- マルチテナント機構の出所は smart-property-dx2 の実装
