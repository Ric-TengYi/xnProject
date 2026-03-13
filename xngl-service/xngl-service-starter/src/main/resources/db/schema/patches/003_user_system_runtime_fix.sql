-- 兼容旧 baseline 表结构，补齐用户体系运行所需字段。

CREATE TABLE IF NOT EXISTS sys_role_menu_rel (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  menu_id BIGINT UNSIGNED NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_role_menu_rel (tenant_id, role_id, menu_id),
  KEY idx_sys_role_menu_rel_menu (tenant_id, menu_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE sys_org ADD COLUMN tenant_id BIGINT DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN org_code VARCHAR(64) DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN org_name VARCHAR(128) DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN org_type VARCHAR(32) DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN org_path VARCHAR(500) DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN leader_user_id BIGINT DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN leader_name_cache VARCHAR(128) DEFAULT NULL;
ALTER TABLE sys_org ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ENABLED';
UPDATE sys_org
SET org_code = COALESCE(org_code, code),
    org_name = COALESCE(org_name, name),
    org_type = COALESCE(org_type, 'DEPARTMENT'),
    status = COALESCE(status, 'ENABLED');

ALTER TABLE sys_role ADD COLUMN tenant_id BIGINT DEFAULT NULL;
ALTER TABLE sys_role ADD COLUMN role_code VARCHAR(64) DEFAULT NULL;
ALTER TABLE sys_role ADD COLUMN role_name VARCHAR(128) DEFAULT NULL;
ALTER TABLE sys_role ADD COLUMN role_scope VARCHAR(32) DEFAULT NULL;
ALTER TABLE sys_role ADD COLUMN role_category VARCHAR(32) DEFAULT NULL;
ALTER TABLE sys_role ADD COLUMN data_scope_type_default VARCHAR(32) DEFAULT NULL;
ALTER TABLE sys_role ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ENABLED';
ALTER TABLE sys_role ADD COLUMN builtin_flag TINYINT(1) NOT NULL DEFAULT 0;
UPDATE sys_role
SET role_code = COALESCE(role_code, code),
    role_name = COALESCE(role_name, name),
    role_scope = COALESCE(role_scope, 'TENANT'),
    role_category = COALESCE(role_category, 'CUSTOM'),
    data_scope_type_default = COALESCE(data_scope_type_default, 'SELF'),
    status = COALESCE(status, 'ENABLED'),
    builtin_flag = COALESCE(builtin_flag, 0);

ALTER TABLE sys_user ADD COLUMN tenant_id BIGINT DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN password_hash VARCHAR(255) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN email VARCHAR(128) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(255) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN id_card_mask VARCHAR(64) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN user_type VARCHAR(32) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN main_org_id BIGINT DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN password_expire_time DATETIME DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN need_reset_password TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN lock_status TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN lock_reason VARCHAR(255) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN auth_source VARCHAR(32) DEFAULT NULL;
ALTER TABLE sys_user ADD COLUMN external_user_id VARCHAR(128) DEFAULT NULL;
ALTER TABLE sys_user MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ENABLED';
UPDATE sys_user
SET password_hash = COALESCE(password_hash, password),
    user_type = COALESCE(user_type, 'TENANT_USER'),
    main_org_id = COALESCE(main_org_id, org_id),
    auth_source = COALESCE(auth_source, 'LOCAL'),
    status = CASE
        WHEN status IN ('0', 'DISABLED') THEN 'DISABLED'
        ELSE 'ENABLED'
    END;

ALTER TABLE sys_menu ADD COLUMN tenant_id BIGINT DEFAULT NULL;
UPDATE sys_menu
SET tenant_id = COALESCE(tenant_id, 0);

ALTER TABLE sys_permission ADD COLUMN tenant_id BIGINT DEFAULT NULL;
ALTER TABLE sys_permission ADD COLUMN menu_id BIGINT DEFAULT NULL;
UPDATE sys_permission
SET tenant_id = COALESCE(tenant_id, 0);
