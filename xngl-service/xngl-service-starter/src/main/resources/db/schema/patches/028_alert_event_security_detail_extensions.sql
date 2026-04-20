SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'report_address'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN report_address VARCHAR(255) NULL AFTER source_channel'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'contact_phone'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN contact_phone VARCHAR(64) NULL AFTER reporter_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'deadline_time'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN deadline_time DATETIME NULL AFTER occur_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'attachment_urls'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN attachment_urls VARCHAR(2000) NULL AFTER close_remark'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'assignee_name'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN assignee_name VARCHAR(64) NULL AFTER attachment_urls'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'assignee_phone'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN assignee_phone VARCHAR(64) NULL AFTER assignee_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_manual_event'
      AND COLUMN_NAME = 'dispatch_remark'
  ),
  'SELECT 1',
  'ALTER TABLE biz_manual_event ADD COLUMN dispatch_remark VARCHAR(500) NULL AFTER assignee_phone'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_security_inspection'
      AND COLUMN_NAME = 'danger_level'
  ),
  'SELECT 1',
  'ALTER TABLE biz_security_inspection ADD COLUMN danger_level VARCHAR(32) NULL AFTER result_level'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_security_inspection'
      AND COLUMN_NAME = 'hazard_category'
  ),
  'SELECT 1',
  'ALTER TABLE biz_security_inspection ADD COLUMN hazard_category VARCHAR(64) NULL AFTER check_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_security_inspection'
      AND COLUMN_NAME = 'rectify_owner'
  ),
  'SELECT 1',
  'ALTER TABLE biz_security_inspection ADD COLUMN rectify_owner VARCHAR(64) NULL AFTER inspector_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_security_inspection'
      AND COLUMN_NAME = 'rectify_owner_phone'
  ),
  'SELECT 1',
  'ALTER TABLE biz_security_inspection ADD COLUMN rectify_owner_phone VARCHAR(64) NULL AFTER rectify_owner'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_security_inspection'
      AND COLUMN_NAME = 'attachment_urls'
  ),
  'SELECT 1',
  'ALTER TABLE biz_security_inspection ADD COLUMN attachment_urls VARCHAR(2000) NULL AFTER description'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_security_inspection'
      AND COLUMN_NAME = 'estimated_cost'
  ),
  'SELECT 1',
  'ALTER TABLE biz_security_inspection ADD COLUMN estimated_cost DECIMAL(12,2) NULL DEFAULT 0 AFTER attachment_urls'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_manual_event
SET report_address = COALESCE(report_address, '杭州市余杭区运溪高架施工现场'),
    contact_phone = COALESCE(contact_phone, '13800001111'),
    deadline_time = COALESCE(deadline_time, DATE_ADD(report_time, INTERVAL 24 HOUR)),
    attachment_urls = COALESCE(attachment_urls, 'https://example.local/event/notice.jpg'),
    assignee_name = COALESCE(assignee_name, '安全员张伟'),
    assignee_phone = COALESCE(assignee_phone, '13800002222'),
    dispatch_remark = COALESCE(dispatch_remark, '请在时限内完成核查并反馈处置结果')
WHERE tenant_id = 1;

UPDATE biz_security_inspection
SET danger_level = COALESCE(danger_level, CASE WHEN issue_count > 2 THEN 'HIGH' WHEN issue_count > 0 THEN 'MEDIUM' ELSE 'LOW' END),
    hazard_category = COALESCE(hazard_category, CASE WHEN object_type = 'SITE' THEN 'FIRE' WHEN object_type = 'VEHICLE' THEN 'OPERATION' ELSE 'PERSONNEL' END),
    rectify_owner = COALESCE(rectify_owner, '整改责任人'),
    rectify_owner_phone = COALESCE(rectify_owner_phone, '13800003333'),
    attachment_urls = COALESCE(attachment_urls, 'https://example.local/security/check.jpg'),
    estimated_cost = COALESCE(estimated_cost, CASE WHEN issue_count > 0 THEN 500 ELSE 0 END)
WHERE tenant_id = 1;
