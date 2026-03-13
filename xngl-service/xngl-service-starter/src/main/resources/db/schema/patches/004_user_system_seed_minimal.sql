-- 空库最小可用用户体系种子数据。
-- 仅在演示租户不存在时插入，避免覆盖已有业务数据。

INSERT INTO sys_tenant (
  tenant_code,
  tenant_name,
  tenant_type,
  status,
  contact_name,
  contact_mobile,
  remark
)
SELECT
  'demo',
  'Demo Tenant',
  'TENANT',
  'ENABLED',
  'System Admin',
  '13800000000',
  'Minimal bootstrap data for user system'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE tenant_code = 'demo');

INSERT INTO sys_org (
  tenant_id,
  code,
  name,
  org_code,
  org_name,
  parent_id,
  org_type,
  org_path,
  sort_order,
  status,
  deleted
)
SELECT
  t.id,
  'ROOT',
  'Root Org',
  'ROOT',
  'Root Org',
  0,
  'COMPANY',
  '/ROOT',
  0,
  'ENABLED',
  0
FROM sys_tenant t
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_org o WHERE o.tenant_id = t.id AND o.org_code = 'ROOT'
  );

INSERT INTO sys_role (
  tenant_id,
  code,
  name,
  role_code,
  role_name,
  role_scope,
  role_category,
  description,
  data_scope_type_default,
  status,
  builtin_flag,
  deleted
)
SELECT
  t.id,
  'TENANT_ADMIN',
  'Tenant Admin',
  'TENANT_ADMIN',
  'Tenant Admin',
  'TENANT',
  'BUILTIN',
  'Bootstrap admin role for demo tenant',
  'ALL',
  'ENABLED',
  1,
  0
FROM sys_tenant t
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.tenant_id = t.id AND r.role_code = 'TENANT_ADMIN'
  );

INSERT INTO sys_menu (
  tenant_id,
  tenant_scope,
  menu_code,
  menu_name,
  parent_id,
  menu_type,
  route_path,
  component_path,
  icon,
  sort_order,
  visible_flag,
  keep_alive_flag,
  hidden_flag,
  status,
  deleted
)
SELECT
  t.id,
  'TENANT',
  'dashboard',
  'Dashboard',
  0,
  'MENU',
  '/dashboard',
  'dashboard/index',
  'HomeFilled',
  10,
  1,
  1,
  0,
  'ENABLED',
  0
FROM sys_tenant t
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu m WHERE m.tenant_id = t.id AND m.menu_code = 'dashboard'
  );

INSERT INTO sys_permission (
  tenant_id,
  tenant_scope,
  menu_id,
  permission_code,
  permission_name,
  permission_type,
  module_code,
  resource_ref,
  http_method,
  api_path,
  status,
  deleted
)
SELECT
  t.id,
  'TENANT',
  m.id,
  'api:me:read',
  'View current user info',
  'API',
  'AUTH',
  'GET:/api/me',
  'GET',
  '/api/me',
  'ENABLED',
  0
FROM sys_tenant t
JOIN sys_menu m
  ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.tenant_id = t.id AND p.permission_code = 'api:me:read'
  );

INSERT INTO sys_permission (
  tenant_id,
  tenant_scope,
  menu_id,
  permission_code,
  permission_name,
  permission_type,
  module_code,
  resource_ref,
  status,
  deleted
)
SELECT
  t.id,
  'TENANT',
  m.id,
  'user:list',
  'View user list',
  'BUTTON',
  'USER',
  'users:list',
  'ENABLED',
  0
FROM sys_tenant t
JOIN sys_menu m
  ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.tenant_id = t.id AND p.permission_code = 'user:list'
  );

INSERT INTO sys_user (
  tenant_id,
  username,
  password,
  password_hash,
  name,
  mobile,
  email,
  user_type,
  main_org_id,
  status,
  auth_source,
  deleted
)
SELECT
  t.id,
  'demo',
  '$2a$10$wxMs/wSFp7NFCGIUSviwk.O5SNdNP6TIezqKNpBauOSOga9absNxi',
  '$2a$10$wxMs/wSFp7NFCGIUSviwk.O5SNdNP6TIezqKNpBauOSOga9absNxi',
  'Demo Admin',
  '13800000000',
  'demo@xngl.local',
  'TENANT_ADMIN',
  o.id,
  'ENABLED',
  'LOCAL',
  0
FROM sys_tenant t
JOIN sys_org o
  ON o.tenant_id = t.id AND o.org_code = 'ROOT'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user u WHERE u.tenant_id = t.id AND u.username = 'demo'
  );

INSERT INTO sys_user_org_rel (
  tenant_id,
  user_id,
  org_id,
  is_main
)
SELECT
  t.id,
  u.id,
  o.id,
  1
FROM sys_tenant t
JOIN sys_user u
  ON u.tenant_id = t.id AND u.username = 'demo'
JOIN sys_org o
  ON o.tenant_id = t.id AND o.org_code = 'ROOT'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_org_rel rel
    WHERE rel.tenant_id = t.id AND rel.user_id = u.id AND rel.org_id = o.id
  );

INSERT INTO sys_user_role_rel (
  tenant_id,
  user_id,
  role_id
)
SELECT
  t.id,
  u.id,
  r.id
FROM sys_tenant t
JOIN sys_user u
  ON u.tenant_id = t.id AND u.username = 'demo'
JOIN sys_role r
  ON r.tenant_id = t.id AND r.role_code = 'TENANT_ADMIN'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role_rel rel
    WHERE rel.tenant_id = t.id AND rel.user_id = u.id AND rel.role_id = r.id
  );

INSERT INTO sys_role_menu_rel (
  tenant_id,
  role_id,
  menu_id
)
SELECT
  t.id,
  r.id,
  m.id
FROM sys_tenant t
JOIN sys_role r
  ON r.tenant_id = t.id AND r.role_code = 'TENANT_ADMIN'
JOIN sys_menu m
  ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu_rel rel
    WHERE rel.tenant_id = t.id AND rel.role_id = r.id AND rel.menu_id = m.id
  );

INSERT INTO sys_role_permission_rel (
  tenant_id,
  role_id,
  permission_id
)
SELECT
  t.id,
  r.id,
  p.id
FROM sys_tenant t
JOIN sys_role r
  ON r.tenant_id = t.id AND r.role_code = 'TENANT_ADMIN'
JOIN sys_permission p
  ON p.tenant_id = t.id AND p.permission_code IN ('api:me:read', 'user:list')
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission_rel rel
    WHERE rel.tenant_id = t.id AND rel.role_id = r.id AND rel.permission_id = p.id
  );
