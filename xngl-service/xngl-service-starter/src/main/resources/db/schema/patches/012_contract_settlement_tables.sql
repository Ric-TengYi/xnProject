-- ============================================================
-- 012: 合同结算模块迭代一 - 表结构升级与新建
-- ============================================================

-- 1. biz_contract 补字段
ALTER TABLE biz_contract
  ADD COLUMN IF NOT EXISTS contract_no VARCHAR(64) NULL COMMENT '合同编号',
  ADD COLUMN IF NOT EXISTS contract_type VARCHAR(32) NULL COMMENT '合同类型: DISPOSAL/VEHICLE_LEASE/LABOR/OTHER',
  ADD COLUMN IF NOT EXISTS site_id BIGINT NULL COMMENT '约定场地ID',
  ADD COLUMN IF NOT EXISTS construction_org_id BIGINT NULL COMMENT '建设单位ID',
  ADD COLUMN IF NOT EXISTS transport_org_id BIGINT NULL COMMENT '运输单位ID',
  ADD COLUMN IF NOT EXISTS site_operator_org_id BIGINT NULL COMMENT '场地运营单位ID',
  ADD COLUMN IF NOT EXISTS sign_date DATE NULL COMMENT '签订日期',
  ADD COLUMN IF NOT EXISTS effective_date DATE NULL COMMENT '生效日期',
  ADD COLUMN IF NOT EXISTS expire_date DATE NULL COMMENT '到期日期',
  ADD COLUMN IF NOT EXISTS agreed_volume DECIMAL(18,2) NULL DEFAULT 0 COMMENT '约定方量',
  ADD COLUMN IF NOT EXISTS unit_price DECIMAL(18,2) NULL COMMENT '单价',
  ADD COLUMN IF NOT EXISTS contract_amount DECIMAL(18,2) NULL DEFAULT 0 COMMENT '合同总金额',
  ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '累计已结算金额',
  ADD COLUMN IF NOT EXISTS change_version INT NOT NULL DEFAULT 0 COMMENT '变更版本号',
  ADD COLUMN IF NOT EXISTS remark VARCHAR(500) NULL COMMENT '备注',
  ADD COLUMN IF NOT EXISTS is_three_party TINYINT NOT NULL DEFAULT 0 COMMENT '是否三方合同',
  ADD COLUMN IF NOT EXISTS unit_price_inside DECIMAL(18,2) NULL COMMENT '区内单价',
  ADD COLUMN IF NOT EXISTS unit_price_outside DECIMAL(18,2) NULL COMMENT '区外单价',
  ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '来源: ONLINE/OFFLINE/IMPORT',
  ADD COLUMN IF NOT EXISTS applicant_id BIGINT NULL COMMENT '申请人ID',
  ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500) NULL COMMENT '退回原因';

CREATE UNIQUE INDEX IF NOT EXISTS uk_contract_no ON biz_contract(tenant_id, contract_no);
CREATE INDEX IF NOT EXISTS idx_contract_project_status ON biz_contract(tenant_id, project_id, contract_status);
CREATE INDEX IF NOT EXISTS idx_contract_site_status ON biz_contract(tenant_id, site_id, contract_status);
CREATE INDEX IF NOT EXISTS idx_contract_sign_date ON biz_contract(tenant_id, sign_date);
CREATE INDEX IF NOT EXISTS idx_contract_type ON biz_contract(tenant_id, contract_type, contract_status);

-- 2. 合同变更申请
CREATE TABLE IF NOT EXISTS biz_contract_change_apply (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  change_no VARCHAR(64) NOT NULL COMMENT '变更申请编号',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  change_type VARCHAR(32) NOT NULL COMMENT '变更类型: SITE/VOLUME/PRICE/PERIOD/OTHER',
  before_snapshot_json TEXT NULL COMMENT '变更前快照JSON',
  after_snapshot_json TEXT NULL COMMENT '变更后快照JSON',
  reason VARCHAR(500) NULL COMMENT '变更原因',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  process_instance_id VARCHAR(64) NULL COMMENT '流程实例ID',
  current_node_code VARCHAR(64) NULL COMMENT '当前审批节点',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  reject_reason VARCHAR(500) NULL COMMENT '退回原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_change_no (tenant_id, change_no),
  KEY idx_change_contract_status (tenant_id, contract_id, approval_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同变更申请';

-- 3. 延期申请
CREATE TABLE IF NOT EXISTS biz_contract_extension_apply (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  apply_no VARCHAR(64) NOT NULL COMMENT '延期申请编号',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  original_expire_date DATE NOT NULL COMMENT '原到期日期',
  requested_expire_date DATE NOT NULL COMMENT '申请新到期日期',
  requested_volume_delta DECIMAL(18,2) NULL DEFAULT 0 COMMENT '追加方量',
  reason VARCHAR(500) NULL COMMENT '延期原因',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  process_instance_id VARCHAR(64) NULL COMMENT '流程实例ID',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  reject_reason VARCHAR(500) NULL COMMENT '退回原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_extension_no (tenant_id, apply_no),
  KEY idx_extension_contract_status (tenant_id, contract_id, approval_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同延期申请';

-- 4. 内拨申请
CREATE TABLE IF NOT EXISTS biz_contract_transfer_apply (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  transfer_no VARCHAR(64) NOT NULL COMMENT '内拨申请编号',
  source_contract_id BIGINT NOT NULL COMMENT '源合同ID',
  target_contract_id BIGINT NOT NULL COMMENT '目标合同ID',
  transfer_amount DECIMAL(18,2) NULL DEFAULT 0 COMMENT '调拨金额',
  transfer_volume DECIMAL(18,2) NULL DEFAULT 0 COMMENT '调拨方量',
  reason VARCHAR(500) NULL COMMENT '调拨原因',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  process_instance_id VARCHAR(64) NULL COMMENT '流程实例ID',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  reject_reason VARCHAR(500) NULL COMMENT '退回原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_transfer_no (tenant_id, transfer_no),
  KEY idx_transfer_source_status (tenant_id, source_contract_id, approval_status),
  KEY idx_transfer_target_status (tenant_id, target_contract_id, approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同内拨申请';

-- 5. 结算单主表
CREATE TABLE IF NOT EXISTS biz_settlement_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  settlement_no VARCHAR(64) NOT NULL COMMENT '结算单号',
  settlement_type VARCHAR(32) NOT NULL COMMENT '结算类型: PROJECT/SITE',
  target_project_id BIGINT NULL COMMENT '项目ID',
  target_site_id BIGINT NULL COMMENT '场地ID',
  period_start DATE NOT NULL COMMENT '结算周期开始',
  period_end DATE NOT NULL COMMENT '结算周期结束',
  settlement_date DATE NULL COMMENT '结算日期',
  total_volume DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总方量',
  unit_price DECIMAL(18,2) NULL COMMENT '单价',
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
  adjust_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '调整金额',
  payable_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应付金额',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  settlement_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '结算状态',
  process_instance_id VARCHAR(64) NULL COMMENT '流程实例ID',
  creator_id BIGINT NULL COMMENT '创建人ID',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_settlement_no (tenant_id, settlement_no),
  KEY idx_settlement_project_period (tenant_id, settlement_type, target_project_id, period_start, period_end),
  KEY idx_settlement_site_period (tenant_id, settlement_type, target_site_id, period_start, period_end),
  KEY idx_settlement_status (tenant_id, settlement_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单主表';

-- 6. 结算明细
CREATE TABLE IF NOT EXISTS biz_settlement_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  settlement_order_id BIGINT NOT NULL COMMENT '结算单ID',
  source_record_type VARCHAR(32) NULL COMMENT '来源类型',
  source_record_id BIGINT NULL COMMENT '来源记录ID',
  project_id BIGINT NULL COMMENT '项目ID',
  site_id BIGINT NULL COMMENT '场地ID',
  vehicle_id BIGINT NULL COMMENT '车辆ID',
  biz_date DATE NULL COMMENT '业务日期',
  volume DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '方量',
  unit_price DECIMAL(18,2) NULL COMMENT '单价',
  amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_settlement_item_order (settlement_order_id, biz_date),
  KEY idx_settlement_item_project (tenant_id, project_id, biz_date),
  KEY idx_settlement_item_site (tenant_id, site_id, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算明细';

-- 7. 合同导入批次
CREATE TABLE IF NOT EXISTS biz_contract_import_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
  file_name VARCHAR(200) NULL COMMENT '文件名',
  file_url VARCHAR(500) NULL COMMENT '文件URL',
  file_hash VARCHAR(128) NULL COMMENT '文件哈希(幂等)',
  total_count INT NOT NULL DEFAULT 0 COMMENT '总行数',
  success_count INT NOT NULL DEFAULT 0 COMMENT '成功行数',
  fail_count INT NOT NULL DEFAULT 0 COMMENT '失败行数',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PREVIEWED/COMMITTED/FAILED',
  operator_id BIGINT NULL COMMENT '操作人',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_import_batch_no (tenant_id, batch_no),
  KEY idx_import_batch_status (tenant_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同导入批次';

-- 8. 合同导入错误
CREATE TABLE IF NOT EXISTS biz_contract_import_error (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '租户ID',
  batch_id BIGINT NOT NULL COMMENT '批次ID',
  row_no INT NOT NULL COMMENT '行号',
  contract_no VARCHAR(64) NULL COMMENT '合同编号',
  error_code VARCHAR(64) NULL COMMENT '错误码',
  error_message VARCHAR(500) NULL COMMENT '错误信息',
  raw_json TEXT NULL COMMENT '原始数据JSON',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_import_error_batch (batch_id, row_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同导入错误明细';

-- 9. 异步导出任务
CREATE TABLE IF NOT EXISTS biz_report_export_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型: CONTRACT/SETTLEMENT/MONTHLY_REPORT',
  export_type VARCHAR(32) NULL COMMENT '导出类型',
  query_json TEXT NULL COMMENT '查询条件JSON',
  file_name VARCHAR(200) NULL COMMENT '文件名',
  file_url VARCHAR(500) NULL COMMENT '文件URL',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
  fail_reason VARCHAR(500) NULL COMMENT '失败原因',
  creator_id BIGINT NULL COMMENT '创建人',
  expire_time DATETIME NULL COMMENT '文件过期时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_export_task_biz_status (tenant_id, biz_type, status, create_time),
  KEY idx_export_task_creator (tenant_id, creator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出任务';

-- 10. 月报/单位统计快照
CREATE TABLE IF NOT EXISTS biz_contract_stat_snapshot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  stat_date DATE NULL COMMENT '统计日期',
  stat_month VARCHAR(7) NOT NULL COMMENT '统计月份 YYYY-MM',
  stat_type VARCHAR(32) NOT NULL COMMENT '统计类型: MONTHLY/DAILY/YEARLY',
  dimension_type VARCHAR(32) NOT NULL DEFAULT 'TOTAL' COMMENT '维度: TOTAL/ORG/PROJECT/SITE',
  dimension_id BIGINT NULL COMMENT '维度ID',
  contract_count INT NOT NULL DEFAULT 0 COMMENT '合同数',
  new_contract_count INT NOT NULL DEFAULT 0 COMMENT '新增合同数',
  contract_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '合同金额',
  receipt_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '入账金额',
  settlement_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '结算金额',
  agreed_volume DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '约定方量',
  actual_volume DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '实际方量',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_contract_stat_month (tenant_id, stat_month, stat_type),
  KEY idx_contract_stat_dimension (tenant_id, dimension_type, dimension_id, stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同统计快照';
