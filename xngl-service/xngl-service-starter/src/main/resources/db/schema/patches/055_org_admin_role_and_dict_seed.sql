-- 055_org_admin_role_and_dict_seed.sql
-- 1. 预置 ORG_ADMIN 角色
-- 2. 补全 ORG_TYPE 组织类型字典

-- ===================== 1. ORG_ADMIN 角色 =====================
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
  'ORG_ADMIN',
  '组织管理员',
  'ORG_ADMIN',
  '组织管理员',
  'ORG',
  'SYSTEM',
  '创建组织时自动生成的管理员角色，拥有该组织范围内的基础管理权限',
  'ORG',
  'ENABLED',
  1,
  0
FROM sys_tenant t
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.tenant_id = t.id AND r.role_code = 'ORG_ADMIN'
  );

-- 为 ORG_ADMIN 角色关联 dashboard 菜单（与 TENANT_ADMIN 一致的基础权限）
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
  ON r.tenant_id = t.id AND r.role_code = 'ORG_ADMIN'
JOIN sys_menu m
  ON m.tenant_id = t.id AND m.menu_code = 'dashboard'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu_rel rel
    WHERE rel.tenant_id = t.id AND rel.role_id = r.id AND rel.menu_id = m.id
  );

-- 为 ORG_ADMIN 角色关联基础 API 权限
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
  ON r.tenant_id = t.id AND r.role_code = 'ORG_ADMIN'
JOIN sys_permission p
  ON p.tenant_id = t.id AND p.permission_code IN ('api:me:read', 'user:list')
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission_rel rel
    WHERE rel.tenant_id = t.id AND rel.role_id = r.id AND rel.permission_id = p.id
  );

-- ===================== 2. ORG_TYPE 字典数据 =====================
INSERT INTO sys_data_dict (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT t.id, 'ORG_TYPE', 'DEPARTMENT', '部门', 'DEPARTMENT', 1, 'ENABLED', '内部组织部门'
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_data_dict d WHERE d.tenant_id = t.id AND d.dict_type = 'ORG_TYPE' AND d.dict_code = 'DEPARTMENT');

INSERT INTO sys_data_dict (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT t.id, 'ORG_TYPE', 'CONSTRUCTION_UNIT', '建设单位', 'CONSTRUCTION_UNIT', 10, 'ENABLED', '建设单位（业务单位）'
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_data_dict d WHERE d.tenant_id = t.id AND d.dict_type = 'ORG_TYPE' AND d.dict_code = 'CONSTRUCTION_UNIT');

INSERT INTO sys_data_dict (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT t.id, 'ORG_TYPE', 'BUILDER_UNIT', '施工单位', 'BUILDER_UNIT', 20, 'ENABLED', '施工单位（业务单位）'
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_data_dict d WHERE d.tenant_id = t.id AND d.dict_type = 'ORG_TYPE' AND d.dict_code = 'BUILDER_UNIT');

INSERT INTO sys_data_dict (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT t.id, 'ORG_TYPE', 'TRANSPORT_COMPANY', '运输单位', 'TRANSPORT_COMPANY', 30, 'ENABLED', '运输单位（业务单位）'
FROM sys_tenant t WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (SELECT 1 FROM sys_data_dict d WHERE d.tenant_id = t.id AND d.dict_type = 'ORG_TYPE' AND d.dict_code = 'TRANSPORT_COMPANY');
