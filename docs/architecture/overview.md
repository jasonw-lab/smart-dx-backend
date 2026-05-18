# アーキテクチャ概要

## 全体構成

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Nginx (Reverse Proxy)                      │
│                              localhost:80                            │
└─────────────────────────────────────────────────────────────────────┘
                    │                           │
                    ▼                           ▼
┌─────────────────────────────┐   ┌─────────────────────────────┐
│       property-be           │   │        retail-be            │
│       (Port 8081)           │   │       (Port 8082)           │
│   ┌─────────────────────┐   │   │   ┌─────────────────────┐   │
│   │ Spring Boot 3.3.6   │   │   │   │ Spring Boot 3.3.6   │   │
│   │ Java 21             │   │   │   │ Java 21             │   │
│   │ MyBatis-Plus        │   │   │   │ MyBatis-Plus        │   │
│   └─────────────────────┘   │   │   └─────────────────────┘   │
│           │                 │   │           │                 │
│           ▼                 │   │           ▼                 │
│   ┌─────────────────────┐   │   │   ┌─────────────────────┐   │
│   │      libs/*         │   │   │   │      libs/*         │   │
│   │  - smart-be-core    │   │   │   │  - smart-be-core    │   │
│   │  - smart-be-tenant  │   │   │   │  - smart-be-tenant  │   │
│   │  - smart-be-redis   │   │   │   │  - smart-be-redis   │   │
│   │  - ...              │   │   │   │  - ...              │   │
│   └─────────────────────┘   │   │   └─────────────────────┘   │
└─────────────────────────────┘   └─────────────────────────────┘
         │          │                    │          │
         ▼          ▼                    ▼          ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│    MySQL     │ │    Redis     │ │  OpenSearch  │
│  property_db │ │   (Shared)   │ │   (Shared)   │
│  retail_db   │ │              │ │              │
│  (Port 3306) │ │ (Port 6379)  │ │ (Port 9200)  │
└──────────────┘ └──────────────┘ └──────────────┘
```

## 設計原則

### 1. サービス独立性
- 各BEサービスは独立したSpring Bootアプリケーション
- 独立ビルド・独立デプロイ
- サービス間の直接Javaクラス参照は禁止

### 2. 共通ライブラリ共有
- `libs/` 配下の共通ライブラリをjar依存で共有
- 全サービスで同一バージョンを使用
- 業務ロジックはlibsに入れない

### 3. マルチテナント統一
- 全サービスが `smart-be-tenant` を利用
- 行レベル分離型 (tenant_id カラム)
- 運用形態はサービスごとに選択可能

### 4. リソース最適化
- 1サービス 150-250MB メモリ目標
- JVMチューニング適用
- 将来的にGraalVM native image対応

## 依存関係

```
{service}-be
    │
    ├─→ smart-be-core
    │       │
    ├─→ smart-be-security ─→ smart-be-core
    │       │
    ├─→ smart-be-tenant ─→ smart-be-core, smart-be-security
    │       │
    ├─→ smart-be-redis ─→ smart-be-core, smart-be-tenant
    │
    ├─→ smart-be-opensearch ─→ smart-be-core, smart-be-tenant
    │
    └─→ smart-be-observability ─→ smart-be-core, smart-be-tenant
```

## データフロー

### リクエスト処理
1. Nginx がリクエストを受信、パスに基づいてルーティング
2. サービスの `TenantContextFilter` がテナントID解決
3. `TenantContextHolder` にテナントID設定
4. ビジネスロジック実行
5. MyBatis-Plus が自動で `WHERE tenant_id = ?` 付与
6. レスポンス返却
7. `TenantContextHolder.clear()` でクリーンアップ

### サービス間通信
- 同期: REST API (tenant_id をHeader伝搬)
- 非同期: Redis Streams (将来対応)

## 技術スタック

| 領域 | 技術 |
|------|------|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 3.3.6 |
| ORM | MyBatis-Plus 3.5.15 |
| DB | MySQL 8.0 |
| キャッシュ | Redis 7 |
| 検索 | OpenSearch 2.11 |
| ビルド | Maven |
| コンテナ | Docker / Docker Compose |

## 関連ドキュメント
- [マルチテナント設計](multi-tenant.md)
- [サービス間通信](service-communication.md)
- [共通ライブラリ](shared-libs.md)
- [デプロイメント](deployment.md)
