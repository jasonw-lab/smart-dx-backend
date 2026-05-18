# 命名規則

## リポジトリ

| パターン | 役割 | 例 |
|---------|------|-----|
| `smart-{業界}-dx` | 業界特化ドメインリポ (FE + docs) | smart-property-dx2, smart-retail-dx |
| `smart-dx-{基盤名}` | 横断的な共通基盤 | smart-dx-backend |

**「-dx」の位置で役割を区別**

## サービス

| パターン | 例 |
|---------|-----|
| `{domain}-be` | property-be, retail-be |

## ライブラリ

| パターン | 例 |
|---------|-----|
| `smart-be-{機能}` | smart-be-core, smart-be-tenant |

## パッケージ

| 種別 | パターン | 例 |
|------|---------|-----|
| サービス | `com.smartdx.{domain}` | com.smartdx.property |
| ライブラリ | `com.smartdx.{libname}` | com.smartdx.tenant |

## データベース

| 種別 | パターン | 例 |
|------|---------|-----|
| DB名 | `{domain}_db` | property_db, retail_db |
| テナントカラム | `tenant_id` | 全テーブル共通 |

## Redis Key

```
{service}:{tenant_id}:{業務key}
```

例: `property:tenant-A:search:condition:userId-123`

## Docker イメージ

```
smart-dx/{service}:{version}
```

例: `smart-dx/property-be:1.0.0`

詳細は [ADR-004](adr/004-naming-convention.md) を参照。
