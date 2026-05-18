# Smart DX Backend

> 複数業界向けDXプロダクトの共通BE基盤monorepo。
> 行レベル分離型マルチテナント機構を共通ライブラリ化し、
> 全サービスで統一されたSaaS基盤として運用。
> Spring Boot multi-module + JVM軽量化で
> VPS上に複数BEサービスを並べる構成を実現。

## このリポの役割

Smart DX シリーズ (不動産DX / 小売DX / 今後追加予定) の
バックエンドサービスを集約した monorepo。

## 関連リポジトリ

- [smart-property-dx2](https://github.com/ross-dev2024/smart-property-dx2) - 不動産ドメイン (FE + docs、複数テナント運用)
- [smart-retail-dx](https://github.com/ross-dev2024/smart-retail-dx) - 小売ドメイン (FE + docs、1テナント固定運用)

## 設計思想

- **マルチドメイン運用**: 単一monorepoで複数ドメインのBEを管理、共通libsでDRY維持
- **マルチテナント基盤**: 全サービスがマルチテナント対応 (`libs/smart-be-tenant`)
- **独立デプロイ**: 各サービスは独立Spring Boot、デプロイ単位を分離
- **VPS最適化**: 1サービス 150-250MB目標、JVMチューニング適用

詳細: [docs/architecture/overview.md](docs/architecture/overview.md) / [docs/architecture/multi-tenant.md](docs/architecture/multi-tenant.md)

## ディレクトリ構成

```
smart-dx-backend/
├── apps/backend/
│   ├── pom.xml                    # parent pom
│   ├── services/
│   │   ├── property-be/           # 不動産BE (Phase 2)
│   │   ├── retail-be/             # 小売BE (Phase 3)
│   │   └── _template/             # 新サービス用テンプレート
│   └── libs/
│       ├── smart-be-core/         # 基盤ユーティリティ
│       ├── smart-be-security/     # 認証・認可
│       ├── smart-be-tenant/       # マルチテナント機構
│       ├── smart-be-redis/        # Redis操作
│       ├── smart-be-opensearch/   # 検索基盤
│       └── smart-be-observability/# 可観測性
├── docs/
│   ├── architecture/              # アーキテクチャドキュメント
│   ├── adr/                       # 設計判断記録
│   ├── services/                  # サービス別ドキュメント
│   └── ops/                       # 運用ドキュメント
├── platform/
│   ├── docker-compose.yml
│   ├── nginx/
│   └── mysql/init/
└── .github/workflows/
```

## クイックスタート

### ミドルウェア起動

```bash
cd platform
docker-compose up -d mysql redis opensearch
```

### ビルド

```bash
cd apps/backend
mvn clean compile
```

### 個別サービス起動 (Phase 2以降)

```bash
cd apps/backend
mvn -pl services/property-be spring-boot:run
```

## マルチテナント

- **方式**: 行レベル分離型 (各テーブルに `tenant_id` カラム)
- **実装**: `libs/smart-be-tenant` で機構を統一
- **運用**:
  - property-be: 複数テナント運用
  - retail-be: 1テナント固定運用 (default、DEMO用途)

詳細: [docs/architecture/multi-tenant.md](docs/architecture/multi-tenant.md)

## ドキュメント

- [アーキテクチャ概要](docs/architecture/overview.md)
- [マルチテナント設計](docs/architecture/multi-tenant.md)
- [命名規則](docs/naming-convention.md)
- [新サービス追加ガイド](docs/new-service-guide.md)
- [ADR一覧](docs/adr/)

## メモリ目標

- 1サービスあたり 150-250MB
- 詳細: [docs/ops/memory-tuning.md](docs/ops/memory-tuning.md)
