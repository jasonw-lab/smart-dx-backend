-- ============================================================================
-- Migration V3: Fix property_demo_embedding property_key column
-- Date: 2026-05-20
-- Description: property_key カラムがNOT NULLで存在する場合にNULLableに変更
-- ============================================================================

-- property_key カラムが存在する場合、NULLableでデフォルトNULLに変更
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'property_demo_embedding'
      AND column_name = 'property_key'
);

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE property_demo_embedding MODIFY COLUMN property_key CHAR(36) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
