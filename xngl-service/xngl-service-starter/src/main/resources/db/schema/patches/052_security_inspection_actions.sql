CREATE TABLE IF NOT EXISTS biz_security_inspection_action (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  inspection_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  action_label VARCHAR(64) NOT NULL,
  before_status VARCHAR(32) NULL,
  after_status VARCHAR(32) NULL,
  before_result_level VARCHAR(32) NULL,
  after_result_level VARCHAR(32) NULL,
  action_remark VARCHAR(1000) NULL,
  next_check_time DATETIME NULL,
  actor_id BIGINT NULL,
  actor_name VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_security_inspection_action_inspection (tenant_id, inspection_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO biz_security_inspection_action
  (tenant_id, inspection_id, action_type, action_label, after_status, after_result_level, action_remark, next_check_time, actor_id, actor_name, create_time, update_time, deleted)
SELECT
  i.tenant_id,
  i.id,
  'CREATE',
  '创建检查',
  i.status,
  i.result_level,
  CONCAT('历史补录：', IFNULL(i.title, '安全检查')),
  i.next_check_time,
  i.inspector_id,
  i.inspector_name,
  i.create_time,
  i.update_time,
  0
FROM biz_security_inspection i
WHERE NOT EXISTS (
  SELECT 1
  FROM biz_security_inspection_action a
  WHERE a.tenant_id = i.tenant_id
    AND a.inspection_id = i.id
    AND a.action_type = 'CREATE'
    AND a.deleted = 0
);
