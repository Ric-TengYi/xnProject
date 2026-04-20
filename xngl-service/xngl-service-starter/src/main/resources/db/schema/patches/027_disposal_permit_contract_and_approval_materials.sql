-- ============================================================
-- 027: 处置证合同关联 + 审批材料配置
-- ============================================================

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'disposal_permit'
        AND COLUMN_NAME = 'tenant_id'
    ),
    'SELECT 1',
    'ALTER TABLE disposal_permit ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id'
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
        AND TABLE_NAME = 'disposal_permit'
        AND COLUMN_NAME = 'contract_id'
    ),
    'SELECT 1',
    'ALTER TABLE disposal_permit ADD COLUMN contract_id BIGINT NULL AFTER project_id'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE disposal_permit
SET tenant_id = 1
WHERE tenant_id IS NULL OR tenant_id = 0;

UPDATE disposal_permit p
SET p.contract_id = (
  SELECT MIN(c.id)
  FROM biz_contract c
  WHERE c.deleted = 0
    AND c.project_id = p.project_id
    AND (p.site_id IS NULL OR c.site_id = p.site_id)
)
WHERE p.contract_id IS NULL;

CREATE TABLE IF NOT EXISTS sys_approval_material_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  process_key VARCHAR(64) NOT NULL,
  material_code VARCHAR(64) NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  material_type VARCHAR(64) NULL,
  required_flag INT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_approval_material_code (tenant_id, process_key, material_code),
  KEY idx_approval_material_process_status (tenant_id, process_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_approval_material_config
  (tenant_id, process_key, material_code, material_name, material_type, required_flag, sort_order, status, remark)
SELECT 1, 'CONTRACT_APPROVAL', 'APPLY_FORM', '合同申请表', 'PDF', 1, 10, 'ENABLED', '合同审批默认材料'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_approval_material_config
  WHERE tenant_id = 1 AND process_key = 'CONTRACT_APPROVAL' AND material_code = 'APPLY_FORM'
);

INSERT INTO sys_approval_material_config
  (tenant_id, process_key, material_code, material_name, material_type, required_flag, sort_order, status, remark)
SELECT 1, 'CONTRACT_APPROVAL', 'ID_COPY', '经办人身份证明', 'IMAGE', 1, 20, 'ENABLED', '合同审批默认材料'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_approval_material_config
  WHERE tenant_id = 1 AND process_key = 'CONTRACT_APPROVAL' AND material_code = 'ID_COPY'
);

INSERT INTO sys_approval_material_config
  (tenant_id, process_key, material_code, material_name, material_type, required_flag, sort_order, status, remark)
SELECT 1, 'SITE_SETTLEMENT', 'SETTLEMENT_ATTACHMENT', '结算附件', 'ZIP', 1, 10, 'ENABLED', '场地结算默认材料'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_approval_material_config
  WHERE tenant_id = 1 AND process_key = 'SITE_SETTLEMENT' AND material_code = 'SETTLEMENT_ATTACHMENT'
);
