-- System Service Database Schema
-- Baseline schema for system management

-- Tenant table
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT 'Tenant name',
    code VARCHAR(50) NOT NULL COMMENT 'Tenant code',
    domain VARCHAR(255) COMMENT 'Tenant domain',
    status TINYINT DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tenant';

-- User table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT 'Tenant ID',
    username VARCHAR(64) NOT NULL COMMENT 'Username',
    password VARCHAR(128) NOT NULL COMMENT 'Password (encrypted)',
    nickname VARCHAR(64) COMMENT 'Nickname',
    mobile VARCHAR(20) COMMENT 'Mobile phone',
    email VARCHAR(128) COMMENT 'Email',
    gender TINYINT DEFAULT 0 COMMENT '0=unknown, 1=male, 2=female',
    avatar VARCHAR(255) COMMENT 'Avatar URL',
    status TINYINT DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    dept_id BIGINT COMMENT 'Department ID',
    can_switch_tenant TINYINT DEFAULT 0 COMMENT 'Can switch tenant: 1=yes, 0=no',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_username (username),
    INDEX idx_mobile (mobile),
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User';

-- Role table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT 'Tenant ID',
    name VARCHAR(64) NOT NULL COMMENT 'Role name',
    code VARCHAR(64) NOT NULL COMMENT 'Role code',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    status TINYINT DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    data_scope TINYINT DEFAULT 0 COMMENT 'Data scope: 0=all, 1=dept, 2=dept_and_sub, 3=self',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    INDEX idx_tenant_id (tenant_id),
    UNIQUE KEY uk_tenant_code (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role';

-- Department table
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT 'Tenant ID',
    name VARCHAR(64) NOT NULL COMMENT 'Department name',
    parent_id BIGINT DEFAULT 0 COMMENT 'Parent department ID',
    tree_path VARCHAR(255) COMMENT 'Tree path',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    status TINYINT DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Department';

-- Menu table
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT 'Parent menu ID',
    tree_path VARCHAR(255) COMMENT 'Tree path',
    name VARCHAR(64) NOT NULL COMMENT 'Menu name',
    type TINYINT NOT NULL COMMENT '1=catalog, 2=menu, 3=button, 4=link',
    route_name VARCHAR(128) COMMENT 'Route name',
    route_path VARCHAR(255) COMMENT 'Route path',
    component VARCHAR(255) COMMENT 'Component path',
    perm VARCHAR(128) COMMENT 'Permission code',
    icon VARCHAR(64) COMMENT 'Icon',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    visible TINYINT DEFAULT 1 COMMENT '1=visible, 0=hidden',
    redirect VARCHAR(255) COMMENT 'Redirect path',
    always_show TINYINT DEFAULT 0 COMMENT 'Always show',
    keep_alive TINYINT DEFAULT 0 COMMENT 'Keep alive',
    params VARCHAR(255) COMMENT 'Route params',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Menu';

-- User-Role relation
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-Role relation';

-- Role-Menu relation
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    menu_id BIGINT NOT NULL COMMENT 'Menu ID',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-Menu relation';

-- Dictionary table
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT COMMENT 'Tenant ID (null for system dict)',
    name VARCHAR(64) NOT NULL COMMENT 'Dictionary name',
    code VARCHAR(64) NOT NULL COMMENT 'Dictionary code',
    status TINYINT DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    remark VARCHAR(255) COMMENT 'Remark',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary';

-- Dictionary item table
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_id BIGINT NOT NULL COMMENT 'Dictionary ID',
    name VARCHAR(64) NOT NULL COMMENT 'Item name',
    value VARCHAR(128) NOT NULL COMMENT 'Item value',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    status TINYINT DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    tag_type VARCHAR(32) COMMENT 'Tag type for display',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    INDEX idx_dict_id (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary item';

-- System log table
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT COMMENT 'Tenant ID',
    module VARCHAR(64) COMMENT 'Module',
    content VARCHAR(255) COMMENT 'Log content',
    request_uri VARCHAR(255) COMMENT 'Request URI',
    method VARCHAR(20) COMMENT 'HTTP method',
    ip VARCHAR(64) COMMENT 'Client IP',
    province VARCHAR(64) COMMENT 'Province',
    city VARCHAR(64) COMMENT 'City',
    execution_time BIGINT COMMENT 'Execution time in ms',
    browser VARCHAR(128) COMMENT 'Browser',
    os VARCHAR(128) COMMENT 'Operating system',
    operator_id BIGINT COMMENT 'Operator ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System log';

-- System config table
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL COMMENT 'Config key',
    config_value TEXT COMMENT 'Config value',
    remark VARCHAR(255) COMMENT 'Remark',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System config';

-- Initial data: Default tenant
INSERT INTO sys_tenant (id, name, code, status) VALUES (1, 'Default Tenant', 'default', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Initial data: Admin user (password: 123456)
INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant)
VALUES (1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKqjZ5SG', 'Administrator', 1, 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- Initial data: Admin role
INSERT INTO sys_role (id, tenant_id, name, code, status, data_scope)
VALUES (1, 1, 'Administrator', 'ADMIN', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Initial data: Admin user-role relation
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Initial data: Root department
INSERT INTO sys_dept (id, tenant_id, name, parent_id, tree_path, sort, status)
VALUES (1, 1, 'Root Department', 0, '/1/', 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);
