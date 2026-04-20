-- 为 demo 租户补充完整菜单（与前端侧栏一致），并关联 TENANT_ADMIN，便于 /me/menus 返回完整树做权限控制。
-- 仅当对应 menu_code 不存在时插入，避免重复执行冲突。

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'dashboard_home', '总体分析', m.id, 'MENU', '/', 1, 1, 'ENABLED', 0
FROM sys_tenant t
JOIN sys_menu m ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'dashboard_home');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'dashboard_sites', '消纳场数据', m.id, 'MENU', '/dashboard/sites', 2, 1, 'ENABLED', 0
FROM sys_tenant t JOIN sys_menu m ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'dashboard_sites');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'dashboard_projects', '项目数据', m.id, 'MENU', '/dashboard/projects', 3, 1, 'ENABLED', 0
FROM sys_tenant t JOIN sys_menu m ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'dashboard_projects');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'dashboard_map', '地图展示', m.id, 'MENU', '/dashboard/map', 4, 1, 'ENABLED', 0
FROM sys_tenant t JOIN sys_menu m ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'dashboard_map');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'projects', '项目管理', 0, 'MENU', '/projects', 20, 1, 'ENABLED', 0
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'projects');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'sites', '消纳场地', 0, 'MENU', '/sites', 30, 1, 'ENABLED', 0
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'sites');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'vehicles', '车辆与运力', 0, 'MENU', '/vehicles', 40, 1, 'ENABLED', 0
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'vehicles');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'contracts', '合同与结算', 0, 'MENU', '/contracts', 50, 1, 'ENABLED', 0
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'contracts');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'alerts', '预警与安全', 0, 'MENU', '/alerts', 60, 1, 'ENABLED', 0
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'alerts');

INSERT INTO sys_menu (tenant_id, tenant_scope, menu_code, menu_name, parent_id, menu_type, route_path, sort_order, visible_flag, status, deleted)
SELECT t.id, 'TENANT', 'settings', '系统设置', 0, 'MENU', '/settings/organization', 70, 1, 'ENABLED', 0
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.tenant_id = t.id AND x.menu_code = 'settings');

-- 将新增菜单授权给 TENANT_ADMIN（仅插入尚未存在的关联）
INSERT INTO sys_role_menu_rel (tenant_id, role_id, menu_id)
SELECT t.id, r.id, m.id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_code = 'TENANT_ADMIN'
JOIN sys_menu m ON m.tenant_id = t.id AND m.menu_code IN ('dashboard_home','dashboard_sites','dashboard_projects','dashboard_map','projects','sites','vehicles','contracts','alerts','settings')
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu_rel rel WHERE rel.tenant_id = t.id AND rel.role_id = r.id AND rel.menu_id = m.id);
