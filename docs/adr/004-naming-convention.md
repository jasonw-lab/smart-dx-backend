# ADR-004: 命名規則

## ステータス
承認済み

## 日付
2025-05-17

## コンテキスト
複数のリポジトリ、サービス、ライブラリを管理するにあたり、一貫した命名規則を定義する必要がある。

## 決定

### リポジトリ命名規則

| パターン | 役割 | 例 |
|---------|------|-----|
| `smart-{業界}-dx` | 業界特化ドメインリポ (FE + docs) | smart-property-dx2, smart-retail-dx |
| `smart-dx-{基盤名}` | 横断的な共通基盤 | smart-dx-backend |

**「-dx」の位置で役割を区別**:
- `smart-{X}-dx`: ドメイン (業界特化)
- `smart-dx-{X}`: 基盤 (横断的)

### サービス命名規則

| パターン | 例 |
|---------|-----|
| `{domain}-be` | property-be, retail-be |

### ライブラリ命名規則

| パターン | 例 |
|---------|-----|
| `smart-be-{機能}` | smart-be-core, smart-be-tenant, smart-be-redis |

### パッケージ命名規則

| 種別 | パターン | 例 |
|------|---------|-----|
| サービス | `com.smartdx.{domain}` | com.smartdx.property, com.smartdx.retail |
| ライブラリ | `com.smartdx.{libname}` | com.smartdx.core, com.smartdx.tenant |

### データベース命名規則

| 種別 | パターン | 例 |
|------|---------|-----|
| データベース名 | `{domain}_db` | property_db, retail_db |
| テナントカラム | `tenant_id` | (全テーブル共通) |

### Redis Key 命名規則

| パターン | 例 |
|---------|-----|
| `{service}:{tenant_id}:{業務key}` | property:tenant-A:search:condition:userId-123 |

### Docker イメージ命名規則

| パターン | 例 |
|---------|-----|
| `smart-dx/{service}:{version}` | smart-dx/property-be:1.0.0 |

## 理由
- 一貫性のある命名により、新メンバーの学習コストを削減
- コードベース全体での検索性を向上
- 役割の明確化による設計判断の容易化

## 影響
- 新規作成するファイル・ディレクトリ・クラスは本規則に従う
- 既存コードは段階的に移行
