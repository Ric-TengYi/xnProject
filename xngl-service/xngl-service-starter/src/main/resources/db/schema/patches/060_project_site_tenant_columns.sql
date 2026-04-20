SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_project' AND COLUMN_NAME = 'tenant_id'
  ),
  'SELECT 1',
  'ALTER TABLE biz_project ADD COLUMN tenant_id BIGINT NULL COMMENT ''租户ID'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'tenant_id'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN tenant_id BIGINT NULL COMMENT ''租户ID'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site_settlement' AND COLUMN_NAME = 'tenant_id'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site_settlement ADD COLUMN tenant_id BIGINT NULL COMMENT ''租户ID'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_project
SET tenant_id = 1
WHERE tenant_id IS NULL;

UPDATE biz_site
SET tenant_id = 1
WHERE tenant_id IS NULL;

UPDATE biz_site_settlement
SET tenant_id = 1
WHERE tenant_id IS NULL;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_project' AND INDEX_NAME = 'idx_biz_project_tenant_status'
  ),
  'SELECT 1',
  'CREATE INDEX idx_biz_project_tenant_status ON biz_project(tenant_id, status)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_project' AND INDEX_NAME = 'idx_biz_project_tenant_org'
  ),
  'SELECT 1',
  'CREATE INDEX idx_biz_project_tenant_org ON biz_project(tenant_id, org_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND INDEX_NAME = 'idx_biz_site_tenant_status'
  ),
  'SELECT 1',
  'CREATE INDEX idx_biz_site_tenant_status ON biz_site(tenant_id, status)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND INDEX_NAME = 'idx_biz_site_tenant_project'
  ),
  'SELECT 1',
  'CREATE INDEX idx_biz_site_tenant_project ON biz_site(tenant_id, project_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site_settlement' AND INDEX_NAME = 'idx_biz_site_settlement_tenant_site_status_date'
  ),
  'SELECT 1',
  'CREATE INDEX idx_biz_site_settlement_tenant_site_status_date ON biz_site_settlement(tenant_id, site_id, settlement_status, settlement_date)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site_settlement' AND INDEX_NAME = 'idx_biz_site_settlement_tenant_site_period'
  ),
  'SELECT 1',
  'CREATE INDEX idx_biz_site_settlement_tenant_site_period ON biz_site_settlement(tenant_id, site_id, period_start, period_end)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
