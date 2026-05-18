# ADR-005: マルチテナント戦略

## ステータス
承認済み

## 日付
2025-05-17

## コンテキスト
Smart DX プロダクト群において、マルチテナントをどのように実装・運用するかを決定する必要がある。

### 現状
- smart-property-dx2: マルチテナント対応済み (複数テナント運用中)
- smart-retail-dx: マルチテナント未対応 (シングルテナント)

## 決定

### 採用方式
**行レベル分離型 (Row-Level Multi-Tenancy)** を採用

- 同一MySQLインスタンス
- 同一テーブルに `tenant_id` カラムを持つ
- `WHERE tenant_id = ?` で分離
- データベースはサービスごとに分離 (property_db / retail_db)

### 実装方式
smart-property-dx2の実装をベースに `libs/smart-be-tenant` として共通化

#### 主要コンポーネント
1. **TenantContextHolder**: TransmittableThreadLocal でテナントID保持
2. **TenantContextFilter**: HTTP リクエストからテナントID解決
3. **MyTenantLineHandler**: MyBatis-Plus でSQL自動フィルタリング
4. **TenantProperties**: 設定プロパティ

#### テナントID解決優先順位
1. SecurityContext (認証済みユーザー)
2. JWT Authorization Header
3. ドメインマッピング (Host header)
4. デフォルトテナント (設定による)

### サービスごとの運用方針

| サービス | コード | 運用 |
|---------|--------|------|
| property-be | マルチテナント対応 | 複数テナント運用 |
| retail-be | マルチテナント対応 | **1テナント固定** (default) |
| 将来サービス | マルチテナント対応 | サービスごとに判断 |

### retail-be の固定運用設定
```yaml
smart-be-tenant:
  enabled: true
  default-tenant: default
  force-default: true   # 全リクエストを default テナントで処理
```

## 理由

### 1. 全サービスで同じ前提
コードベース・libsの設計が統一でき、保守性が高い

### 2. 将来の拡張性
retail を本格マルチテナント運用したいとき、運用切替だけで対応可能

### 3. ポートフォリオ訴求
「マルチテナントSaaS基盤を統一設計した」というアピールが可能

### 4. 実装コスト最小
retail のテーブルに `tenant_id` を追加し、固定値 `default` で運用するだけ

## 設計詳細

### テーブル設計
- 全テーブルに `tenant_id VARCHAR(50) NOT NULL` カラム
- インデックスは `(tenant_id, ...)` 複合インデックスを基本

### 除外テーブル
以下のシステムテーブルはテナントフィルタを適用しない:
- sys_tenant (テナントマスタ)
- sys_tenant_plan (テナントプラン)
- sys_menu (メニューマスタ)
- sys_dict / sys_dict_item (辞書マスタ)
- sys_config (システム設定)

### Redis Key
`{service}:{tenant_id}:{業務key}` 形式
- property: `property:tenant-A:search:condition:userId-123`
- retail: `retail:default:cart:userId-456`

### OpenSearch
- ドキュメントに `tenant_id` フィールドを持つ
- 検索時に必ず `tenant_id` filter を付与

## 影響
- retail-be の全テーブルに `tenant_id` カラム追加が必要 (Phase 3)
- 既存データは `tenant_id = 'default'` で一括更新
- FE側の修正は最小限 (retail は tenant_id を意識しなくてよい)

## 関連
- ADR-000: 依存関係調査 (smart-property-dx2 の実装詳細)
- docs/architecture/multi-tenant.md: 実装詳細ドキュメント
