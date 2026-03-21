-- ============================================================
-- 030: 审批流程配置 + 预警自动生成支持
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_approval_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  process_key VARCHAR(64) NOT NULL,
  config_name VARCHAR(128) NOT NULL,
  approval_type VARCHAR(32) NOT NULL DEFAULT 'SERIAL',
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(128) NOT NULL,
  approvers VARCHAR(500) NULL,
  condition_expr VARCHAR(1000) NULL,
  form_template_code VARCHAR(64) NULL,
  timeout_hours INT NOT NULL DEFAULT 24,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_approval_config_node (tenant_id, process_key, node_code),
  KEY idx_sys_approval_config_status (tenant_id, process_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_approval_config
  (tenant_id, process_key, config_name, approval_type, node_code, node_name, approvers, condition_expr, form_template_code, timeout_hours, sort_order, status, remark)
SELECT
  1,
  'CONTRACT_APPROVAL',
  '合同审批标准流',
  'SERIAL',
  'APPLY_AUDIT',
  '申请审核',
  'role:project_manager',
  'amount<=500000',
  'CONTRACT_APPLY_DOC',
  24,
  10,
  'ENABLED',
  '合同审批默认节点'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_approval_config
  WHERE tenant_id = 1 AND process_key = 'CONTRACT_APPROVAL' AND node_code = 'APPLY_AUDIT'
);

INSERT INTO sys_approval_config
  (tenant_id, process_key, config_name, approval_type, node_code, node_name, approvers, condition_expr, form_template_code, timeout_hours, sort_order, status, remark)
SELECT
  1,
  'CONTRACT_APPROVAL',
  '合同审批标准流',
  'PARALLEL',
  'FINANCE_AUDIT',
  '财务复核',
  'role:finance,role:admin',
  'amount>500000',
  'CONTRACT_APPLY_DOC',
  48,
  20,
  'ENABLED',
  '大额合同自动进入财务复核'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_approval_config
  WHERE tenant_id = 1 AND process_key = 'CONTRACT_APPROVAL' AND node_code = 'FINANCE_AUDIT'
);

INSERT INTO sys_approval_config
  (tenant_id, process_key, config_name, approval_type, node_code, node_name, approvers, condition_expr, form_template_code, timeout_hours, sort_order, status, remark)
SELECT
  1,
  'SITE_SETTLEMENT',
  '场地结算审批流',
  'SERIAL',
  'SETTLEMENT_REVIEW',
  '结算复核',
  'role:settlement_manager',
  'siteType in (STATE, COLLECTIVE)',
  'SETTLEMENT_ATTACHMENT',
  24,
  10,
  'ENABLED',
  '场地结算默认节点'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_approval_config
  WHERE tenant_id = 1 AND process_key = 'SITE_SETTLEMENT' AND node_code = 'SETTLEMENT_REVIEW'
);
