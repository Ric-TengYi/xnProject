CREATE TABLE IF NOT EXISTS biz_project_payment_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  payment_no VARCHAR(64) NOT NULL COMMENT '交款单号',
  payment_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '交款类型',
  amount DECIMAL(18,2) NOT NULL COMMENT '交款金额',
  payment_date DATE NOT NULL COMMENT '交款日期',
  voucher_no VARCHAR(64) DEFAULT NULL COMMENT '凭证号',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  source_id BIGINT DEFAULT NULL COMMENT '来源业务ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  operator_id BIGINT DEFAULT NULL COMMENT '登记操作人',
  cancel_operator_id BIGINT DEFAULT NULL COMMENT '冲销操作人',
  cancel_time DATETIME DEFAULT NULL COMMENT '冲销时间',
  cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '冲销原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_project_payment_no (tenant_id, payment_no),
  KEY idx_project_payment_project_date (tenant_id, project_id, payment_date),
  KEY idx_project_payment_status (tenant_id, status, payment_date),
  KEY idx_project_payment_cancel_operator (tenant_id, cancel_operator_id, cancel_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目交款记录表';

-- MySQL 不支持 ADD COLUMN IF NOT EXISTS；该列已在上面 CREATE TABLE 中定义。
-- 若表已存在且缺少 cancel_operator_id，可手动执行：
-- ALTER TABLE biz_project_payment_record ADD COLUMN cancel_operator_id BIGINT DEFAULT NULL COMMENT '冲销操作人';
