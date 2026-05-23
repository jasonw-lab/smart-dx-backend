# ADR-006: VPS デプロイ構成 (Modular Monolith)

## ステータス
採用 (2026-05-19 実装完了)

## 日付
2026-05-19

## コンテキスト

現状、`property-be` / `auth-be` / `system-be` (将来 `retail-be` 追加予定) は
それぞれ独立した Spring Boot アプリケーションとして実装され、別コンテナ・別ポート
(8081 / 8082 / 8083) で稼働している。

```
[現状] 1 service = 1 JVM = 1 container = 1 external port
  property-be : container 8081 / host 8081
  auth-be     : container 8082 / host 8082
  system-be   : container 8083 / host 8083
  retail-be   : (Phase3, container 8082計画)
```

### 課題

1. **VPS リソース効率**
   - 1 JVM あたり Spring Boot のオーバーヘッド (base ~100MB) が常時複数発生
   - 3 サービスで合計 768MB 以上のメモリを占有 (`mem_limit` 256m × 3)
   - VPS の限られたメモリ予算 (本番想定 2GB) を圧迫

2. **外部公開の煩雑さ**
   - 複数ポート公開はファイアウォール / TLS 終端 / CORS 設定が複雑
   - クライアントから見たエントリポイントが分散

3. **新ドメイン追加コスト**
   - 新 domain ごとに Dockerfile / docker-compose entry / port 割当 / healthcheck を増設

### 要件

- 外部公開ポートは **8080 一本**に統一
- VPS 上は **1 コンテナ** で全 BE ドメインを提供
- ソース構造は **ドメイン独立 module** を維持 (将来の microservice 分離を阻害しない)
- 新ドメイン追加は **module 追加 + 1 行依存追加** で完結

## 決定

**Modular Monolith デプロイ構成**を採用する。

- ソース: 既存の maven multi-module を維持 (各 `services/*-be` は独立)
- ビルド: 新規 `app/` (bootstrap) module が全 domain module を依存として取り込み単一 fat jar 化
- 実行: 1 コンテナ / 1 JVM / port 8080

### 各 domain module の役割変更

| 項目 | 現状 | 変更後 |
|---|---|---|
| `@SpringBootApplication` | 各 module に存在 | **bootstrap module のみ** |
| `spring-boot-maven-plugin` (repackage) | 各 module で実行 | bootstrap module のみ |
| Bean 公開 | アプリ起動時 ComponentScan | `@AutoConfiguration` + `AutoConfiguration.imports` |
| Controller `@RequestMapping` | domain 内任意 | `/api/<domain>/...` prefix 必須 |
| `application.yml` | フル設定 | fragment のみ (`application-<domain>.yml`) |

### 新規 bootstrap module 構成

```
apps/backend/
├── libs/                          # 既存共通lib (変更なし)
├── services/
│   ├── auth-be/                   # 独立module (library化)
│   ├── system-be/
│   ├── property-be/
│   ├── retail-be/
│   └── <new-domain>-be/           # 将来追加もここに足すだけ
└── app/                           # ★新規: 統合bootstrap module
    ├── pom.xml                    #   各 *-be を dependency に列挙
    ├── Dockerfile                 #   1つだけ
    └── src/main/
        ├── java/com/smartdx/SmartDxApplication.java
        └── resources/application.yml
```

```java
// app/src/main/java/com/smartdx/SmartDxApplication.java
@SpringBootApplication(scanBasePackages = "com.smartdx")
public class SmartDxApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartDxApplication.class, args);
    }
}
```

```xml
<!-- app/pom.xml dependencies -->
<dependency><groupId>com.smartdx</groupId><artifactId>auth-be</artifactId></dependency>
<dependency><groupId>com.smartdx</groupId><artifactId>system-be</artifactId></dependency>
<dependency><groupId>com.smartdx</groupId><artifactId>property-be</artifactId></dependency>
<dependency><groupId>com.smartdx</groupId><artifactId>retail-be</artifactId></dependency>
```

### docker-compose.yml 変更

```yaml
services:
  smart-dx-be:                     # 3 service entry を 1 つに集約
    build: ../apps/backend/app
    container_name: smart-dx-be
    ports:
      - "8080:8080"                # 外部port 8080 一本
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
    mem_limit: 768m
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
```

## 理由

### メリット

| 観点 | 効果 |
|---|---|
| メモリ | 3 JVM (256m × 3 = 768m) → 1 JVM (512m目安) で約 30〜40% 削減 |
| 外部公開 | 8080 一本に統一、FW / TLS / CORS 設定単純化 |
| 起動 | 1 コンテナのみ、起動時間も短縮 |
| 運用 | デプロイ・ログ集約・監視がシンプル |
| 開発 | source は独立 module のまま、新 domain 追加は「module 追加 + `app/pom.xml` に 1 行」 |
| 将来分離 | domain 間直接呼び出しを禁止しておけば、microservice 分割は可能 |

### 採用しなかった案

#### 案 B: nginx 集約 (各 domain は独立コンテナ維持)

各 service は内部 8080、外部は nginx 1 台が 8080 で path-based ルーティング。

- 利点: domain 完全独立 (障害分離・独立スケール可能)
- 欠点: **メモリ削減効果なし** (JVM 数は変わらない) — VPS リソース要件を満たせない

→ VPS 1 台運用前提のため**不採用**。将来 k8s 等へ移行する場合に再評価する。

#### 案 C: Spring Cloud Gateway

API Gateway service を別途立てる構成。

- 欠点: さらに JVM 1 個増えメモリ負荷悪化
- VPS 規模では over-engineering

→ **不採用**。

## 影響

### 既存コードへの変更

1. 各 `services/*-be` から `@SpringBootApplication` クラス削除
2. 各 `services/*-be` の `pom.xml` から `spring-boot-maven-plugin` の `repackage` 設定削除
3. 各 `services/*-be` の Controller `@RequestMapping` に domain prefix 付与 (`/api/property`, `/api/auth`, `/api/system`, `/api/retail`)
4. 各 `services/*-be` を `@AutoConfiguration` 化 (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`)
5. 各 `services/*-be/Dockerfile` 削除 (bootstrap 側に集約)
6. `platform/docker-compose.yml` から 3 service entry 削除、`smart-dx-be` 1 entry に置換

### 運用ルール (新規)

1. **package 隔離厳守**: `com.smartdx.<domain>.*` を厳守、cross-domain の直接 import 禁止
   - ArchUnit による静的検査を CI に組込推奨
2. **DB 分離**: 単一 MySQL でも schema 分離 or table prefix (`prop_`, `sys_`, `rtl_`) 必須
3. **domain 間連携**: 直接 Service 呼び出しではなく **Spring Event** または明示 **Facade interface** 経由 (将来分離容易性のため)
4. **設定分離**: 各 module は `application-<domain>.yml` を `resources/` 配下に持ち、bootstrap 側で `spring.config.import` で読込
5. **healthcheck**: `/actuator/health` に各 domain の `HealthIndicator` を集約

### CLAUDE.md / docs 更新

- `CLAUDE.md`: 「各BEサービスは独立Spring Boot」記述を「ソース独立 / デプロイ統合」に修正
- `docs/new-service-guide.md`: 新 domain 追加手順を bootstrap module 依存追加方式に更新
- `docs/architecture/overview.md`: デプロイ構成図を更新

### 既存 ADR との関係

- ADR-001 (Monorepo): 影響なし (本リポ構造は維持)
- ADR-002 (Shared libs): 影響なし
- ADR-005 (Multi-tenant): 影響なし (TenantContextHolder は引き続き有効)

## 移行ステップ

1. **Phase 1**: `app/` bootstrap module 雛形作成 (空の main + pom)
2. **Phase 2**: 各 `services/*-be` の main クラス削除 + `@AutoConfiguration` 化
3. **Phase 3**: Controller `@RequestMapping` に domain prefix 付与 + E2E 動作確認
4. **Phase 4**: `Dockerfile` 統合 + `docker-compose.yml` 置換
5. **Phase 5**: 旧 Dockerfile / 個別 service entry 削除、ドキュメント更新

## 未決事項 (Open Questions)

1. JWT secret / DB datasource を全 domain 共通化するか、domain 別に保持するか
2. `application.yml` の merge 戦略 (`spring.config.import` vs profile 分離)
3. domain 別 logger appender 分離の要否
4. ArchUnit 導入タイミング (本 ADR 適用と同時 / 後続)
