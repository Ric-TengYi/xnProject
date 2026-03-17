-- 司机端角色：平台在「组织与人员」中可为人员分配「司机」角色，该账号在小程序登录后仅展示司机工作台。
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
  'DRIVER',
  '司机',
  'DRIVER',
  '司机',
  'TENANT',
  'BUILTIN',
  '司机端账号，小程序登录后仅具备司机工作台（任务、预警、处置证、打卡异常、问题反馈）',
  'SELF',
  'ENABLED',
  1,
  0
FROM sys_tenant t
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.tenant_id = t.id AND r.role_code = 'DRIVER'
  );
