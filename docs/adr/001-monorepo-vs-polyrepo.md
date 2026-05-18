# ADR-001: Monorepo vs Polyrepo

## ステータス
承認済み

## 日付
2025-05-17

## コンテキスト
複数の業界向けDXプロダクト (property-dx, retail-dx, 将来の追加ドメイン) のバックエンドをどのように管理するかを決定する必要がある。

### 選択肢
1. **Polyrepo**: 各サービスを独立したリポジトリで管理
2. **Monorepo**: 全サービスを1つのリポジトリで管理
3. **Hybrid**: ドメインリポ (FE) + BE基盤リポ (本案)

## 決定
**Hybrid方式を採用**: ドメインリポ (FE + docs) と BE基盤リポ (smart-dx-backend) を分離

## 理由

### メリット
1. **共通ライブラリの一元管理**: libs/ 配下で共通コードを管理、バージョン整合性を保証
2. **依存関係の明確化**: サービス間の直接参照を防止、libs経由のみに制限
3. **統一的なビルド・CI/CD**: 全サービスで同一のビルド設定を共有
4. **マルチテナント基盤の共有**: smart-be-tenant を全サービスで利用
5. **VPSリソース最適化**: 統一されたメモリチューニング戦略の適用

### Polyrepoの問題点
- 共通コードの重複
- バージョン管理の複雑化
- 依存関係の追跡が困難

### 完全Monorepoの問題点
- FEとBEの変更頻度の差異による不要なCI実行
- ドメインリポの責務が不明確になる

## 構成

```
smart-dx-backend/           # BE基盤リポ (本リポ)
├── apps/backend/
│   ├── services/
│   │   ├── property-be/
│   │   └── retail-be/
│   └── libs/
│       ├── smart-be-core/
│       ├── smart-be-tenant/
│       └── ...
└── platform/

smart-property-dx2/         # 不動産ドメインリポ
├── apps/frontend/
└── docs/domain/

smart-retail-dx/            # 小売ドメインリポ
├── apps/frontend/
└── docs/domain/
```

## 影響
- ドメインリポからBEコードを削除
- BE修正は smart-dx-backend で実施
- CI/CDはリポ単位で独立
