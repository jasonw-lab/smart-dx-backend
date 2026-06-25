-- Add tenant_id to dictionary tables for multi-tenant support
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0 COMMENT '租户ID' AFTER id;
ALTER TABLE sys_dict ADD INDEX IF NOT EXISTS idx_tenant_id (tenant_id);

ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0 COMMENT '租户ID' AFTER id;
ALTER TABLE sys_dict_item ADD INDEX IF NOT EXISTS idx_tenant_id (tenant_id);
