# {Domain} Backend

## 概要
TODO: サービスの説明を記載

## 前提条件
- Java 21
- Maven 3.9+
- MySQL 8.0
- Redis 7

## ビルド

```bash
# リポジトリルートから
cd apps/backend
mvn -pl services/{domain}-be -am clean package
```

## 起動

### ローカル
```bash
mvn -pl services/{domain}-be spring-boot:run
```

### Docker
```bash
cd platform
docker-compose up -d {domain}-be
```

## API

- Base URL: `http://localhost:808X`
- API Docs: `http://localhost:808X/doc.html`
- Health: `http://localhost:808X/actuator/health`

## 設定

| 環境変数 | デフォルト | 説明 |
|---------|-----------|------|
| DB_HOST | localhost | MySQL ホスト |
| DB_PORT | 3306 | MySQL ポート |
| DB_NAME | {domain}_db | データベース名 |
| DB_USERNAME | root | DB ユーザー |
| DB_PASSWORD | root123 | DB パスワード |
| REDIS_HOST | localhost | Redis ホスト |
| REDIS_PORT | 6379 | Redis ポート |

## 関連ドキュメント
- [アーキテクチャ概要](../../../docs/architecture/overview.md)
- [マルチテナント設計](../../../docs/architecture/multi-tenant.md)
