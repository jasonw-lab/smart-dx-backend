-- ============================================================================
-- Migration V2: Alter property_demo_embedding add missing columns
-- Date: 2026-05-19
-- Description: 既存DBの property_demo_embedding にカラム不足がある場合に追加
-- ============================================================================

-- demo_ref カラムが存在しない場合に追加
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'demo_ref'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE property_demo_embedding ADD COLUMN demo_ref VARCHAR(64) NOT NULL DEFAULT "" COMMENT "DEMO参照キー" AFTER tenant_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- embedding_vector カラムが存在しない場合に追加
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'embedding_vector'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE property_demo_embedding ADD COLUMN embedding_vector JSON NOT NULL COMMENT "embedding vector (512次元)" AFTER demo_ref',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- thumbnail_url カラムが存在しない場合に追加
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'thumbnail_url'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE property_demo_embedding ADD COLUMN thumbnail_url VARCHAR(512) NULL COMMENT "サムネイルURL" AFTER embedding_vector',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- title カラムが存在しない場合に追加
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'title'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE property_demo_embedding ADD COLUMN title VARCHAR(256) NOT NULL DEFAULT "" COMMENT "表示タイトル" AFTER thumbnail_url',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- description カラムが存在しない場合に追加
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'description'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE property_demo_embedding ADD COLUMN description VARCHAR(512) NULL COMMENT "説明" AFTER title',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- is_active カラムが存在しない場合に追加
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'is_active'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE property_demo_embedding ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT "有効フラグ" AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- uk_demo_ref ユニークキーが存在しない場合に追加
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND index_name = 'uk_demo_ref'
);

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE property_demo_embedding ADD UNIQUE KEY uk_demo_ref (demo_ref)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
