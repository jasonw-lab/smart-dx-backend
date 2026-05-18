# Smart DX Backend ドキュメント

## ディレクトリ構成

```
docs/
├── README.md                  # このファイル
├── naming-convention.md       # 命名規則
├── new-service-guide.md       # 新サービス追加手順
├── architecture/
│   ├── overview.md            # アーキテクチャ概要
│   ├── multi-tenant.md        # マルチテナント設計
│   ├── service-communication.md  # サービス間通信 (Phase 2+)
│   ├── shared-libs.md         # 共通ライブラリ (Phase 2+)
│   └── deployment.md          # デプロイメント (Phase 4)
├── services/
│   ├── property-be.md         # 不動産BE (Phase 2)
│   └── retail-be.md           # 小売BE (Phase 3)
├── adr/
│   ├── 000-dependency-audit.md    # 依存関係調査
│   ├── 001-monorepo-vs-polyrepo.md # Monorepo採用理由
│   ├── 002-why-shared-libs.md     # 共通ライブラリ設計
│   ├── 004-naming-convention.md   # 命名規則
│   └── 005-multi-tenant-strategy.md # マルチテナント戦略
└── ops/
    ├── memory-tuning.md       # メモリチューニング
    └── runbook.md             # 運用手順書
```

## ADR (Architecture Decision Records)

設計判断を記録したドキュメント。新しい判断を追加する際は連番でファイルを作成。

| ADR | タイトル | ステータス |
|-----|---------|----------|
| 000 | 依存関係調査 | 承認済み |
| 001 | Monorepo vs Polyrepo | 承認済み |
| 002 | 共通ライブラリ設計 | 承認済み |
| 004 | 命名規則 | 承認済み |
| 005 | マルチテナント戦略 | 承認済み |

## クイックリンク

- [アーキテクチャ概要](architecture/overview.md)
- [マルチテナント設計](architecture/multi-tenant.md)
- [新サービス追加ガイド](new-service-guide.md)
- [運用手順書](ops/runbook.md)
