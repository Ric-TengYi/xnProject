-- ============================================================
-- 013: 合同结算模块迭代二 - 增强功能表结构
-- ============================================================

-- 1. 合同审批记录表
CREATE TABLE IF NOT EXISTS biz_contract_approval_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  action_type VARCHAR(32) NOT NULL COMMENT '操作类型: SUBMIT/APPROVE/REJECT/CANCEL',
  operator_id BIGINT NOT NULL COMMENT '操作人ID',
  from_status VARCHAR(32) NULL COMMENT '原状态',
  to_status VARCHAR(32) NULL COMMENT '目标状态',
  remark VARCHAR(500) NULL COMMENT '备注',
  operate_time DATETIME NOT NULL COMMENT '操作时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_approval_contract (tenant_id, contract_id, operate_time),
  KEY idx_approval_operator (tenant_id, operator_id, operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审批记录';

-- 2. 合同办事材料表
CREATE TABLE IF NOT EXISTS biz_contract_material (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  material_name VARCHAR(200) NOT NULL COMMENT '材料名称',
  material_type VARCHAR(64) NULL COMMENT '材料类型',
  file_url VARCHAR(500) NULL COMMENT '文件URL',
  file_size BIGINT NULL COMMENT '文件大小',
  uploader_id BIGINT NULL COMMENT '上传人ID',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_material_contract (tenant_id, contract_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同办事材料';

-- 3. 合同发票表
CREATE TABLE IF NOT EXISTS biz_contract_invoice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  invoice_no VARCHAR(64) NOT NULL COMMENT '发票号码',
  invoice_type VARCHAR(32) NULL COMMENT '发票类型: NORMAL/SPECIAL',
  invoice_date DATE NULL COMMENT '开票日期',
  amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
  tax_rate DECIMAL(5,2) NULL COMMENT '税率',
  tax_amount DECIMAL(18,2) NULL COMMENT '税额',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_invoice_no (tenant_id, invoice_no),
  KEY idx_invoice_contract (tenant_id, contract_id, invoice_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同发票';

-- 4. 合同领票记录表
CREATE TABLE IF NOT EXISTS biz_contract_ticket (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  ticket_no VARCHAR(64) NOT NULL COMMENT '票据号码',
  ticket_type VARCHAR(32) NULL COMMENT '票据类型',
  ticket_date DATE NULL COMMENT '领票日期',
  amount DECIMAL(18,2) NULL COMMENT '金额',
  volume DECIMAL(18,2) NULL COMMENT '方量',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  remark VARCHAR(500) NULL COMMENT '备注',
  creator_id BIGINT NULL COMMENT '创建人ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_ticket_contract (tenant_id, contract_id, ticket_date),
  KEY idx_ticket_no (tenant_id, ticket_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同领票记录';

-- 5. 合同入账表增强 - 添加分次入账字段
ALTER TABLE biz_contract_receipt
  ADD COLUMN IF NOT EXISTS installment_no INT NULL COMMENT '分次入账序号',
  ADD COLUMN IF NOT EXISTS installment_total INT NULL COMMENT '分次入账总数';

-- 6. 合同变更申请表增强 - 添加场地变更、方量变更字段
ALTER TABLE biz_contract_change_apply
  ADD COLUMN IF NOT EXISTS original_site_id BIGINT NULL COMMENT '原场地ID',
  ADD COLUMN IF NOT EXISTS new_site_id BIGINT NULL COMMENT '新场地ID',
  ADD COLUMN IF NOT EXISTS original_volume DECIMAL(18,2) NULL COMMENT '原方量',
  ADD COLUMN IF NOT EXISTS new_volume DECIMAL(18,2) NULL COMMENT '新方量',
  ADD COLUMN IF NOT EXISTS volume_delta DECIMAL(18,2) NULL COMMENT '方量变更值',
  ADD COLUMN IF NOT EXISTS original_amount DECIMAL(18,2) NULL COMMENT '原金额',
  ADD COLUMN IF NOT EXISTS new_amount DECIMAL(18,2) NULL COMMENT '新金额',
  ADD COLUMN IF NOT EXISTS original_unit_price DECIMAL(18,2) NULL COMMENT '原单价',
  ADD COLUMN IF NOT EXISTS new_unit_price DECIMAL(18,2) NULL COMMENT '新单价',
  ADD COLUMN IF NOT EXISTS original_expire_date DATE NULL COMMENT '原到期日期',
  ADD COLUMN IF NOT EXISTS new_expire_date DATE NULL COMMENT '新到期日期';

-- 7. 索引优化
CREATE INDEX IF NOT EXISTS idx_contract_approval_status ON biz_contract(tenant_id, approval_status, contract_status);
CREATE INDEX IF NOT EXISTS idx_contract_three_party ON biz_contract(tenant_id, is_three_party, contract_status);
CREATE INDEX IF NOT EXISTS idx_contract_effective_date ON biz_contract(tenant_id, effective_date);
CREATE INDEX IF NOT EXISTS idx_contract_expire_date ON biz_contract(tenant_id, expire_date);