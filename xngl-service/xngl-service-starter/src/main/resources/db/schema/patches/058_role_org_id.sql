-- 058: 角色增加组织归属字段
-- 角色按 org_id 隔离，组织只能看到自身+上级链创建的角色

ALTER TABLE sys_role ADD COLUMN org_id BIGINT NULL COMMENT '角色所属组织ID' AFTER tenant_id;

-- 存量角色归属到 Root Org (id=1)
UPDATE sys_role SET org_id = 1 WHERE org_id IS NULL AND tenant_id = 1;

-- 索引
CREATE INDEX idx_role_org_id ON sys_role(org_id);
