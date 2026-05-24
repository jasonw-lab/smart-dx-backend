# Smart-Retail Backend 移植計画

## 概要

smart-retail/backend の **modules 配下のみ** を smart-dx-backend/retail-be に統合する。

**移植対象**: `com.youlai.boot.modules.*` → `com.smartdx.retail.*`

**対象外**: system, shared, common, core, config（これらは既に smart-dx-backend の libs/ や system-be で実現済み）

---

## 移植対象モジュール

```
smart-retail/apps/backend/src/main/java/com/youlai/boot/modules/
├── retail/    # メイン業務（部分移植済み）
├── member/    # 会員管理（未移植）
└── order/     # 注文管理（未移植）
```

---

## 現状比較

### retail モジュール

| カテゴリ | 移植元 (smart-retail) | 移植先 (retail-be) | 状態 |
|---------|----------------------|-------------------|------|
| Controller | 11個 | 9個 | **2個不足** |
| Service | 13個 | 8個 | **5個不足** |
| Entity | 12個 | 9個 | **3個不足** |
| Mapper | 13個 | 9個 | **4個不足** |
| Converter | 6個 | 1個 | **5個不足** |
| Form | 14個 | 1個 | **13個不足** |
| Query | 7個 | 1個 | **6個不足** |
| VO | 16個 | 1個 | **15個不足** |
| Enum | 1個 | 0個 | **1個不足** |
| Scheduler | 1個 | 0個 | **1個不足** |
| Job | 1個 | 0個 | **1個不足** |

#### 不足Controller
- InventoryTransactionController
- PaymentController

#### 不足Service
- InventoryTransactionService
- PaymentService
- PaymentDemoService
- HeartbeatService
- DeviceMonitorService

#### 不足Entity
- InventoryHistory
- InventoryIn
- InventoryOut

#### 不足Mapper
- DashboardMapper
- InventoryHistoryMapper
- InventoryInMapper
- InventoryOutMapper

### member モジュール（全て未移植）

| コンポーネント | 状態 |
|--------------|------|
| MemberController | 未移植 |
| MemberService | 未移植 |
| Member Entity | 未移植 |
| MemberMapper | 未移植 |

### order モジュール（全て未移植）

| コンポーネント | 状態 |
|--------------|------|
| OrderController | 未移植 |
| OrderService | 未移植 |
| Order Entity | 未移植 |
| OrderMapper | 未移植 |

---

## 移植フェーズ

### Phase 1: Retail モジュール補完

**目標**: retail/ 配下の不足コンポーネントを移植

#### 1.1 Controller (2個追加)
```
services/retail-be/src/main/java/com/smartdx/retail/controller/
├── PaymentController.java              # 新規
└── InventoryTransactionController.java # 新規
```

#### 1.2 Service (5個追加)
```
services/retail-be/src/main/java/com/smartdx/retail/service/
├── PaymentService.java + impl/
├── PaymentDemoService.java + impl/
├── InventoryTransactionService.java + impl/
├── HeartbeatService.java + impl/
└── DeviceMonitorService.java + impl/
```

#### 1.3 Entity (3個追加)
```
services/retail-be/src/main/java/com/smartdx/retail/model/entity/
├── InventoryHistory.java
├── InventoryIn.java
└── InventoryOut.java
```

#### 1.4 Mapper (4個追加)
```
services/retail-be/src/main/java/com/smartdx/retail/mapper/
├── DashboardMapper.java
├── InventoryHistoryMapper.java
├── InventoryInMapper.java
└── InventoryOutMapper.java
```

#### 1.5 Converter (5個追加)
```
services/retail-be/src/main/java/com/smartdx/retail/converter/
├── AlertConverter.java
├── DeviceConverter.java
├── InventoryConverter.java
├── InventoryTransactionConverter.java
└── StoreConverter.java
```

#### 1.6 Model (Form/Query/VO/Enum)
- Form: 13個追加
- Query: 6個追加
- VO: 15個追加
- Enum: InventoryStatus.java

#### 1.7 Scheduler/Job
```
services/retail-be/src/main/java/com/smartdx/retail/
├── scheduler/DeviceMonitorScheduler.java
└── job/AlertDetectionJob.java
```

### Phase 2: Member モジュール移植

**目標**: member/ 配下を retail-be に統合

```
services/retail-be/src/main/java/com/smartdx/retail/
├── controller/MemberController.java
├── service/MemberService.java + impl/
├── model/entity/Member.java
└── mapper/MemberMapper.java
```

### Phase 3: Order モジュール移植

**目標**: order/ 配下を retail-be に統合

```
services/retail-be/src/main/java/com/smartdx/retail/
├── controller/OrderController.java
├── service/OrderService.java + impl/
├── model/entity/Order.java
└── mapper/OrderMapper.java
```

### Phase 4: DB マイグレーション

#### 4.1 テーブル追加
```sql
-- services/retail-be/src/main/resources/db/migration/retail/V2__add_modules_tables.sql
CREATE TABLE retail_inventory_history (...);
CREATE TABLE retail_inventory_in (...);
CREATE TABLE retail_inventory_out (...);
CREATE TABLE retail_member (...);
CREATE TABLE retail_order (...);
```

### Phase 5: 統合テスト

- API互換性テスト
- ビルド確認: `mvn clean package -DskipTests`

---

## パッケージ対応表

| 移植元 (smart-retail) | 移植先 (retail-be) |
|----------------------|-------------------|
| `com.youlai.boot.modules.retail.*` | `com.smartdx.retail.*` |
| `com.youlai.boot.modules.member.*` | `com.smartdx.retail.*` (統合) |
| `com.youlai.boot.modules.order.*` | `com.smartdx.retail.*` (統合) |

### Import 変換

| 移植元 | 移植先 |
|--------|--------|
| `com.youlai.boot.common.result.Result` | `com.smartdx.core.result.ApiResponse` |
| `com.youlai.boot.common.result.PageResult` | `com.smartdx.core.result.PageResponse` |
| `com.youlai.boot.common.base.BaseEntity` | `com.smartdx.core.base.BaseEntity` |
| `com.baomidou.mybatisplus.extension.plugins.pagination.Page` | 同じ（変更なし） |

---

## 主要ソースファイル

### 移植元（参照）
```
../smart-retail/apps/backend/src/main/java/com/youlai/boot/modules/
├── retail/controller/PaymentController.java
├── retail/controller/InventoryTransactionController.java
├── retail/service/impl/*.java
├── retail/model/entity/*.java
├── retail/model/form/*.java
├── retail/model/query/*.java
├── retail/model/vo/*.java
├── retail/converter/*.java
├── retail/scheduler/DeviceMonitorScheduler.java
├── retail/job/AlertDetectionJob.java
├── member/*.java
└── order/*.java
```

### 移植先
```
services/retail-be/src/main/java/com/smartdx/retail/
├── controller/
├── service/
├── model/entity/
├── model/form/
├── model/query/
├── model/vo/
├── converter/
├── scheduler/
└── job/
```

---

## 検証方法

1. **ビルド確認**: `mvn clean package -DskipTests -pl services/retail-be`
2. **統合ビルド**: `mvn clean package -DskipTests -pl app`
3. **起動テスト**: `mvn spring-boot:run -pl app`
4. **API テスト**:
   - GET /api/v1/retail/products
   - POST /api/v1/retail/payments
   - GET /api/v1/retail/inventory-transactions
   - GET /api/v1/retail/members
   - GET /api/v1/retail/orders

---

## リスクと対策

| リスク | 対策 |
|--------|------|
| Import エラー | パッケージ変換スクリプト使用 |
| BaseEntity 差異 | tenant_id 追加対応 |
| API レスポンス差異 | ApiResponse 統一 |

---

## 優先順位

1. **Phase 1** (Retail補完) - 最優先、コア機能
2. **Phase 2** (Member移植) - 高優先
3. **Phase 3** (Order移植) - 高優先
4. **Phase 4** (DB) - Phase 1-3と並行
5. **Phase 5** (テスト) - 各Phase完了後

---

## ファイル数サマリ

| Phase | 新規ファイル数（概算） |
|-------|---------------------|
| Phase 1 | 約50ファイル |
| Phase 2 | 約5ファイル |
| Phase 3 | 約5ファイル |
| Phase 4 | 1ファイル (SQL) |
| **合計** | **約61ファイル** |
