# 新サービス追加ガイド

## 概要
新しいBEサービスを smart-dx-backend に追加する手順。

## 前提条件
- `apps/backend/services/_template/` を雛形として使用
- 命名規則: `{domain}-be` (例: `logistics-be`)
- パッケージ: `com.smartdx.{domain}`

## 手順

### 1. ディレクトリ作成

```bash
cp -r apps/backend/services/_template apps/backend/services/{domain}-be
```

### 2. pom.xml 編集

```xml
<parent>
    <groupId>com.smartdx</groupId>
    <artifactId>smart-dx-backend</artifactId>
    <version>1.0.0</version>
    <relativePath>../../pom.xml</relativePath>
</parent>

<artifactId>{domain}-be</artifactId>
<name>{Domain} Backend</name>

<dependencies>
    <!-- libs -->
    <dependency>
        <groupId>com.smartdx</groupId>
        <artifactId>smart-be-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.smartdx</groupId>
        <artifactId>smart-be-tenant</artifactId>
    </dependency>
    <!-- 必要に応じて追加 -->
</dependencies>
```

### 3. parent pom に module 追加

`apps/backend/pom.xml`:
```xml
<modules>
    <!-- 既存 -->
    <module>services/{domain}-be</module>
</modules>
```

### 4. パッケージ構成

```
src/main/java/com/smartdx/{domain}/
├── {Domain}Application.java
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
└── worker/
```

### 5. application.yml 設定

```yaml
spring:
  application:
    name: {domain}-be
  datasource:
    url: jdbc:mysql://localhost:3306/{domain}_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

smart-be-tenant:
  enabled: true
  column: tenant_id
  default-tenant: default
  force-default: false  # 複数テナント運用: false, 固定運用: true

server:
  port: 808X  # 空きポート
```

### 6. データベース作成

`platform/mysql/init/` に追加:
```sql
CREATE DATABASE IF NOT EXISTS {domain}_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 7. docker-compose 追加

`platform/docker-compose.yml`:
```yaml
{domain}-be:
  build:
    context: ../apps/backend/services/{domain}-be
    dockerfile: Dockerfile
  container_name: smart-dx-{domain}-be
  ports:
    - "808X:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - JAVA_OPTS=-Xmx256m -Xms128m -XX:+UseSerialGC
  depends_on:
    mysql:
      condition: service_healthy
    redis:
      condition: service_healthy
  mem_limit: 300m
```

### 8. nginx 追加

`platform/nginx/nginx.conf`:
```nginx
location /api/{domain}/ {
    proxy_pass http://{domain}-be/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

### 9. CI/CD 追加

`.github/workflows/{domain}-be-ci.yml`:
```yaml
name: {Domain} BE CI

on:
  push:
    paths:
      - 'apps/backend/services/{domain}-be/**'
      - 'apps/backend/libs/**'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: mvn -f apps/backend/pom.xml -pl services/{domain}-be -am verify
```

### 10. ドキュメント追加

- `docs/services/{domain}-be.md`: サービス説明
- `services/{domain}-be/openapi.yaml`: API仕様
- `services/{domain}-be/README.md`: サービスREADME

## チェックリスト

- [ ] ディレクトリ作成
- [ ] pom.xml 設定
- [ ] parent pom に module 追加
- [ ] Application.java 作成
- [ ] application.yml 設定
- [ ] Dockerfile 作成
- [ ] データベース初期化SQL追加
- [ ] docker-compose 追加
- [ ] nginx routing 追加
- [ ] CI/CD workflow 追加
- [ ] ドキュメント作成
- [ ] ビルド確認
- [ ] 起動確認
- [ ] マルチテナント動作確認
