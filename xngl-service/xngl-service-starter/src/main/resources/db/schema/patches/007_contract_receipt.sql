-- 合同主表补字段：为合同入账回写累计入账金额和状态预留字段
ALTER TABLE biz_contract
  ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID';

ALTER TABLE biz_contract
  ADD COLUMN received_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计已入账金额';

ALTER TABLE biz_contract
  ADD COLUMN approval_status VARCHAR(32) NULL COMMENT '审批状态';

ALTER TABLE biz_contract
  ADD COLUMN contract_status VARCHAR(32) NULL COMMENT '合同状态';

-- 合同入账流水
CREATE TABLE IF NOT EXISTS biz_contract_receipt (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '租户ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  receipt_no VARCHAR(64) NOT NULL COMMENT '入账流水号',
  receipt_date DATE NOT NULL COMMENT '入账日期',
  amount DECIMAL(18, 2) NOT NULL COMMENT '入账金额，冲销流水为负数',
  receipt_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '流水类型',
  voucher_no VARCHAR(64) NULL COMMENT '凭证号',
  bank_flow_no VARCHAR(64) NULL COMMENT '银行流水号',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  operator_id BIGINT NULL COMMENT '操作人',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_receipt_no (tenant_id, receipt_no),
  KEY idx_receipt_contract_date (tenant_id, contract_id, receipt_date),
  KEY idx_receipt_status_date (tenant_id, status, receipt_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同入账流水';

-- 入账附件（为后续凭证上传预留）
CREATE TABLE IF NOT EXISTS biz_contract_attachment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '租户ID',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
  biz_id BIGINT NOT NULL COMMENT '业务ID',
  file_name VARCHAR(200) NOT NULL COMMENT '文件名',
  file_url VARCHAR(500) NOT NULL COMMENT '文件地址',
  file_size BIGINT NULL COMMENT '文件大小',
  mime_type VARCHAR(100) NULL COMMENT '文件类型',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_contract_attachment_biz (tenant_id, biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同业务附件';
