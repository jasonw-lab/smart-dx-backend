-- System Service Database Schema
-- Baseline schema for system management (synced with existing DB 2026-05-20)

-- Tenant table
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    name VARCHAR(100) NOT NULL COMMENT '租户名称',
    code VARCHAR(50) NOT NULL COMMENT '租户編碼（唯一）',
    contact_name VARCHAR(50) DEFAULT NULL COMMENT '連絡先氏名',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '連絡先電話',
    contact_email VARCHAR(100) DEFAULT NULL COMMENT '連絡先メール',
    domain VARCHAR(100) DEFAULT NULL COMMENT '租户域名（用于域名识别）',
    logo VARCHAR(255) DEFAULT NULL COMMENT '租户Logo',
    plan_id BIGINT DEFAULT NULL COMMENT 'プランID',
    status TINYINT DEFAULT 1 COMMENT '状態(1-正常 0-禁用)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '備考',
    expire_time DATETIME DEFAULT NULL COMMENT '過期時間（NULL表示永不過期）',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    UNIQUE KEY uk_domain (domain),
    KEY idx_status (status),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统租户表';

-- Tenant Plan table
CREATE TABLE IF NOT EXISTS sys_tenant_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'プランID',
    name VARCHAR(100) NOT NULL COMMENT 'プラン名',
    code VARCHAR(50) NOT NULL COMMENT 'プランコード',
    status TINYINT DEFAULT 1 COMMENT '状態(1-有効 0-無効)',
    sort INT DEFAULT 0 COMMENT '並び順',
    remark VARCHAR(500) DEFAULT NULL COMMENT '備考',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户套餐表';

-- User table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT 'ユーザー名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT 'ニックネーム',
    gender TINYINT(1) DEFAULT 1 COMMENT '性別((1-男 2-女 0-不明)',
    password VARCHAR(100) DEFAULT NULL COMMENT 'パスワード',
    dept_id INT DEFAULT NULL COMMENT '部門ID',
    avatar VARCHAR(255) DEFAULT NULL COMMENT 'アバター',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '携帯番号',
    status TINYINT(1) DEFAULT 1 COMMENT '状態(1-正常 0-禁用)',
    can_switch_tenant TINYINT(1) DEFAULT 0 COMMENT 'テナント切替可否',
    email VARCHAR(128) DEFAULT NULL COMMENT 'メール',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    create_by BIGINT DEFAULT NULL COMMENT '作成者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ(0-未削除 1-削除済)',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE KEY uk_username_tenant (username, tenant_id, is_deleted),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

-- Role table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    name VARCHAR(64) NOT NULL COMMENT 'ロール名',
    code VARCHAR(32) NOT NULL COMMENT 'ロールコード',
    sort INT DEFAULT NULL COMMENT '並び順',
    status TINYINT(1) DEFAULT 1 COMMENT 'ロール状態(1-正常 0-停用)',
    data_scope TINYINT DEFAULT NULL COMMENT 'データ権限(1-全データ 2-部門及び子部門 3-本部門 4-本人 5-カスタム)',
    create_by BIGINT DEFAULT NULL COMMENT '作成者ID',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ(0-未削除 1-削除済)',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE KEY uk_tenant_name (tenant_id, name, is_deleted) USING BTREE COMMENT '租户内ロール名唯一索引',
    UNIQUE KEY uk_tenant_code (tenant_id, code, is_deleted) USING BTREE COMMENT '租户内ロールコード唯一索引',
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';

-- Department table
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    name VARCHAR(100) NOT NULL COMMENT '部門名',
    code VARCHAR(100) NOT NULL COMMENT '部門コード',
    parent_id BIGINT DEFAULT 0 COMMENT '親ノードID',
    tree_path VARCHAR(255) NOT NULL COMMENT '親ノードIDパス',
    sort SMALLINT DEFAULT 0 COMMENT '並び順',
    status TINYINT DEFAULT 1 COMMENT '状態(1-正常 0-禁用)',
    create_by BIGINT DEFAULT NULL COMMENT '作成者ID',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    is_deleted TINYINT DEFAULT 0 COMMENT '削除フラグ(1-削除済 0-未削除)',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE KEY uk_tenant_code (tenant_id, code, is_deleted) USING BTREE COMMENT '租户内部門コード唯一索引',
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门管理表';

-- Menu table
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    parent_id BIGINT NOT NULL COMMENT '親メニューID',
    tree_path VARCHAR(255) DEFAULT NULL COMMENT '親ノードIDパス',
    name VARCHAR(64) NOT NULL COMMENT 'メニュー名',
    type CHAR(1) NOT NULL COMMENT 'メニュータイプ（C-ディレクトリ M-メニュー B-ボタン）',
    route_name VARCHAR(255) DEFAULT NULL COMMENT 'ルート名（Vue Router）',
    route_path VARCHAR(128) DEFAULT NULL COMMENT 'ルートパス（Vue Router URL）',
    component VARCHAR(128) DEFAULT NULL COMMENT 'コンポーネントパス',
    perm VARCHAR(128) DEFAULT NULL COMMENT '【ボタン】権限識別子',
    always_show TINYINT DEFAULT 0 COMMENT '【ディレクトリ】常に表示（1-はい 0-いいえ）',
    keep_alive TINYINT DEFAULT 0 COMMENT '【メニュー】キャッシュ有効（1-はい 0-いいえ）',
    visible TINYINT(1) DEFAULT 1 COMMENT '表示状態（1-表示 0-非表示）',
    sort INT DEFAULT 0 COMMENT '並び順',
    icon VARCHAR(64) DEFAULT NULL COMMENT 'アイコン',
    redirect VARCHAR(128) DEFAULT NULL COMMENT 'リダイレクトパス',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    params JSON DEFAULT NULL COMMENT 'ルートパラメータ',
    scope TINYINT(1) NOT NULL DEFAULT 2 COMMENT 'メニュー範囲(1=プラットフォーム 2=業務)',
    PRIMARY KEY (id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单表';

-- User-Role relation
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT 'ユーザーID',
    role_id BIGINT NOT NULL COMMENT 'ロールID',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (user_id, role_id) USING BTREE,
    KEY idx_user_role_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

-- Role-Menu relation
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT 'ロールID',
    menu_id BIGINT NOT NULL COMMENT 'メニューID',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    UNIQUE KEY uk_roleid_menuid (role_id, menu_id) USING BTREE COMMENT 'ロールメニュー唯一索引',
    KEY idx_role_menu_tenant_id (tenant_id),
    KEY idx_tenant_role (tenant_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

-- Role-Dept relation (for custom data scope)
CREATE TABLE IF NOT EXISTS sys_role_dept (
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    role_id BIGINT NOT NULL COMMENT 'ロールID',
    dept_id BIGINT NOT NULL COMMENT '部門ID',
    UNIQUE KEY uk_tenant_roleid_deptid (tenant_id, role_id, dept_id) USING BTREE COMMENT '租户ロール部門唯一索引',
    KEY idx_role_dept_tenant_id (tenant_id),
    KEY idx_tenant_role_dept (tenant_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色部门关联表(用于自定义数据权限)';

-- Tenant-Menu relation
CREATE TABLE IF NOT EXISTS sys_tenant_menu (
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    menu_id BIGINT NOT NULL COMMENT 'メニューID',
    PRIMARY KEY (tenant_id, menu_id) USING BTREE,
    KEY idx_tenant_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户菜单关联表';

-- Tenant-Plan-Menu relation
CREATE TABLE IF NOT EXISTS sys_tenant_plan_menu (
    plan_id BIGINT NOT NULL COMMENT 'プランID',
    menu_id BIGINT NOT NULL COMMENT 'メニューID',
    PRIMARY KEY (plan_id, menu_id) USING BTREE,
    KEY idx_plan_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户套餐菜单关联表';

-- Dictionary table
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_code VARCHAR(50) DEFAULT NULL COMMENT 'タイプコード',
    name VARCHAR(50) DEFAULT NULL COMMENT 'タイプ名',
    status TINYINT(1) DEFAULT 0 COMMENT '状態(0:正常 1:禁用)',
    remark VARCHAR(255) DEFAULT NULL COMMENT '備考',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    create_by BIGINT DEFAULT NULL COMMENT '作成者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '削除フラグ(1-削除済 0-未削除)',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据字典类型表';

-- Dictionary item table
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_code VARCHAR(50) DEFAULT NULL COMMENT '関連字典コード',
    value VARCHAR(50) DEFAULT NULL COMMENT '字典項目値',
    label VARCHAR(100) DEFAULT NULL COMMENT '字典項目ラベル',
    tag_type VARCHAR(50) DEFAULT NULL COMMENT 'タグタイプ（success, warning等）',
    status TINYINT DEFAULT 0 COMMENT '状態（1-正常 0-禁用）',
    sort INT DEFAULT 0 COMMENT '並び順',
    remark VARCHAR(255) DEFAULT NULL COMMENT '備考',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    create_by BIGINT DEFAULT NULL COMMENT '作成者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    PRIMARY KEY (id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据字典项表';

-- System log table
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    module TINYINT NOT NULL COMMENT 'モジュール（LogModule 参照）',
    action_type TINYINT NOT NULL COMMENT '操作タイプ（ActionType 参照）',
    title VARCHAR(100) NOT NULL COMMENT '表示タイトル',
    content TEXT COMMENT 'ログ内容',
    operator_id BIGINT DEFAULT NULL COMMENT '操作者ID',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作者名',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT 'リクエストパス',
    request_method VARCHAR(10) DEFAULT NULL COMMENT 'HTTPメソッド',
    ip VARCHAR(45) DEFAULT NULL COMMENT 'IPアドレス',
    province VARCHAR(100) DEFAULT NULL COMMENT '省份',
    city VARCHAR(100) DEFAULT NULL COMMENT '都市',
    device VARCHAR(100) DEFAULT NULL COMMENT 'デバイス',
    os VARCHAR(100) DEFAULT NULL COMMENT 'OS',
    browser VARCHAR(100) DEFAULT NULL COMMENT 'ブラウザ',
    status TINYINT DEFAULT 1 COMMENT '0失敗 1成功',
    error_msg VARCHAR(255) DEFAULT NULL COMMENT 'エラーメッセージ',
    execution_time INT DEFAULT NULL COMMENT '実行時間(ms)',
    create_time DATETIME DEFAULT NULL COMMENT '操作時間',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_tenant_module_action (tenant_id, module, action_type, create_time),
    KEY idx_tenant_operator (tenant_id, operator_id, create_time),
    KEY idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作日志表';

-- System config table
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_name VARCHAR(50) NOT NULL COMMENT '設定名',
    config_key VARCHAR(50) NOT NULL COMMENT '設定キー',
    config_value VARCHAR(100) NOT NULL COMMENT '設定値',
    remark VARCHAR(255) DEFAULT NULL COMMENT '備考',
    create_time DATETIME DEFAULT NULL COMMENT '作成時間',
    create_by BIGINT DEFAULT NULL COMMENT '作成者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '削除フラグ(0-未削除 1-削除済)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- Notice table
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    title VARCHAR(50) DEFAULT NULL COMMENT '通知タイトル',
    content TEXT COMMENT '通知内容',
    type TINYINT NOT NULL COMMENT '通知タイプ（字典コード：notice_type）',
    level VARCHAR(5) NOT NULL COMMENT '通知レベル（字典code：notice_level）',
    target_type TINYINT NOT NULL COMMENT 'ターゲットタイプ（1:全体 2:指定）',
    target_user_ids VARCHAR(255) DEFAULT NULL COMMENT 'ターゲットユーザーID（カンマ区切り）',
    publisher_id BIGINT DEFAULT NULL COMMENT '発行者ID',
    publish_status TINYINT DEFAULT 0 COMMENT '発行状態（0:未発行 1:発行済 -1:撤回）',
    publish_time DATETIME DEFAULT NULL COMMENT '発行時間',
    revoke_time DATETIME DEFAULT NULL COMMENT '撤回時間',
    create_by BIGINT NOT NULL COMMENT '作成者ID',
    create_time DATETIME NOT NULL COMMENT '作成時間',
    update_by BIGINT DEFAULT NULL COMMENT '更新者ID',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ（0:未削除 1:削除済）',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知公告表';

-- User-Notice relation
CREATE TABLE IF NOT EXISTS sys_user_notice (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    notice_id BIGINT NOT NULL COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT 'ユーザーID',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    is_read TINYINT DEFAULT 0 COMMENT '既読状態（0:未読 1:既読）',
    read_time DATETIME DEFAULT NULL COMMENT '閲覧時間',
    create_time DATETIME NOT NULL COMMENT '作成時間',
    update_time DATETIME DEFAULT NULL COMMENT '更新時間',
    is_deleted TINYINT DEFAULT 0 COMMENT '削除フラグ(0:未削除 1:削除済)',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知公告关联表';

-- Initial data: Default tenant
INSERT INTO sys_tenant (id, name, code, status, create_time) VALUES (1, 'Default Tenant', 'default', 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Initial data: Admin user (password: 123456)
INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, create_time)
VALUES (1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKqjZ5SG', 'Administrator', 1, 1, NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- Initial data: Admin role
INSERT INTO sys_role (id, tenant_id, name, code, status, data_scope, create_time)
VALUES (1, 1, 'Administrator', 'ADMIN', 1, 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Initial data: Admin user-role relation
INSERT INTO sys_user_role (user_id, role_id, tenant_id) VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Initial data: Root department
INSERT INTO sys_dept (id, tenant_id, name, code, parent_id, tree_path, sort, status, create_time)
VALUES (1, 1, 'Root Department', 'ROOT', 0, '/1/', 0, 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Initial data: Default tenant plan
INSERT INTO sys_tenant_plan (id, name, code, status, sort, create_time)
VALUES (1, 'Basic Plan', 'basic', 1, 0, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);
