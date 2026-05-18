# メモリチューニング

## 目標
- 1サービスあたり **150-250MB**
- VPS全体で複数サービスを安定稼働

## JVM設定

### 推奨設定
```bash
JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"
```

| オプション | 値 | 説明 |
|-----------|-----|------|
| -Xmx | 256m | 最大ヒープサイズ |
| -Xms | 128m | 初期ヒープサイズ |
| -XX:+UseSerialGC | - | シングルスレッドGC (メモリ効率重視) |
| -XX:MaxMetaspaceSize | 128m | メタスペース上限 |

### Dockerfile例
```dockerfile
FROM eclipse-temurin:21-jre-alpine

ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"

COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
```

## Spring Boot 軽量化

### application.yml
```yaml
spring:
  main:
    lazy-initialization: true  # 遅延初期化
  jmx:
    enabled: false  # JMX無効化
  jackson:
    serialization:
      fail-on-empty-beans: false
```

### 不要starter除去
- 使用しないstarter依存を除去
- spring-boot-devtools は本番で無効化

## docker-compose設定

```yaml
services:
  property-be:
    mem_limit: 300m
    memswap_limit: 300m
```

## 計測

### Before/After記録

| 日付 | サービス | 設定 | メモリ使用量 | 備考 |
|------|----------|------|-------------|------|
| YYYY-MM-DD | property-be | デフォルト | - | Phase 2前 |
| YYYY-MM-DD | property-be | チューニング後 | - | Phase 2後 |
| YYYY-MM-DD | retail-be | チューニング後 | - | Phase 3後 |

### 計測コマンド
```bash
./platform/scripts/memory-check.sh
```

## 将来: GraalVM Native Image

### 期待効果
- メモリ: 80-120MB
- 起動時間: 0.1秒

### 対応予定
- Spring Boot 3.x + GraalVM
- Phase 4以降で検討
