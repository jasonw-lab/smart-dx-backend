# Codex Review: Phase 1 / Phase 2

対象: `feature/phase1-skeleton` (`main...HEAD`)
基準: `smart-dx-backend-prompt.md` の Phase 1 / Phase 2 完了条件
実施日: 2026-05-18

## 結論

Phase 1 の骨組みと Maven ビルドは概ね成立していますが、Phase 2 は未完了です。特に `property-be` の実体移植、テナント解決、Flyway migration、API実装、マルチテナント分離テストが欠落しており、この状態を「Phase 2 実行済み」として扱うと後続 Phase 3 の retail 取り込みで基盤前提が崩れます。

## Findings

### [Critical] Phase 2 の property-be 移植が実質未実施

- 根拠:
  - `apps/backend/services/property-be/src/main/java` 配下の Java ファイルは 1 件のみ。
  - [PropertyApplication.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/services/property-be/src/main/java/com/smartdx/property/PropertyApplication.java:14) はアプリ起動クラスだけで、`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `worker/` が存在しません。
  - [openapi.yaml](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/services/property-be/openapi.yaml:29) は `/auth/login`, `/properties`, `/properties/search/similar` などの API を定義していますが、対応する controller 実装がありません。
- 影響:
  - Phase 2 完了条件の「主要APIが動作」「property-be 動作」「smart-property-dx2 BEコードを移植」が満たせません。
  - OpenAPI が実装と乖離しており、API契約のSSOTとして信用できません。
- 推奨修正:
  - `smart-property-dx2/apps/backend/src/` から property ドメインの controller/service/repository/entity/mapper/dto/worker を移植する。
  - パッケージを `com.smartdx.property` に統一し、OpenAPI は実装済み API に合わせて再生成または修正する。

### [Critical] `smart-be-tenant` にリクエストからテナントを解決する経路がない

- 根拠:
  - [TenantAutoConfiguration.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-tenant/src/main/java/com/smartdx/tenant/config/TenantAutoConfiguration.java:32) は `TenantLineHandler`, `TenantAspect`, `MybatisPlusInterceptor` のみを登録しています。
  - Phase 2 要件の `TenantResolver` と `TenantInterceptor` / HTTP filter が存在しません。
  - [TenantLineHandler.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-tenant/src/main/java/com/smartdx/tenant/mybatis/TenantLineHandler.java:55) は `TenantContextHolder` が空の場合に [defaultTenantId へフォールバック](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-tenant/src/main/java/com/smartdx/tenant/mybatis/TenantLineHandler.java:57) します。
- 影響:
  - JWT claim / `X-Tenant-Id` からテナントが設定されないため、全リクエストが `tenant_id = 1` になり得ます。
  - property-be の複数テナント運用で、テナントA/Bの切り替えや分離確認ができません。
- 推奨修正:
  - `TenantResolver` を追加し、優先順を `SecurityContext` / JWT claim / `X-Tenant-Id` / default のように明文化する。
  - `OncePerRequestFilter` または `HandlerInterceptor` で `TenantContextHolder.setTenantId(...)` と `clear()` を必ず実行する。
  - property-be では未認証API以外で tenant 未解決時に安易に default fallback しない設定を追加する。

### [High] MyBatis-Plus 既存 interceptor があると tenant interceptor が登録されない

- 根拠:
  - [TenantAutoConfiguration.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-tenant/src/main/java/com/smartdx/tenant/config/TenantAutoConfiguration.java:56) が `@ConditionalOnMissingBean(MybatisPlusInterceptor.class)` になっています。
  - コメントでも「既存がある場合は、アプリ側で TenantLineHandler を追加する必要がある」とされていますが、サービス側にその補完実装はありません。
- 影響:
  - property-be が pagination / data-permission 等で独自 `MybatisPlusInterceptor` を定義した瞬間、tenant filter が無効化されます。
  - マルチテナント分離漏れが silent failure になります。
- 推奨修正:
  - libs 側で `MybatisPlusInterceptor` への追加を確実に行う設計にする、またはサービス側に必須 config とテストを追加する。
  - `TenantLineInnerInterceptor` が登録済みであることを検証する起動テストを追加する。

### [High] DB schema / Flyway migration がない

- 根拠:
  - `apps/backend/services/property-be/src/main/resources/db/migration` 配下の migration は 0 件。
  - [00-init-databases.sql](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/platform/mysql/init/00-init-databases.sql:5) は database 作成のみで、テーブル作成や `tenant_id` 追加を行っていません。
  - [application.yml](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/services/property-be/src/main/resources/application.yml:15) は `property_db` を参照しますが、アプリ側に schema 管理がありません。
- 影響:
  - 初期環境で property API を実行できません。
  - Phase 2 要件の「Flyway migrationを移動」「tenant_id による行レベル分離」が未検証です。
- 推奨修正:
  - 既存 property DB migration を `services/property-be/src/main/resources/db/migration/` に移動する。
  - 全 tenant-aware table に `tenant_id` があることを migration とテストで保証する。

### [High] Docker healthcheck が成立しない可能性が高い

- 根拠:
  - [docker-compose.yml](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/platform/docker-compose.yml:84) と [openapi.yaml](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/services/property-be/openapi.yaml:155) は `/actuator/health` を前提にしています。
  - [property-be/pom.xml](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/services/property-be/pom.xml:46) の Spring Boot 依存には `spring-boot-starter-actuator` がありません。
- 影響:
  - `docker-compose up` で `property-be` が unhealthy になり、nginx 依存も不安定になります。
  - Phase 2 完了条件の「docker-compose で property-be 起動可能」を満たせません。
- 推奨修正:
  - `spring-boot-starter-actuator` を追加し、`management.endpoints.web.exposure.include=health,info` を明示する。
  - Security 設定で `/actuator/health` を permitAll にする。

### [Medium] Redis / OpenSearch の tenant default が設定と分離してハードコードされている

- 根拠:
  - [TenantRedisKeyGenerator.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-redis/src/main/java/com/smartdx/redis/TenantRedisKeyGenerator.java:29) は tenant 未設定時に `1L` を固定使用しています。
  - [TenantQueryBuilder.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-opensearch/src/main/java/com/smartdx/opensearch/TenantQueryBuilder.java:23) も `1L` を固定使用しています。
  - 一方、[TenantProperties.java](/Users/wangjw/Dev/Git/ross-dev2024/vps/smart-dx-backend/apps/backend/libs/smart-be-tenant/src/main/java/com/smartdx/tenant/TenantProperties.java:39) には `defaultTenantId` 設定があります。
- 影響:
  - retail の `force-default` や将来サービスごとの default tenant を変えても Redis / OpenSearch だけ `1` に流れます。
  - DB・cache・search のテナント境界が不一致になります。
- 推奨修正:
  - `TenantProperties` または共通 `TenantIdProvider` を Redis / OpenSearch libs に注入し、ハードコードを排除する。

### [Medium] Phase 1 の GitHub Actions が未作成

- 根拠:
  - 目標ディレクトリ構成では `.github/workflows/property-be-ci.yml`, `retail-be-ci.yml`, `libs-ci.yml` が示されていますが、`.github/` 配下に workflow がありません。
- 影響:
  - Maven compile / test / service build をPRで継続検証できません。
- 推奨修正:
  - 少なくとも `libs-ci.yml` と `property-be-ci.yml` を追加し、`mvn -f apps/backend/pom.xml verify` を実行する。

## Verification

- 実行: `mvn -f apps/backend/pom.xml clean verify`
- 結果: 成功
- 補足: 成功していますが、`property-be` のコンパイル対象は 1 source file のため、Phase 2 の動作保証にはなっていません。テストファイルも 0 件です。

## 次にやるべき修正順

1. `smart-property-dx2` から property-be 実装と Flyway migration を移植する。
2. `smart-be-tenant` に `TenantResolver` と HTTP filter/interceptor を追加し、未解決時の挙動をサービス別に制御する。
3. tenant interceptor 登録をテストで保証する。
4. `/actuator/health` を実際に起動するように actuator と security 設定を追加する。
5. OpenAPI を実装済み API と同期する。
6. tenant A/B の分離テスト、Redis key prefix、OpenSearch tenant filter のテストを追加する。
