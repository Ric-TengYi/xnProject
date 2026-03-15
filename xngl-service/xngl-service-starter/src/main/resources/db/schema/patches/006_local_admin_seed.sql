-- Ensure a local bootstrap admin account admin/admin exists under the demo tenant.

UPDATE sys_user u
JOIN sys_tenant t
  ON t.id = u.tenant_id
 AND t.tenant_code = 'demo'
JOIN sys_org o
  ON o.tenant_id = t.id
 AND o.org_code = 'ROOT'
SET u.password = '$2a$10$S7DTsiaBHo4RvAZGydIGzuxQbmsPwM5py6OVoUxJC6guEErhVA9WG',
    u.password_hash = '$2a$10$S7DTsiaBHo4RvAZGydIGzuxQbmsPwM5py6OVoUxJC6guEErhVA9WG',
    u.name = 'Local Admin',
    u.mobile = '13800000001',
    u.email = 'admin@xngl.local',
    u.user_type = 'TENANT_ADMIN',
    u.main_org_id = o.id,
    u.status = 'ENABLED',
    u.auth_source = 'LOCAL',
    u.deleted = 0
WHERE u.username = 'admin';

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
  'admin',
  '$2a$10$S7DTsiaBHo4RvAZGydIGzuxQbmsPwM5py6OVoUxJC6guEErhVA9WG',
  '$2a$10$S7DTsiaBHo4RvAZGydIGzuxQbmsPwM5py6OVoUxJC6guEErhVA9WG',
  'Local Admin',
  '13800000001',
  'admin@xngl.local',
  'TENANT_ADMIN',
  o.id,
  'ENABLED',
  'LOCAL',
  0
FROM sys_tenant t
JOIN sys_org o
  ON o.tenant_id = t.id
 AND o.org_code = 'ROOT'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user u WHERE u.tenant_id = t.id AND u.username = 'admin'
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
  ON u.tenant_id = t.id
 AND u.username = 'admin'
JOIN sys_org o
  ON o.tenant_id = t.id
 AND o.org_code = 'ROOT'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_user_org_rel rel
    WHERE rel.tenant_id = t.id
      AND rel.user_id = u.id
      AND rel.org_id = o.id
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
  ON u.tenant_id = t.id
 AND u.username = 'admin'
JOIN sys_role r
  ON r.tenant_id = t.id
 AND r.role_code = 'TENANT_ADMIN'
WHERE t.tenant_code = 'demo'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_user_role_rel rel
    WHERE rel.tenant_id = t.id
      AND rel.user_id = u.id
      AND rel.role_id = r.id
  );
