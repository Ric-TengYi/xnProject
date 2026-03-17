-- 操作日志表补齐 tenant_id，供按租户查询使用。
ALTER TABLE sys_operation_log ADD COLUMN tenant_id BIGINT DEFAULT NULL;
