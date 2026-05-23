# Property Backend Service

## 概要

不動産DXドメインのバックエンドサービス。物件管理、類似物件検索、OpenSearch連携などを提供。

## 技術スタック

- Java 21
- Spring Boot 3.3.6
- MyBatis-Plus 3.5.15
- MySQL 8.0
- Redis 7
- OpenSearch 2.11

## マルチテナント

**運用方式**: 複数テナント運用

```yaml
smart-be-tenant:
  enabled: true
  column: tenant_id
  default-tenant-id: 1
  force-default: false  # 複数テナントを許可
```

## ポート

- サービスポート: 8081
- 内部ヘルスチェック: /actuator/health

## 環境変数

| 変数名 | 説明 | デフォルト |
|--------|------|-----------|
| SPRING_PROFILES_ACTIVE | プロファイル | dev |
| MYSQL_HOST | MySQLホスト | localhost |
| MYSQL_PORT | MySQLポート | 3306 |
| MYSQL_USER | MySQLユーザー | root |
| MYSQL_PASSWORD | MySQLパスワード | root |
| REDIS_HOST | Redisホスト | localhost |
| REDIS_PORT | Redisポート | 6379 |
| OPENSEARCH_HOST | OpenSearchホスト | localhost |
| OPENSEARCH_PORT | OpenSearchポート | 9200 |

## データベース

- DB名: `property_db`
- 全テーブルに `tenant_id` カラム

## 依存ライブラリ

- smart-be-core: 共通レスポンス・例外
- smart-be-security: JWT認証
- smart-be-tenant: マルチテナント機構
- smart-be-redis: テナント対応Redisキー
- smart-be-opensearch: テナント対応検索
- smart-be-observability: ログMDC

## ローカル起動

```bash
# ミドルウェア起動
cd platform
docker-compose up -d mysql redis opensearch

# アプリケーション起動
cd apps/backend
mvn -pl services/property-be spring-boot:run
```

## Docker起動

```bash
cd platform
docker-compose up -d property-be
```

## メモリ設定

JVMチューニング (目標: 150-250MB):

```
-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m
```

## API仕様

- OpenAPI: `services/property-be/openapi.yaml`
- Swagger UI: http://localhost:8081/swagger-ui.html

## ドメイン知識

不動産ドメインの詳細は smart-property-dx2 リポジトリを参照:
- https://github.com/{org}/smart-property-dx2/tree/main/docs/domain

## 関連ドキュメント

- [アーキテクチャ概要](../architecture/overview.md)
- [マルチテナント設計](../architecture/multi-tenant.md)
