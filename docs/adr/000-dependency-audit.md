# ADR-000: 依存関係調査

## ステータス
承認済み

## 日付
2025-05-17

## コンテキスト
smart-dx-backendを構築するにあたり、既存の2つのバックエンドプロジェクトの依存関係を調査し、統合時のバージョン衝突を事前に把握する必要がある。

## 調査対象
- smart-property-dx2/apps/backend (マルチテナント対応済み)
- smart-retail-dx/apps/backend (マルチテナント未対応)

## 依存関係バージョン比較

| ライブラリ | smart-property-dx2 | smart-retail-dx | 統合版 (smart-dx-backend) |
|-----------|-------------------|-----------------|--------------------------|
| Spring Boot | 4.0.5 | 3.3.6 | 3.3.6 |
| Spring Framework | 7.0.6 | 6.1.15 | 6.1.15 |
| MyBatis-Plus | 3.5.15 | 3.5.5 | 3.5.15 |
| MySQL Connector J | 9.1.0 | 9.1.0 | 9.1.0 |
| Druid | 1.2.24 | 1.2.24 | 1.2.24 |
| Lombok | 1.18.44 | 1.18.36 | 1.18.36 |
| Hutool | 5.8.41 | 5.8.34 | 5.8.41 |
| MapStruct | 1.6.3 | 1.6.3 | 1.6.3 |
| Redisson | 4.1.0 | 3.40.2 | 3.40.2 |
| Knife4j | 4.5.0 | 4.5.0 | 4.5.0 |
| XXL-Job | 3.2.0 | 2.4.2 | - |
| TransmittableThreadLocal | 2.14.5 | - | 2.14.5 |

## smart-property-dx2 マルチテナント実装方式調査

### 使用ライブラリ
- **MyBatis-Plus TenantLineHandler**: SQL自動フィルタリング
- **TransmittableThreadLocal**: 非同期/スレッドプール環境でのテナントID伝播
- **Spring Security**: 認証・認可
- **Hutool JWT**: トークン生成・検証

### 主要コンポーネント

#### 1. TenantContextHolder (ThreadLocal管理)
```java
// com.youlai.boot.framework.tenant.TenantContextHolder
- TransmittableThreadLocal<Long> TENANT_ID_HOLDER
- TransmittableThreadLocal<Boolean> IGNORE_TENANT_HOLDER
- setTenantId(Long tenantId)
- getTenantId()
- setIgnoreTenant(boolean ignore)
- isIgnoreTenant()
- clear()
```

#### 2. TenantContextFilter (テナントID解決)
```java
// com.youlai.boot.framework.web.filter.TenantContextFilter
優先順位:
1. SecurityContext → SysUserDetails.tenantId
2. JWT Authorization Header → tenantId claim
3. ドメインマッピング → TenantService.getTenantIdByDomain()
4. デバッグモード → DEFAULT_TENANT_ID
```

#### 3. MyTenantLineHandler (SQL自動フィルタリング)
```java
// com.youlai.boot.framework.mybatis.interceptor.MyTenantLineHandler
implements TenantLineHandler
- getTenantId(): テナントID取得
- getTenantIdColumn(): "tenant_id"
- ignoreTable(tableName): 除外テーブル判定
```

#### 4. MybatisConfig (インターセプタ登録)
```java
// インターセプタ実行順序
1. TenantLineInnerInterceptor (最優先)
2. DataPermissionInterceptor
3. PaginationInnerInterceptor
```

### 設定ファイル構造
```yaml
tenant:
  column: tenant_id
  ignore-tables:
    - sys_tenant
    - sys_tenant_plan
    - sys_tenant_plan_menu
    - sys_menu
    - sys_dict
    - sys_dict_item
    - sys_config
    - gen_table
    - gen_table_column
    - sys_user
    - sys_user_role
    - sys_test_config
```

### JWT Payload Claims
```java
TENANT_ID = "tenantId"
USER_ID = "userId"
DEPT_ID = "deptId"
CAN_SWITCH_TENANT = "canSwitchTenant"
DATA_SCOPES = "dataScopes"
AUTHORITIES = "authorities"
TOKEN_VERSION = "tokenVersion"
```

### テナント関連テーブル
| テーブル | 説明 |
|---------|------|
| sys_tenant | テナント基本情報 (id, name, code, domain, plan_id, status, expire_time) |
| sys_tenant_plan | テナントプラン |
| sys_tenant_plan_menu | プランメニュー設定 |
| sys_tenant_menu | テナントメニュー設定 |

### データ権限制御
- `@DataPermission` アノテーションで対象メソッド指定
- データスコープ: ALL / DEPT_AND_SUB / DEPT / SELF / CUSTOM

## 決定事項

### Spring Boot バージョン
- **Spring Boot 3.3.6** を採用
- 理由: smart-property-dx2の4.0.5はプレリリース版、retail-dxの3.3.6が安定版

### MyBatis-Plus バージョン
- **3.5.15** を採用
- 理由: 最新の安定版、TenantLineHandler実装が改善

### マルチテナント実装
- smart-property-dx2の実装をベースに `libs/smart-be-tenant` として抽出
- 詳細は ADR-005 参照

## 影響
- parent pomで全バージョンを一元管理
- 個別サービスでのバージョン指定を禁止
- 統合時に互換性問題が発生した場合は本ADRを更新
