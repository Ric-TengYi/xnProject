SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'diagnosis_result'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN diagnosis_result VARCHAR(1000) NULL AFTER repair_content'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'safety_impact'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN safety_impact VARCHAR(255) NULL AFTER diagnosis_result'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'parts_cost'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN parts_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER actual_amount'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'labor_cost'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN labor_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER parts_cost'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'other_cost'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN other_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER labor_cost'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'repair_manager'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN repair_manager VARCHAR(64) NULL AFTER vendor_name'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'technician_name'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN technician_name VARCHAR(64) NULL AFTER repair_manager'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'acceptance_result'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN acceptance_result VARCHAR(1000) NULL AFTER technician_name'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'signoff_status'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN signoff_status VARCHAR(32) NOT NULL DEFAULT ''UNSIGNED'' AFTER acceptance_result'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'biz_vehicle_repair_order'
        AND COLUMN_NAME = 'attachment_urls'
    ),
    'SELECT 1',
    'ALTER TABLE biz_vehicle_repair_order ADD COLUMN attachment_urls VARCHAR(2000) NULL AFTER signoff_status'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_vehicle_repair_order
SET diagnosis_result = COALESCE(diagnosis_result, repair_content),
    safety_impact = COALESCE(safety_impact, '中'),
    parts_cost = CASE WHEN parts_cost = 0 AND actual_amount IS NOT NULL THEN ROUND(actual_amount * 0.55, 2) ELSE parts_cost END,
    labor_cost = CASE WHEN labor_cost = 0 AND actual_amount IS NOT NULL THEN ROUND(actual_amount * 0.35, 2) ELSE labor_cost END,
    other_cost = CASE WHEN other_cost = 0 AND actual_amount IS NOT NULL THEN actual_amount - ROUND(actual_amount * 0.55, 2) - ROUND(actual_amount * 0.35, 2) ELSE other_cost END,
    repair_manager = COALESCE(repair_manager, applicant_name),
    technician_name = COALESCE(technician_name, approved_by),
    acceptance_result = COALESCE(acceptance_result, remark),
    signoff_status = CASE WHEN signoff_status IS NULL OR signoff_status = '' THEN IF(status = 'COMPLETED', 'SIGNED', 'UNSIGNED') ELSE signoff_status END
WHERE tenant_id = 1;
