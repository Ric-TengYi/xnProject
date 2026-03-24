SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'labor_cost'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN labor_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER cost_amount'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'material_cost'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN material_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER labor_cost'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'external_cost'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN external_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER material_cost'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'issue_description'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN issue_description VARCHAR(1000) NULL AFTER items'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'result_summary'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN result_summary VARCHAR(1000) NULL AFTER issue_description'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'technician_name'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN technician_name VARCHAR(64) NULL AFTER operator_name'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'checker_name'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN checker_name VARCHAR(64) NULL AFTER technician_name'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'signoff_status'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN signoff_status VARCHAR(32) NOT NULL DEFAULT ''UNSIGNED'' AFTER checker_name'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_maintenance_record'
        AND COLUMN_NAME = 'attachment_urls'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_maintenance_record ADD COLUMN attachment_urls VARCHAR(2000) NULL AFTER signoff_status'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_vehicle_maintenance_record
SET labor_cost = CASE WHEN labor_cost = 0 THEN ROUND(cost_amount * 0.35, 2) ELSE labor_cost END,
    material_cost = CASE WHEN material_cost = 0 THEN ROUND(cost_amount * 0.55, 2) ELSE material_cost END,
    external_cost = CASE WHEN external_cost = 0 THEN cost_amount - ROUND(cost_amount * 0.35, 2) - ROUND(cost_amount * 0.55, 2) ELSE external_cost END,
    issue_description = COALESCE(issue_description, '例行维保检查，未发现重大异常'),
    result_summary = COALESCE(result_summary, '完成计划维保项目并恢复车辆正常运营状态'),
    technician_name = COALESCE(technician_name, operator_name),
    checker_name = COALESCE(checker_name, operator_name),
    signoff_status = CASE WHEN signoff_status IS NULL OR signoff_status = '' THEN 'SIGNED' ELSE signoff_status END
WHERE tenant_id = 1;
