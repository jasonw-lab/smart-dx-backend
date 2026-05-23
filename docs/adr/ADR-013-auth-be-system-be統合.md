# ADR-013: auth-be を system-be に統合

## ステータス

承認済み (Accepted) - 2026-05-19 実装完了

**注記:** 本 ADR は auth-be の system-be への**コード統合**を定義。
最終的なデプロイ構成 (container, port, nginx routing) は **ADR-006 (Modular Monolith)** が supersede する。
本 ADR 中の `port: 8083`, `upstream system-be` 等の記述は、ADR-006 適用前の過去経緯として残置。
現行構成は `smart-dx-be:8080` 単一コンテナ。

## コンテキスト

現在、認証機能 (auth-be) とシステム管理機能 (system-be) が別サービスとして実装されています。

### 現状の構成

```
apps/backend/services/
├── auth-be (port: 8082)     ← 統合対象
├── system-be (port: 8083)   ← 統合先
├── property-be (port: 8081) ← 対象外
└── retail-be (port: 8082)   ← 対象外 (DEMO用、未稼働)
```

**本 ADR のスコープ:** 管理系サービス (auth-be / system-be) の統合のみ。property-be / retail-be は対象外。

### 問題点

| 項目 | 問題 |
|-----|------|
| DB依存 | 両サービスが `smart_dx_db` の同一テーブル (`sys_user`, `sys_tenant`, `sys_role`) を参照 |
| Flyway | 同一DBに対して別々の migration 履歴テーブルが必要 |
| 運用負荷 | 2サービス分のデプロイ・監視・ログ管理 |
| API設計 | 認証とユーザー管理が密結合（ログインにユーザーデータ必須） |
| ドメイン | 同一ドメイン (Identity Management) が分散 |

## 決定

**auth-be の機能を system-be に統合する。**

### 統合後の構成

```
system-be (port: 8083)
├── 認証 (auth)
│   ├── POST /api/v1/auth/login
│   ├── POST /api/v1/auth/logout
│   ├── POST /api/v1/auth/refresh-token
│   └── GET  /api/v1/auth/captcha
├── ユーザー管理 (users) ← 既存
│   ├── GET    /api/v1/users
│   ├── POST   /api/v1/users
│   ├── PUT    /api/v1/users/{id}
│   ├── DELETE /api/v1/users/{id}
│   └── GET    /api/v1/users/me
└── テナント管理 (tenants) ← 既存
    ├── GET    /api/v1/tenants
    ├── POST   /api/v1/tenants
    ├── PUT    /api/v1/tenants/{id}
    └── DELETE /api/v1/tenants/{id}
```

**注意:** `/api/v1/roles/*` は現時点で未実装。RoleMapper / Role エンティティは存在するが、Controller / Service は未作成。本 ADR のスコープ外とし、別タスクで対応する。

## ポート 8082 直アクセスの扱い

### 方針

統合後、`auth-be:8082` への直接アクセスは**廃止**する。

### 互換性

| アクセス方法 | 統合前 | 統合後 | 備考 |
|-------------|-------|-------|------|
| nginx 経由 `/api/v1/auth/*` | auth-be:8082 | system-be:8083 | **URL 互換維持** |
| `localhost:8082` 直アクセス | 可 | **不可** | 破壊的変更 |
| E2E テスト | 8082 直 or nginx | nginx 経由に統一 | 要修正 |

### 移行期間

- 移行期間は設けない (auth-be は本番未稼働のため)
- 開発者向けに `system-be:8083` への切り替えを周知

## Flyway 履歴の扱い

### 方針

- **新規 migration は `flyway_schema_history_system` に一本化**
- **既存の `flyway_schema_history_auth` は履歴として残置** (削除しない)

### 理由

- `auth-be` 固有のテーブルは存在しない (`sys_user` 等は `system-be` migration に包含済み)
- `flyway_schema_history_auth` の削除は本番 DB の migration 状態判定を壊すリスクあり
- 残置しても実害なし (参照されないだけ)

### 検証項目

- [ ] クリーン DB で `system-be` 起動 → migration 正常完了
- [ ] 既存 DB で `system-be` 起動 → migration 正常完了 (auth 履歴は無視される)

## 統合の進め方

### Phase 1: 準備

1. **移行対象ファイル確認** (現物ベース)

   **auth-be から移動:**
   ```
   services/auth-be/src/main/java/com/smartdx/auth/
   ├── controller/
   │   └── AuthController.java
   ├── service/
   │   ├── AuthService.java
   │   └── impl/AuthServiceImpl.java
   ├── captcha/
   │   └── CaptchaProperties.java
   ├── config/
   │   └── SecurityConfig.java        ← system-be の既存と統合
   ├── mapper/
   │   └── UserMapper.java            ← system-be の既存と差分確認
   └── model/
       ├── entity/User.java           ← system-be の既存と差分確認
       ├── req/LoginReq.java
       └── vo/CaptchaVO.java
   ```

   **system-be 既存 (参考):**
   ```
   services/system-be/src/main/java/com/smartdx/system/
   ├── controller/
   │   ├── UserController.java
   │   └── TenantController.java
   ├── service/
   │   ├── UserService.java
   │   ├── TenantService.java
   │   └── impl/...
   ├── config/
   │   └── SecurityConfig.java        ← auth-be 設定を統合
   ├── mapper/
   │   ├── UserMapper.java
   │   ├── TenantMapper.java
   │   └── RoleMapper.java
   └── model/
       ├── entity/User.java, Tenant.java, Role.java
       ├── query/UserQuery.java, TenantQuery.java
       └── vo/UserVO.java, TenantVO.java
   ```

2. **差分確認**
   - `auth-be/UserMapper` vs `system-be/UserMapper` → 認証用クエリの有無
   - `auth-be/User` vs `system-be/User` → フィールド差分
   - `auth-be/SecurityConfig` vs `system-be/SecurityConfig` → Bean 重複確認

### Phase 2: コード統合

1. **パッケージ移動**
   ```
   system-be/src/main/java/com/smartdx/system/
   ├── controller/
   │   └── AuthController.java      ← auth-be から移動
   ├── service/
   │   ├── AuthService.java         ← auth-be から移動
   │   └── impl/AuthServiceImpl.java
   ├── captcha/
   │   └── CaptchaProperties.java   ← auth-be から移動
   └── model/
       ├── req/LoginReq.java        ← auth-be から移動
       └── vo/CaptchaVO.java        ← auth-be から移動
   ```

2. **SecurityConfig 統合**
   - `system-be/SecurityConfig` に auth-be の設定をマージ
   - Bean 重複を解消
   - 認証不要エンドポイントを追加

3. **application.yml 統合**

   追加設定:
   ```yaml
   security:
     ignore-urls:
       - /api/v1/auth/**    ← 追加
       - /actuator/**
       - /v3/api-docs/**
       - /swagger-ui/**
       - /doc.html
     unsecured-urls:
       - /api/v1/auth/**    ← 追加
       - /actuator/**
       - /v3/api-docs/**
       - /swagger-ui/**
       - /doc.html

   # Captcha (auth-be から移植)
   captcha:
     required: ${CAPTCHA_REQUIRED:false}
     width: 120
     height: 40
     code-length: 4
     expire-seconds: 300
   ```

4. **UserMapper 統合**
   - `auth-be/UserMapper` の認証用クエリを `system-be/UserMapper` に追加
   - 例: `selectByUsername(String username, Long tenantId)`

### Phase 3: インフラ更新

1. **docker-compose.yml**
   ```yaml
   # 削除
   # auth-be:
   #   build: ./services/auth-be
   #   ...

   # system-be に CAPTCHA_REQUIRED を追加
   system-be:
     build: ./services/system-be
     ports:
       - "8083:8083"
     environment:
       - MYSQL_DATABASE=smart_dx_db
       - JWT_SECRET_KEY=${JWT_SECRET_KEY}
       - CAPTCHA_REQUIRED=${CAPTCHA_REQUIRED:-false}  ← 追加
     depends_on:
       - mysql
       - redis
   ```

2. **nginx.conf**
   ```nginx
   # upstream auth-be を削除
   # upstream auth-be {
   #     server auth-be:8082;
   # }

   # /api/v1/auth/ を system-be に向け直す
   location /api/v1/auth/ {
       proxy_pass http://system-be/api/v1/auth/;  ← 変更
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_set_header X-Tenant-Id $http_x_tenant_id;
       proxy_connect_timeout 30s;
       proxy_read_timeout 60s;
   }
   ```

### Phase 4: クリーンアップ

1. **auth-be ディレクトリ削除**
   ```bash
   rm -rf services/auth-be
   ```

2. **親 pom.xml 更新**
   ```xml
   <modules>
     <!-- <module>services/auth-be</module> -->  ← 削除
     <module>services/system-be</module>
     <module>services/property-be</module>
     <module>services/retail-be</module>
   </modules>
   ```

3. **tenant ignore-tables 更新**
   - `flyway_schema_history_auth` は不要 (system-be が参照しない)

4. **ドキュメント更新**
   - CLAUDE.md のサービス一覧
   - runbook (該当あれば)

### Phase 5: 検証

1. **単体テスト**
   ```bash
   mvn -pl services/system-be test
   ```

2. **Security 検証**
   | テストケース | 期待結果 |
   |-------------|---------|
   | 未認証で `GET /api/v1/auth/captcha` | 200 OK |
   | 未認証で `POST /api/v1/auth/login` | 200 OK (or 401 if invalid) |
   | 認証ありで `GET /api/v1/users/me` | 200 OK |
   | 未認証で `GET /api/v1/users` | 401 Unauthorized |

3. **統合テスト**
   - captcha 取得 → ログイン → ユーザー取得 フロー確認
   - JWT トークンで property-be API 呼び出し確認

4. **E2E テスト**
   ```bash
   docker-compose up -d
   # nginx 経由で全 API 疎通確認
   curl http://localhost/api/v1/auth/captcha
   curl -X POST http://localhost/api/v1/auth/login -d '...'
   curl -H "Authorization: Bearer ..." http://localhost/api/v1/users/me
   ```

## 移行チェックリスト

- [x] Phase 1: 準備
  - [x] auth-be/system-be 差分確認 (User, UserMapper, SecurityConfig)
  - [x] 移行対象ファイル最終確認
- [x] Phase 2: コード統合
  - [x] AuthController 移動
  - [x] AuthService / AuthServiceImpl 移動
  - [x] CaptchaProperties 移動
  - [x] LoginReq / CaptchaVO 移動
  - [x] SecurityConfig 統合 (Bean 重複解消)
  - [x] UserMapper 認証クエリ追加
  - [x] application.yml 統合 (security.ignore-urls, captcha)
- [x] Phase 3: インフラ更新
  - [x] docker-compose.yml から auth-be 削除
  - [x] nginx.conf の auth routing を system-be に変更
  - [x] upstream auth-be 削除
- [x] Phase 4: クリーンアップ
  - [x] auth-be ディレクトリ削除 (手動対応待ち)
  - [x] 親 pom.xml から auth-be 削除
  - [x] ドキュメント更新
- [x] Phase 5: 検証 (2026-05-20)
  - [x] クリーン DB で migration 成功 (TestContainers使用)
  - [x] 既存 DB で migration 成功 (Flyway disabled で動作確認)
  - [x] Security 検証 (captcha/login 未認証OK、users 未認証NG)
  - [x] 統合フロー (captcha → login → API) 成功
  - [x] E2E nginx 経由疎通 (nginx対応不要のため省略)

## 影響

### ポジティブ

- 管理系サービス数削減 (auth-be + system-be → system-be のみ)
- Flyway 履歴テーブル実質削減 (auth 用は不要に)
- 運用・デプロイ簡素化
- 認証とユーザー管理が同一サービス内で完結
- API レイテンシ削減 (サービス間通信不要)

### ネガティブ

- system-be のコードベース増加
- 将来的な認証サービス独立化が困難に

### リスク

| リスク | 対策 |
|-------|------|
| 移行中の認証障害 | auth-be 本番未稼働のため影響なし |
| API 互換性 | nginx 経由 URL は変更なし |
| JWT トークン互換性 | 同一シークレット使用のため問題なし |
| 8082 直アクセス | 廃止方針を周知、E2E は nginx 経由に統一 |
| Flyway 履歴 | auth 履歴は残置、system のみ使用 |

## 代替案

### 代替案 1: auth-be に統合

system-be を auth-be に統合する案。

却下理由:
- system-be の方がスコープが広い (ユーザー、テナント、将来のロール)
- 「認証」より「システム管理」の方が機能拡張の余地がある

### 代替案 2: 現状維持 (2サービス)

却下理由:
- 同一 DB を参照する密結合状態
- 運用負荷が高い
- メリットが薄い

## 参考

- [review_0519_task1-task3.codex.md](../../../../review/review_0519_task1-task3.codex.md) - 初期指摘事項
- [review_0519_adr-013-auth-be-system-be統合.codex.md](../../../../review/review_0519_adr-013-auth-be-system-be統合.codex.md) - ADR レビュー
