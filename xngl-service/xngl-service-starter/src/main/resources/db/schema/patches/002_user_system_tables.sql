-- 用户体系新表（01-er-ddl-design）：仅 CREATE TABLE IF NOT EXISTS，不修改已有 sys_org/sys_user/sys_role
-- 已有表新增字段（如 tenant_id、password_hash 等）需单独一次性迁移，见 docs/user-system/01-er-ddl-design.md

CREATE TABLE IF NOT EXISTS sys_tenant (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(128) NOT NULL,
  tenant_type VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_mobile VARCHAR(32) DEFAULT NULL,
  business_license_no VARCHAR(64) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  expire_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_tenant_code (tenant_code),
  KEY idx_sys_tenant_type_status (tenant_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_org_rel (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  org_id BIGINT UNSIGNED NOT NULL,
  is_main TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_org_rel (tenant_id, user_id, org_id),
  KEY idx_sys_user_org_rel_org (tenant_id, org_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role_rel (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_role_rel (tenant_id, user_id, role_id),
  KEY idx_sys_user_role_rel_role (tenant_id, role_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_scope VARCHAR(32) NOT NULL DEFAULT 'TENANT',
  menu_code VARCHAR(128) NOT NULL,
  menu_name VARCHAR(64) NOT NULL,
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU',
  route_path VARCHAR(255) DEFAULT NULL,
  component_path VARCHAR(255) DEFAULT NULL,
  icon VARCHAR(64) DEFAULT NULL,
  permission_code VARCHAR(128) DEFAULT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  visible_flag TINYINT(1) NOT NULL DEFAULT 1,
  keep_alive_flag TINYINT(1) NOT NULL DEFAULT 0,
  hidden_flag TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_menu_scope_code (tenant_scope, menu_code),
  KEY idx_sys_menu_scope_parent (tenant_scope, parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_scope VARCHAR(32) NOT NULL DEFAULT 'TENANT',
  permission_code VARCHAR(128) NOT NULL,
  permission_name VARCHAR(64) NOT NULL,
  permission_type VARCHAR(16) NOT NULL,
  module_code VARCHAR(64) DEFAULT NULL,
  resource_ref VARCHAR(128) DEFAULT NULL,
  http_method VARCHAR(16) DEFAULT NULL,
  api_path VARCHAR(255) DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_permission_scope_code (tenant_scope, permission_code),
  KEY idx_sys_permission_type_module (permission_type, module_code, status),
  KEY idx_sys_permission_api (http_method, api_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission_rel (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_role_permission_rel (tenant_id, role_id, permission_id),
  KEY idx_sys_role_permission_rel_perm (tenant_id, permission_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_data_scope_rule (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  biz_module VARCHAR(64) NOT NULL,
  scope_type VARCHAR(32) NOT NULL,
  scope_value JSON DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_data_scope_rule (tenant_id, role_id, biz_module),
  KEY idx_sys_data_scope_rule_type (tenant_id, scope_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_approval_actor_rule (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  actor_type VARCHAR(32) NOT NULL,
  actor_ref_id VARCHAR(64) DEFAULT NULL,
  match_mode VARCHAR(8) NOT NULL DEFAULT 'OR',
  priority INT NOT NULL DEFAULT 0,
  actor_snapshot_flag TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_sys_approval_actor_rule_biz_node (tenant_id, biz_type, node_code),
  KEY idx_sys_approval_actor_rule_type_ref (tenant_id, actor_type, actor_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED DEFAULT NULL,
  user_id BIGINT UNSIGNED DEFAULT NULL,
  username VARCHAR(64) NOT NULL,
  tenant_name_snapshot VARCHAR(128) DEFAULT NULL,
  login_type VARCHAR(16) NOT NULL DEFAULT 'ACCOUNT',
  success_flag TINYINT(1) NOT NULL DEFAULT 1,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(500) DEFAULT NULL,
  device_fingerprint VARCHAR(128) DEFAULT NULL,
  fail_reason VARCHAR(255) DEFAULT NULL,
  login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sys_login_log_tenant_time (tenant_id, login_time),
  KEY idx_sys_login_log_user_time (user_id, login_time),
  KEY idx_sys_login_log_username_time (username, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
