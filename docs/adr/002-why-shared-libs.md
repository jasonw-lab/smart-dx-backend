# ADR-002: 共通ライブラリの設計

## ステータス
承認済み

## 日付
2025-05-17

## コンテキスト
複数のBEサービス (property-be, retail-be, 将来サービス) 間で共通機能を共有する方法を決定する必要がある。

## 決定
**6つの共通ライブラリ (libs/)** を作成し、全サービスがjar依存として利用する。

## ライブラリ構成

| ライブラリ | 責務 | 入れるもの | 入れないもの |
|-----------|------|-----------|-------------|
| smart-be-core | 基盤ユーティリティ | ApiResponse, BusinessException, ErrorCode, PageResponse, 日時/JSON util | 業務ロジック、ドメインモデル |
| smart-be-security | 認証・認可 | JWT生成/検証, @CurrentUser, SecurityFilterChain base | ユーザーentity、テナント解決 |
| smart-be-tenant | マルチテナント | TenantContext, TenantResolver, TenantInterceptor, MyBatis-Plus TenantLineHandler | 業務ロジック、固有テナントID |
| smart-be-redis | Redis操作 | RedisTemplate設定, key prefix強制機構 | 業務用key定義 |
| smart-be-opensearch | 検索基盤 | client bean, retry policy, tenant-aware query builder | index名、mapping |
| smart-be-observability | 可観測性 | Micrometer設定, log MDC (tenant_id含む), tracing | アプリ固有metric |

## 設計原則

### 1. 単一責任
各libsは明確な責務を持ち、責務外の機能は別libsに分離。

### 2. 業務ロジック排除
libsには業務ロジック・特定ドメインのDTOを入れない。

### 3. 依存方向
```
{service}-be → libs/*  (OK)
libs/* → {service}-be  (NG)
libs/A → libs/B        (OK: 明示的な依存のみ)
```

### 4. バージョン管理
- semverで版管理 (例: 1.0.0, 1.1.0)
- SNAPSHOTは使用しない
- 破壊的変更は1リリースのdeprecate期間を設ける

## 依存関係

```
smart-be-core (基盤)
    ↑
smart-be-security (core に依存)
    ↑
smart-be-tenant (core, security に依存)
    ↑
smart-be-redis (core, tenant に依存)
smart-be-opensearch (core, tenant に依存)
smart-be-observability (core, tenant に依存)
```

## 禁止事項
- 「common」というディレクトリ・パッケージは作らない (粒度が崩れる原因)
- libsに特定ドメインのエンティティを置かない
- libsから外部サービス固有の設定を直接参照しない

## 影響
- 新サービス追加時は libs への依存追加のみで共通機能を利用可能
- libs の変更は全サービスに影響するため、慎重なレビューが必要
