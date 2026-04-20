-- 057_org_max_role.sql
-- 组织最大权限角色字段

SET @db = DATABASE();

-- Add max_role_id column to sys_org
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_org' AND COLUMN_NAME = 'max_role_id') = 0,
  'ALTER TABLE sys_org ADD COLUMN max_role_id BIGINT NULL COMMENT ''组织最大权限角色ID''',
  'SELECT 1'
));
PREPARE _s FROM @stmt; EXECUTE _s; DEALLOCATE PREPARE _s;
