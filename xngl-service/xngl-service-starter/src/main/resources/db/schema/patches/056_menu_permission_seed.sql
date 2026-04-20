-- ============================================================
-- 056_menu_permission_seed.sql
-- Add platform column to sys_menu and seed menu + permission data
-- ============================================================

-- 1. Add platform column (MySQL doesn't support IF NOT EXISTS for ADD COLUMN)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'platform');
SET @sql = IF(@col_exists = 0, "ALTER TABLE sys_menu ADD COLUMN platform VARCHAR(16) NOT NULL DEFAULT 'PC' COMMENT '平台类型: PC-PC端, MINI-小程序, SCREEN-大屏'", 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Get demo tenant id
SET @tenant_id = (SELECT id FROM sys_tenant WHERE tenant_code = 'demo' LIMIT 1);

-- ============================================================
-- PC Platform Menus
-- ============================================================

-- ---- 数据看板 (dashboard) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'dashboard', '数据看板', 0, 'DIR', NULL, 'DashboardOutlined', 1, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @dashboard_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'dashboard' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'dashboard_overview', '总体分析', @dashboard_id, 'MENU', '/', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'dashboard_sites', '消纳场数据', @dashboard_id, 'MENU', '/dashboard/sites', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'dashboard_projects', '项目数据', @dashboard_id, 'MENU', '/dashboard/projects', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'dashboard_capacity', '运力分析', @dashboard_id, 'MENU', '/dashboard/capacity-analysis', NULL, 4, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'dashboard_map', '地图展示', @dashboard_id, 'MENU', '/dashboard/map', NULL, 5, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 项目管理 (projects) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'projects', '项目管理', 0, 'DIR', NULL, 'ProjectOutlined', 2, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @projects_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'projects' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'projects_list', '项目清单', @projects_id, 'MENU', '/projects', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'projects_payments', '交款数据', @projects_id, 'MENU', '/projects/payments', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'projects_permits', '处置证清单', @projects_id, 'MENU', '/projects/permits', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'projects_daily_report', '项目日报', @projects_id, 'MENU', '/projects/daily-report', NULL, 4, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'projects_reports', '项目报表', @projects_id, 'MENU', '/projects/reports', NULL, 5, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 信息查询 (queries) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'queries', '信息查询', 0, 'DIR', NULL, 'SearchOutlined', 3, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @queries_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'queries' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'queries_checkins', '打卡数据', @queries_id, 'MENU', '/queries/checkins', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'queries_disposals', '消纳信息', @queries_id, 'MENU', '/queries/disposals', NULL, 2, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 消纳场地 (sites) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'sites', '消纳场地', 0, 'DIR', NULL, 'EnvironmentOutlined', 4, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @sites_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'sites' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'sites_list', '场地列表', @sites_id, 'MENU', '/sites', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'sites_disposals', '消纳清单', @sites_id, 'MENU', '/sites/disposals', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'sites_reports', '消纳报表', @sites_id, 'MENU', '/sites/reports', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'sites_documents', '场地资料', @sites_id, 'MENU', '/sites/documents', NULL, 4, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'sites_basic_info', '基础信息', @sites_id, 'MENU', '/sites/basic-info', NULL, 5, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 车辆与运力 (vehicles) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'vehicles', '车辆与运力', 0, 'DIR', NULL, 'CarOutlined', 5, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @vehicles_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'vehicles' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'vehicles_list', '车辆信息', @vehicles_id, 'MENU', '/vehicles', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_models', '车型管理', @vehicles_id, 'MENU', '/vehicles/models', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_fleet', '车队管理', @vehicles_id, 'MENU', '/vehicles/fleet', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_cards', '油电卡管理', @vehicles_id, 'MENU', '/vehicles/cards', NULL, 4, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_insurances', '保险管理', @vehicles_id, 'MENU', '/vehicles/insurances', NULL, 5, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_maintenance', '维保计划', @vehicles_id, 'MENU', '/vehicles/maintenance', NULL, 6, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_personnel', '人证管理', @vehicles_id, 'MENU', '/vehicles/personnel-certificates', NULL, 7, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_repairs', '维修管理', @vehicles_id, 'MENU', '/vehicles/repairs', NULL, 8, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_tracking', '送货跟踪', @vehicles_id, 'MENU', '/vehicles/tracking', NULL, 9, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'vehicles_violations', '违规车辆', @vehicles_id, 'MENU', '/vehicles/violations', NULL, 10, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 合同与结算 (contracts) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'contracts', '合同与结算', 0, 'DIR', NULL, 'FileTextOutlined', 6, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @contracts_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'contracts' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'contracts_list', '合同清单', @contracts_id, 'MENU', '/contracts', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'contracts_payments', '合同入账', @contracts_id, 'MENU', '/contracts/payments', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'contracts_transfers', '内拨申请', @contracts_id, 'MENU', '/contracts/transfers', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'contracts_settlements', '结算管理', @contracts_id, 'MENU', '/contracts/settlements', NULL, 4, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'contracts_monthly', '月报统计', @contracts_id, 'MENU', '/contracts/monthly-report', NULL, 5, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 预警与安全 (alerts) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'alerts', '预警与安全', 0, 'DIR', NULL, 'AlertOutlined', 7, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @alerts_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'alerts' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'alerts_monitor', '系统预警', @alerts_id, 'MENU', '/alerts', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'alerts_config', '预警配置', @alerts_id, 'MENU', '/alerts/config', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'alerts_events', '事件管理', @alerts_id, 'MENU', '/alerts/events', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'alerts_security', '安全台账', @alerts_id, 'MENU', '/alerts/security', NULL, 4, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 消息中心 (messages) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'messages', '消息中心', 0, 'DIR', NULL, 'BellOutlined', 8, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @messages_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'messages' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'messages_list', '消息管理', @messages_id, 'MENU', '/messages', NULL, 1, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ---- 系统设置 (settings) ----
INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'settings', '系统设置', 0, 'DIR', NULL, 'SettingOutlined', 9, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @settings_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'settings' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'settings_org', '组织管理', @settings_id, 'MENU', '/settings/org', NULL, 1, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_users', '用户管理', @settings_id, 'MENU', '/settings/users', NULL, 2, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_roles', '角色管理', @settings_id, 'MENU', '/settings/roles', NULL, 3, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_menus', '菜单管理', @settings_id, 'MENU', '/settings/menus', NULL, 4, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_dictionary', '数据字典', @settings_id, 'MENU', '/settings/dictionary', NULL, 5, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_approvals', '审批配置', @settings_id, 'MENU', '/settings/approvals', NULL, 6, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_system_params', '系统参数', @settings_id, 'MENU', '/settings/system-params', NULL, 7, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_platform', '平台对接', @settings_id, 'MENU', '/settings/platform-integrations', NULL, 8, 1, 'ENABLED', 'PC'),
  (@tenant_id, 'TENANT', 'settings_logs', '系统日志', @settings_id, 'MENU', '/settings/logs', NULL, 9, 1, 'ENABLED', 'PC')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), route_path = VALUES(route_path), platform = VALUES(platform);

-- ============================================================
-- MINI Platform Menus
-- ============================================================

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'mini', '小程序', 0, 'DIR', NULL, 'MobileOutlined', 10, 1, 'ENABLED', 'MINI')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @mini_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'mini' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'mini_home', '首页', @mini_id, 'MENU', NULL, NULL, 1, 1, 'ENABLED', 'MINI'),
  (@tenant_id, 'TENANT', 'mini_checkin', '打卡', @mini_id, 'MENU', NULL, NULL, 2, 1, 'ENABLED', 'MINI'),
  (@tenant_id, 'TENANT', 'mini_work_orders', '工单', @mini_id, 'MENU', NULL, NULL, 3, 1, 'ENABLED', 'MINI'),
  (@tenant_id, 'TENANT', 'mini_safety', '安全教育', @mini_id, 'MENU', NULL, NULL, 4, 1, 'ENABLED', 'MINI'),
  (@tenant_id, 'TENANT', 'mini_profile', '我的', @mini_id, 'MENU', NULL, NULL, 5, 1, 'ENABLED', 'MINI')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), platform = VALUES(platform);

-- ============================================================
-- SCREEN Platform Menus
-- ============================================================

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES (@tenant_id, 'TENANT', 'screen', '数据大屏', 0, 'DIR', NULL, 'DesktopOutlined', 11, 1, 'ENABLED', 'SCREEN')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), platform = VALUES(platform);
SET @screen_id = (SELECT id FROM sys_menu WHERE tenant_id = @tenant_id AND menu_code = 'screen' LIMIT 1);

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, icon, sort_order, visible_flag, status, platform)
VALUES
  (@tenant_id, 'TENANT', 'screen_capacity', '运力大屏', @screen_id, 'MENU', NULL, NULL, 1, 1, 'ENABLED', 'SCREEN'),
  (@tenant_id, 'TENANT', 'screen_project', '项目大屏', @screen_id, 'MENU', NULL, NULL, 2, 1, 'ENABLED', 'SCREEN'),
  (@tenant_id, 'TENANT', 'screen_site', '场地大屏', @screen_id, 'MENU', NULL, NULL, 3, 1, 'ENABLED', 'SCREEN')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort_order = VALUES(sort_order), parent_id = VALUES(parent_id), platform = VALUES(platform);

-- ============================================================
-- 3. Insert permissions for all MENU-type entries
-- ============================================================
-- For each MENU, insert 5 standard permissions: view, create, edit, delete, export

INSERT INTO sys_permission (tenant_id, tenant_scope, menu_id, permission_code, permission_name, permission_type, status)
SELECT m.tenant_id, 'TENANT', m.id, CONCAT(m.menu_code, ':view'), '查看', 'BUTTON', 'ENABLED'
FROM sys_menu m WHERE m.tenant_id = @tenant_id AND m.menu_type = 'MENU'
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_permission (tenant_id, tenant_scope, menu_id, permission_code, permission_name, permission_type, status)
SELECT m.tenant_id, 'TENANT', m.id, CONCAT(m.menu_code, ':create'), '新增', 'BUTTON', 'ENABLED'
FROM sys_menu m WHERE m.tenant_id = @tenant_id AND m.menu_type = 'MENU'
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_permission (tenant_id, tenant_scope, menu_id, permission_code, permission_name, permission_type, status)
SELECT m.tenant_id, 'TENANT', m.id, CONCAT(m.menu_code, ':edit'), '编辑', 'BUTTON', 'ENABLED'
FROM sys_menu m WHERE m.tenant_id = @tenant_id AND m.menu_type = 'MENU'
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_permission (tenant_id, tenant_scope, menu_id, permission_code, permission_name, permission_type, status)
SELECT m.tenant_id, 'TENANT', m.id, CONCAT(m.menu_code, ':delete'), '删除', 'BUTTON', 'ENABLED'
FROM sys_menu m WHERE m.tenant_id = @tenant_id AND m.menu_type = 'MENU'
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_permission (tenant_id, tenant_scope, menu_id, permission_code, permission_name, permission_type, status)
SELECT m.tenant_id, 'TENANT', m.id, CONCAT(m.menu_code, ':export'), '导出', 'BUTTON', 'ENABLED'
FROM sys_menu m WHERE m.tenant_id = @tenant_id AND m.menu_type = 'MENU'
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- ============================================================
-- 4. Grant all menus to SUPER_ADMIN and TENANT_ADMIN roles
-- ============================================================

SET @super_admin_role_id = (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND tenant_id = @tenant_id LIMIT 1);
SET @tenant_admin_role_id = (SELECT id FROM sys_role WHERE role_code = 'TENANT_ADMIN' AND tenant_id = @tenant_id LIMIT 1);

-- Grant menus to SUPER_ADMIN
INSERT IGNORE INTO sys_role_menu_rel (tenant_id, role_id, menu_id)
SELECT @tenant_id, @super_admin_role_id, m.id
FROM sys_menu m
WHERE m.tenant_id = @tenant_id AND @super_admin_role_id IS NOT NULL;

-- Grant menus to TENANT_ADMIN
INSERT IGNORE INTO sys_role_menu_rel (tenant_id, role_id, menu_id)
SELECT @tenant_id, @tenant_admin_role_id, m.id
FROM sys_menu m
WHERE m.tenant_id = @tenant_id AND @tenant_admin_role_id IS NOT NULL;

-- ============================================================
-- 5. Grant all permissions to SUPER_ADMIN and TENANT_ADMIN roles
-- ============================================================

-- Grant permissions to SUPER_ADMIN
INSERT IGNORE INTO sys_role_permission_rel (tenant_id, role_id, permission_id)
SELECT @tenant_id, @super_admin_role_id, p.id
FROM sys_permission p
WHERE p.tenant_id = @tenant_id AND @super_admin_role_id IS NOT NULL;

-- Grant permissions to TENANT_ADMIN
INSERT IGNORE INTO sys_role_permission_rel (tenant_id, role_id, permission_id)
SELECT @tenant_id, @tenant_admin_role_id, p.id
FROM sys_permission p
WHERE p.tenant_id = @tenant_id AND @tenant_admin_role_id IS NOT NULL;
