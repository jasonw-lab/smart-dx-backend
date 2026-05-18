-- ============================================================================
-- Baseline Migration: Property DB Schema
-- Date: 2026-05-18
-- Description: Initial schema for property-be (migrated from smart-property-dx2)
-- Note: All tables include tenant_id for multi-tenant support
-- ============================================================================

-- ----------------------------------------------------------------------------
-- property_listing: 物件マスタ
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_listing` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `property_key` CHAR(36) NOT NULL COMMENT '物件キー（内部UUID v4）',
    `scope` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'スコープ（DRAFT/PUBLISHED）',
    `version` INT NOT NULL DEFAULT 1 COMMENT 'バージョン',
    `area` VARCHAR(100) DEFAULT NULL COMMENT 'エリア',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '住所',
    `property_type` VARCHAR(50) DEFAULT NULL COMMENT '物件種別',
    `price_jpy` BIGINT DEFAULT NULL COMMENT '価格（円）',
    `layout` VARCHAR(50) DEFAULT NULL COMMENT '間取り',
    `area_sqm` DECIMAL(10,2) DEFAULT NULL COMMENT '面積（㎡）',
    `station_walk_min` INT DEFAULT NULL COMMENT '最寄駅徒歩分',
    `built_year_month` VARCHAR(10) DEFAULT NULL COMMENT '築年月',
    `listed_date` DATE DEFAULT NULL COMMENT '掲載日',
    `listed_year` INT DEFAULT NULL COMMENT '掲載年',
    `priority_rank` VARCHAR(10) DEFAULT NULL COMMENT '優先順位',
    `review_status` VARCHAR(20) DEFAULT NULL COMMENT 'レビューステータス',
    `registrant_user_id` BIGINT DEFAULT NULL COMMENT '登録者ユーザーID',
    `registrant_display_name` VARCHAR(100) DEFAULT NULL COMMENT '登録者表示名',
    `registered_at` DATETIME DEFAULT NULL COMMENT '登録日時',
    `published_at` DATETIME DEFAULT NULL COMMENT '公開日時',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '削除フラグ（0:有効, 1:削除）',
    `active_property_key` CHAR(36) GENERATED ALWAYS AS (IF(`is_deleted` = 0, `property_key`, NULL)) STORED COMMENT 'active のみ property_key ユニーク化',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_active_property_key_scope` (`tenant_id`, `active_property_key`, `scope`),
    KEY `idx_tenant_property_key` (`tenant_id`, `property_key`),
    KEY `idx_tenant_scope_status` (`tenant_id`, `scope`, `review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物件マスタ';

-- ----------------------------------------------------------------------------
-- property_asset: 物件画像・ファイル
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `property_key` CHAR(36) NOT NULL COMMENT '物件キー（内部UUID）',
    `scope` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'スコープ',
    `asset_key` CHAR(36) NOT NULL COMMENT 'アセットキー（UUID）',
    `active_asset_key` CHAR(36) GENERATED ALWAYS AS (IF(`is_deleted` = 0, `asset_key`, NULL)) STORED COMMENT 'active のみユニーク化',
    `flag` VARCHAR(50) DEFAULT NULL COMMENT 'フラグ（MAIN/SUB等）',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT 'ファイルパス',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'ファイル名',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'Content-Type',
    `file_size` BIGINT DEFAULT NULL COMMENT 'ファイルサイズ（bytes）',
    `sort_no` INT DEFAULT 0 COMMENT 'ソート順',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '削除フラグ',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_property_key_active_asset_scope` (`tenant_id`, `property_key`, `active_asset_key`, `scope`),
    KEY `idx_tenant_property_key_scope_flag` (`tenant_id`, `property_key`, `scope`, `flag`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物件アセット';

-- ----------------------------------------------------------------------------
-- property_document: 物件ドキュメント
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `property_key` CHAR(36) NOT NULL COMMENT '物件キー（内部UUID）',
    `scope` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'スコープ',
    `doc_key` CHAR(36) NOT NULL COMMENT 'ドキュメントキー（UUID）',
    `active_doc_key` CHAR(36) GENERATED ALWAYS AS (IF(`is_deleted` = 0, `doc_key`, NULL)) STORED COMMENT 'active のみユニーク化',
    `doc_type` VARCHAR(50) DEFAULT NULL COMMENT 'ドキュメント種別',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT 'ファイルパス',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'ファイル名',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'Content-Type',
    `file_size` BIGINT DEFAULT NULL COMMENT 'ファイルサイズ（bytes）',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '削除フラグ',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_property_key_active_doc_scope` (`tenant_id`, `property_key`, `active_doc_key`, `scope`),
    KEY `idx_tenant_property_key_scope_type` (`tenant_id`, `property_key`, `scope`, `doc_type`),
    KEY `idx_tenant_property_key_scope_doc_key` (`tenant_id`, `property_key`, `scope`, `doc_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物件ドキュメント';

-- ----------------------------------------------------------------------------
-- property_review: 物件レビュー
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `property_key` CHAR(36) NOT NULL COMMENT '物件キー（内部UUID）',
    `reviewer_user_id` BIGINT DEFAULT NULL COMMENT 'レビュアーユーザーID',
    `reviewer_display_name` VARCHAR(100) DEFAULT NULL COMMENT 'レビュアー表示名',
    `status` VARCHAR(20) DEFAULT NULL COMMENT 'ステータス',
    `comment` TEXT DEFAULT NULL COMMENT 'コメント',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT 'レビュー日時',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '削除フラグ',
    `active_property_key` CHAR(36) GENERATED ALWAYS AS (IF(`is_deleted` = 0, `property_key`, NULL)) STORED COMMENT 'active のみユニーク化',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_active_review_property_key` (`tenant_id`, `active_property_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物件レビュー';

-- ----------------------------------------------------------------------------
-- property_intake_job: 物件取込ジョブ
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_intake_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `job_key` CHAR(36) NOT NULL COMMENT 'ジョブキー（UUID）',
    `job_type` VARCHAR(20) NOT NULL COMMENT 'ジョブ種別（BULK/SINGLE）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'ステータス',
    `property_key` CHAR(36) DEFAULT NULL COMMENT '対象物件（個別登録時のみ、内部UUID）',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT 'ファイルパス（BULK時）',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'ファイル名',
    `total_count` INT DEFAULT 0 COMMENT '総件数',
    `success_count` INT DEFAULT 0 COMMENT '成功件数',
    `error_count` INT DEFAULT 0 COMMENT 'エラー件数',
    `owner_user_id` BIGINT DEFAULT NULL COMMENT '実行者ユーザーID',
    `owner_display_name` VARCHAR(100) DEFAULT NULL COMMENT '実行者表示名',
    `started_at` DATETIME DEFAULT NULL COMMENT '開始日時',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完了日時',
    `resubmit_of_job_key` CHAR(36) DEFAULT NULL COMMENT '修正再提出元ジョブキー',
    `resubmit_of_property_key` CHAR(36) DEFAULT NULL COMMENT '修正再提出対象 propertyKey',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_job_key` (`tenant_id`, `job_key`),
    KEY `idx_tenant_job_property_key` (`tenant_id`, `property_key`),
    KEY `idx_tenant_status_created` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物件取込ジョブ';

-- ----------------------------------------------------------------------------
-- property_intake_job_error: ジョブエラー詳細
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_intake_job_error` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `job_key` CHAR(36) NOT NULL COMMENT 'ジョブキー',
    `property_key` CHAR(36) DEFAULT NULL COMMENT '物件キー（内部UUID）',
    `row_no` INT DEFAULT NULL COMMENT '行番号',
    `error_code` VARCHAR(50) DEFAULT NULL COMMENT 'エラーコード',
    `error_message` TEXT DEFAULT NULL COMMENT 'エラーメッセージ',
    `field_name` VARCHAR(100) DEFAULT NULL COMMENT 'エラー項目',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_job_key` (`tenant_id`, `job_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ジョブエラー詳細';

-- ----------------------------------------------------------------------------
-- property_intake_job_seq: ジョブシーケンス
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_intake_job_seq` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `job_key` CHAR(36) NOT NULL COMMENT 'ジョブキー',
    `seq_no` INT NOT NULL COMMENT 'シーケンス番号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_job_key` (`tenant_id`, `job_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ジョブシーケンス';

-- ----------------------------------------------------------------------------
-- outbox_event: アウトボックスイベント（非同期処理用）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `outbox_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `event_type` VARCHAR(50) NOT NULL COMMENT 'イベント種別',
    `aggregate_type` VARCHAR(50) NOT NULL COMMENT '集約種別',
    `aggregate_id` VARCHAR(100) NOT NULL COMMENT '集約ID',
    `payload` JSON DEFAULT NULL COMMENT 'ペイロード（JSON）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'ステータス',
    `retry_count` INT DEFAULT 0 COMMENT 'リトライ回数',
    `error_message` TEXT DEFAULT NULL COMMENT 'エラーメッセージ',
    `processed_at` DATETIME DEFAULT NULL COMMENT '処理日時',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_status_created` (`tenant_id`, `status`, `create_time`),
    KEY `idx_tenant_aggregate` (`tenant_id`, `aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='アウトボックスイベント';

-- ----------------------------------------------------------------------------
-- sys_test_config: テスト設定（E2Eテスト用）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_test_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '設定キー',
    `config_value` TEXT DEFAULT NULL COMMENT '設定値',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '説明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_config_key` (`tenant_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='テスト設定';

-- ----------------------------------------------------------------------------
-- property_embedding_ref: 物件埋め込みベクトル参照
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_embedding_ref` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `property_key` CHAR(36) NOT NULL COMMENT '物件キー',
    `embedding_type` VARCHAR(50) NOT NULL COMMENT '埋め込み種別',
    `embedding_id` VARCHAR(100) NOT NULL COMMENT '外部埋め込みID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ステータス',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_property_key_type` (`tenant_id`, `property_key`, `embedding_type`),
    KEY `idx_tenant_embedding_id` (`tenant_id`, `embedding_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物件埋め込み参照';

-- ----------------------------------------------------------------------------
-- property_demo_embedding: デモ用埋め込みベクトル
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_demo_embedding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主キー',
    `tenant_id` BIGINT NOT NULL COMMENT 'テナントID',
    `property_key` CHAR(36) NOT NULL COMMENT '物件キー',
    `embedding_vector` JSON NOT NULL COMMENT '埋め込みベクトル（JSON配列）',
    `embedding_model` VARCHAR(50) DEFAULT NULL COMMENT 'モデル名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_property_key` (`tenant_id`, `property_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='デモ用埋め込みベクトル';
