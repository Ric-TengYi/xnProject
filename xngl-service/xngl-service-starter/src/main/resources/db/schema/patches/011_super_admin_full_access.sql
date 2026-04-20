-- 超管角色 + admin 账号拥有全部菜单与按钮权限，用于初始化配置其他角色/用户。
-- 依赖：004/006（demo 租户、admin 用户）、010（完整菜单）。执行后 admin 登录可见全部菜单。

-- 1) 创建 SUPER_ADMIN 角色（demo 租户）
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
  'SUPER_ADMIN',
  '超管',
  'SUPER_ADMIN',
  '超管',
  'TENANT',
  'BUILTIN',
  '拥有全部菜单与按钮权限，用于初始化配置',
  'ALL',
  'ENABLED',
  1,
  0
FROM sys_tenant t
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.tenant_id = t.id AND r.role_code = 'SUPER_ADMIN'
  );

-- 2) 将 demo 租户下所有菜单授权给 SUPER_ADMIN（先删后插，保证与当前菜单一致）
DELETE rel FROM sys_role_menu_rel rel
JOIN sys_role r ON r.id = rel.role_id AND r.tenant_id = rel.tenant_id
JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_code = 'demo'
WHERE r.role_code = 'SUPER_ADMIN';

INSERT INTO sys_role_menu_rel (tenant_id, role_id, menu_id)
SELECT t.id, r.id, m.id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_code = 'SUPER_ADMIN'
JOIN sys_menu m ON m.tenant_id = t.id AND m.deleted = 0
WHERE t.tenant_code = 'demo';

-- 3) 将 demo 租户下所有权限（按钮+API）授权给 SUPER_ADMIN
DELETE rel FROM sys_role_permission_rel rel
JOIN sys_role r ON r.id = rel.role_id AND r.tenant_id = rel.tenant_id
JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_code = 'demo'
WHERE r.role_code = 'SUPER_ADMIN';

INSERT INTO sys_role_permission_rel (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_code = 'SUPER_ADMIN'
JOIN sys_permission p ON p.tenant_id = t.id AND p.deleted = 0
WHERE t.tenant_code = 'demo';

-- 4) admin 用户绑定 SUPER_ADMIN 角色
INSERT INTO sys_user_role_rel (tenant_id, user_id, role_id)
SELECT t.id, u.id, r.id
FROM sys_tenant t
JOIN sys_user u ON u.tenant_id = t.id AND u.username = 'admin'
JOIN sys_role r ON r.tenant_id = t.id AND r.role_code = 'SUPER_ADMIN'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role_rel rel
    WHERE rel.tenant_id = t.id AND rel.user_id = u.id AND rel.role_id = r.id
  );
