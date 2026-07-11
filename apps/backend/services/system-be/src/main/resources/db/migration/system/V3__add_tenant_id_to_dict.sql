-- Add tenant_id to dictionary tables for multi-tenant support
SET @schema_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'sys_dict'
       AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE sys_dict ADD COLUMN tenant_id BIGINT DEFAULT 0 COMMENT ''租户ID'' AFTER id',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'sys_dict'
       AND INDEX_NAME = 'idx_tenant_id') = 0,
    'ALTER TABLE sys_dict ADD INDEX idx_tenant_id (tenant_id)',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'sys_dict_item'
       AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE sys_dict_item ADD COLUMN tenant_id BIGINT DEFAULT 0 COMMENT ''租户ID'' AFTER id',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'sys_dict_item'
       AND INDEX_NAME = 'idx_tenant_id') = 0,
    'ALTER TABLE sys_dict_item ADD INDEX idx_tenant_id (tenant_id)',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
