-- 添加 organization_id 到 sys_user_role_rel 表，支持用户在不同组织拥有不同角色

ALTER TABLE sys_user_role_rel ADD COLUMN organization_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户在该组织拥有的角色' AFTER role_id;

-- 更新唯一键以包含 organization_id
ALTER TABLE sys_user_role_rel DROP KEY uk_sys_user_role_rel;
ALTER TABLE sys_user_role_rel ADD UNIQUE KEY uk_sys_user_role_rel (tenant_id, user_id, role_id, organization_id);

-- 添加索引以支持按组织查询
ALTER TABLE sys_user_role_rel ADD KEY idx_sys_user_role_rel_org (tenant_id, organization_id, user_id);
