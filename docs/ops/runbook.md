# 運用手順書 (Runbook)

## サービス起動

### 全サービス起動
```bash
cd platform
./scripts/start-all.sh
```

### 個別起動
```bash
cd platform
docker-compose up -d mysql redis opensearch
docker-compose up -d property-be  # Phase 2以降
docker-compose up -d retail-be    # Phase 3以降
docker-compose up -d nginx
```

## サービス停止

### 全サービス停止
```bash
cd platform
docker-compose down
```

### 個別停止
```bash
docker-compose stop {service-name}
```

## ログ確認

### 全サービスログ
```bash
docker-compose logs -f
```

### 個別ログ
```bash
docker-compose logs -f property-be
docker-compose logs -f retail-be
```

## ヘルスチェック

### 手動確認
```bash
# Nginx
curl http://localhost/health

# Property BE (Phase 2以降)
curl http://localhost:8081/actuator/health

# Retail BE (Phase 3以降)
curl http://localhost:8082/actuator/health

# MySQL
docker-compose exec mysql mysqladmin ping -h localhost

# Redis
docker-compose exec redis redis-cli ping
```

## メモリ確認

```bash
./platform/scripts/memory-check.sh
```

## トラブルシューティング

### MySQL接続エラー
```bash
# コンテナ状態確認
docker-compose ps mysql

# ログ確認
docker-compose logs mysql

# 再起動
docker-compose restart mysql
```

### Redis接続エラー
```bash
docker-compose logs redis
docker-compose restart redis
```

### サービス起動失敗
```bash
# ログで原因確認
docker-compose logs {service-name}

# 依存サービスの状態確認
docker-compose ps

# 設定確認
cat apps/backend/services/{service-name}/src/main/resources/application.yml
```

### OOM (Out of Memory)
```bash
# メモリ使用量確認
docker stats --no-stream

# JVM設定確認
docker-compose exec {service-name} ps aux | grep java

# mem_limit 調整
# docker-compose.yml の mem_limit を増加
```

## ロールバック

### Phase 4以降のロールバック手順

1. 切り戻し用イメージでの再起動
```bash
cd platform
docker-compose -f docker-compose.rollback.yml up -d
```

2. 確認
```bash
curl http://localhost/health
docker-compose ps
```

3. 問題解決後、通常構成に戻す
```bash
docker-compose -f docker-compose.rollback.yml down
docker-compose up -d
```

## 定期メンテナンス

### ログローテーション
```bash
# Docker のログ設定で自動ローテーション
# daemon.json で設定済みの想定
```

### ディスク使用量確認
```bash
docker system df
```

### 不要イメージ削除
```bash
docker image prune -f
```

## 連絡先

- インフラ担当: infra-agent
- BE担当: backend-service-agent
