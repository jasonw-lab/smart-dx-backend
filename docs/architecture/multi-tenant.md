# マルチテナント設計

## 概要
Smart DX Backend は行レベル分離型マルチテナントを採用。全サービスが共通の `libs/smart-be-tenant` を利用し、統一されたマルチテナント基盤として運用。

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                             │
│                   (Authorization: Bearer JWT)                    │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TenantContextFilter                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 1. SecurityContext → tenantId                            │    │
│  │ 2. JWT claim → tenantId                                  │    │
│  │ 3. Domain mapping → tenantId                             │    │
│  │ 4. Default tenant (force-default mode)                   │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TenantContextHolder                           │
│           TransmittableThreadLocal<Long> TENANT_ID               │
│           (supports async/thread pool propagation)               │
└─────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│    MySQL         │ │     Redis        │ │   OpenSearch     │
│ TenantLineHandler│ │  Key Prefix      │ │  Tenant Filter   │
│ WHERE tenant_id  │ │ {svc}:{tid}:*    │ │ "tenant_id": X   │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

## コンポーネント詳細

### 1. TenantContextHolder

テナントIDをThreadLocalで保持。TransmittableThreadLocalを使用し、非同期処理・スレッドプールでの値伝播をサポート。

```java
package com.smartdx.tenant;

public class TenantContextHolder {
    private static final TransmittableThreadLocal<Long> TENANT_ID_HOLDER
        = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<Boolean> IGNORE_TENANT_HOLDER
        = new TransmittableThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT_HOLDER.set(ignore);
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE_TENANT_HOLDER.get());
    }

    public static void clear() {
        TENANT_ID_HOLDER.remove();
        IGNORE_TENANT_HOLDER.remove();
    }
}
```

### 2. TenantContextFilter

HTTPリクエストからテナントIDを解決し、TenantContextHolderに設定。

```java
@Component
@Order(1)
public class TenantContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        try {
            Long tenantId = resolveTenantId(request);
            TenantContextHolder.setTenantId(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
```

### 3. TenantLineHandler (smart-be-tenant)

MyBatis-Plusのインターセプタ。SELECT/UPDATE/DELETEに自動でWHERE tenant_id = ?を追加。

```java
package com.smartdx.tenant.mybatis;

public class TenantLineHandler implements TenantLineHandler {
    private final TenantProperties tenantProperties;

    @Override
    public Expression getTenantId() {
        // force-default モードの場合、常にデフォルトテナントIDを使用
        if (tenantProperties.isForceDefault()) {
            return new LongValue(tenantProperties.getDefaultTenantId());
        }

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // デフォルトテナントIDにフォールバック
            Long defaultId = tenantProperties.getDefaultTenantId();
            if (defaultId != null) {
                return new LongValue(defaultId);
            }
            throw new IllegalStateException("TenantId is required");
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getColumn();  // default: "tenant_id"
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return TenantContextHolder.isIgnoreTenant()
            || tenantProperties.getIgnoreTables().contains(tableName);
    }
}
```

## 設定

### application.yml

```yaml
smart-be-tenant:
  # マルチテナント機能の有効/無効
  enabled: true

  # テナントIDカラム名
  column: tenant_id

  # デフォルトテナントID (未認証時)
  default-tenant: default

  # 強制デフォルトモード (retail-be用)
  # true: 全リクエストをdefault-tenantで処理
  force-default: false

  # テナントフィルタを適用しないテーブル
  ignore-tables:
    - sys_tenant
    - sys_tenant_plan
    - sys_tenant_plan_menu
    - sys_menu
    - sys_dict
    - sys_dict_item
    - sys_config
```

## サービス別設定

### property-be (複数テナント運用)
```yaml
smart-be-tenant:
  enabled: true
  force-default: false
```

### retail-be (1テナント固定運用)
```yaml
smart-be-tenant:
  enabled: true
  default-tenant: default
  force-default: true
```

## データベース設計

### テーブル設計原則
- 全テーブルに `tenant_id` カラム (VARCHAR(50) または BIGINT)
- インデックスは `(tenant_id, ...)` 複合インデックスを基本
- NOT NULL 制約を付与

```sql
CREATE TABLE properties (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    -- ...
    INDEX idx_tenant_name (tenant_id, name)
);
```

### マイグレーション (retail-be)
```sql
-- 既存テーブルにtenant_idを追加
ALTER TABLE products ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
UPDATE products SET tenant_id = 1;  -- 'default' テナントのID

-- インデックス追加
CREATE INDEX idx_products_tenant ON products(tenant_id);
```

## Redis設計

### Key形式
```
{service}:{tenant_id}:{業務key}
```

### 例
```
property:1:search:condition:user-123
property:2:cache:property:456
retail:1:cart:user-789
```

### smart-be-redis による自動prefix
```java
@Component
public class TenantAwareRedisTemplate {
    public String buildKey(String businessKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        String service = applicationName;
        return String.format("%s:%d:%s", service, tenantId, businessKey);
    }
}
```

## OpenSearch設計

### ドキュメント構造
```json
{
  "tenant_id": 1,
  "property_id": 123,
  "name": "物件名",
  "description": "..."
}
```

### 検索時の自動フィルタ
```java
@Component
public class TenantAwareSearchClient {
    public SearchResponse search(Query query) {
        Long tenantId = TenantContextHolder.getTenantId();
        Query tenantQuery = Query.of(q -> q
            .bool(b -> b
                .must(query)
                .filter(f -> f.term(t -> t
                    .field("tenant_id")
                    .value(tenantId)
                ))
            )
        );
        return client.search(tenantQuery);
    }
}
```

## セキュリティ考慮事項

### 1. テナント分離の検証
- 定期的にクロステナントアクセスがないことを検証
- テストケースで分離を確認

### 2. システム操作時の注意
```java
// システムテーブルアクセス時は一時的に無効化
TenantContextHolder.setIgnoreTenant(true);
try {
    // システムテーブル操作
} finally {
    TenantContextHolder.setIgnoreTenant(false);
}
```

### 3. 非同期処理
TransmittableThreadLocalにより自動伝播するが、外部システム連携時は明示的に伝達。

## 関連ドキュメント
- [ADR-005: マルチテナント戦略](../adr/005-multi-tenant-strategy.md)
- [ADR-000: 依存関係調査](../adr/000-dependency-audit.md)
