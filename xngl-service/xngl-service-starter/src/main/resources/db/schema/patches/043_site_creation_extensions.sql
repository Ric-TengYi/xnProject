SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'site_level'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN site_level VARCHAR(16) NOT NULL DEFAULT ''PRIMARY'' COMMENT ''场地层级: PRIMARY/SECONDARY'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'parent_site_id'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN parent_site_id BIGINT NULL COMMENT ''上级场地ID'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'management_area'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN management_area VARCHAR(64) NULL COMMENT ''所属区域'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'weighbridge_site_id'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN weighbridge_site_id BIGINT NULL COMMENT ''借用地磅场地ID'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND INDEX_NAME = 'idx_site_level_parent'
  ),
  'SELECT 1',
  'CREATE INDEX idx_site_level_parent ON biz_site(site_level, parent_site_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND INDEX_NAME = 'idx_site_weighbridge_site'
  ),
  'SELECT 1',
  'CREATE INDEX idx_site_weighbridge_site ON biz_site(weighbridge_site_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_site
SET site_level = 'PRIMARY'
WHERE deleted = 0
  AND (site_level IS NULL OR site_level = '');

UPDATE biz_site
SET management_area = CASE
    WHEN address LIKE '%滨海%' THEN '滨海新区'
    WHEN address LIKE '%南%' THEN '南部片区'
    WHEN address LIKE '%北%' THEN '北部片区'
    WHEN address LIKE '%西%' THEN '西部片区'
    ELSE '平台统筹'
  END
WHERE deleted = 0
  AND (management_area IS NULL OR management_area = '');
