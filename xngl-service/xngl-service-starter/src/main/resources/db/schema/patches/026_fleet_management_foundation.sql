-- ============================================================
-- 026: 车队维护 / 运输计划 / 调度 / 财务 / 报表基础
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_fleet_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  fleet_name VARCHAR(128) NOT NULL,
  captain_name VARCHAR(64) NULL,
  captain_phone VARCHAR(32) NULL,
  driver_count_plan INT NOT NULL DEFAULT 0,
  vehicle_count_plan INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  attendance_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_fleet_profile_name (tenant_id, fleet_name),
  KEY idx_fleet_profile_org (tenant_id, org_id)
) COMMENT='车队信息维护';

CREATE TABLE IF NOT EXISTS biz_fleet_transport_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  plan_no VARCHAR(64) NOT NULL,
  plan_date DATE NOT NULL,
  source_point VARCHAR(128) NULL,
  destination_point VARCHAR(128) NULL,
  cargo_type VARCHAR(64) NULL,
  planned_trips INT NOT NULL DEFAULT 0,
  planned_volume DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_fleet_transport_plan_no (tenant_id, plan_no),
  KEY idx_fleet_transport_plan_fleet (tenant_id, fleet_id),
  KEY idx_fleet_transport_plan_date (plan_date)
) COMMENT='车队运输计划';

CREATE TABLE IF NOT EXISTS biz_fleet_dispatch_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  order_no VARCHAR(64) NOT NULL,
  related_plan_no VARCHAR(64) NULL,
  apply_date DATE NOT NULL,
  requested_vehicle_count INT NOT NULL DEFAULT 0,
  requested_driver_count INT NOT NULL DEFAULT 0,
  urgency_level VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
  applicant_name VARCHAR(64) NULL,
  approved_by VARCHAR(64) NULL,
  approved_time DATETIME NULL,
  audit_remark VARCHAR(500) NULL,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_fleet_dispatch_order_no (tenant_id, order_no),
  KEY idx_fleet_dispatch_order_fleet (tenant_id, fleet_id),
  KEY idx_fleet_dispatch_order_status (tenant_id, status)
) COMMENT='车队调度申请/审批';

CREATE TABLE IF NOT EXISTS biz_fleet_finance_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  record_no VARCHAR(64) NOT NULL,
  contract_no VARCHAR(64) NULL,
  statement_month VARCHAR(16) NULL,
  revenue_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  cost_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  other_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  settled_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_fleet_finance_record_no (tenant_id, record_no),
  KEY idx_fleet_finance_record_fleet (tenant_id, fleet_id),
  KEY idx_fleet_finance_record_month (statement_month)
) COMMENT='车队财务管理';

INSERT INTO biz_fleet_profile (
  tenant_id, org_id, fleet_name, captain_name, captain_phone, driver_count_plan, vehicle_count_plan, status, attendance_mode, remark
)
SELECT
  1,
  MIN(v.org_id),
  v.fleet_name,
  MAX(v.captain_name),
  MAX(v.captain_phone),
  COUNT(DISTINCT COALESCE(v.driver_phone, v.driver_name)),
  COUNT(1),
  'ENABLED',
  'MANUAL',
  '从车辆主数据初始化'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.fleet_name IS NOT NULL AND v.fleet_name <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM biz_fleet_profile f
    WHERE f.tenant_id = 1
      AND f.fleet_name = CONVERT(v.fleet_name USING utf8mb4) COLLATE utf8mb4_unicode_ci
  )
GROUP BY v.fleet_name;

INSERT INTO biz_fleet_transport_plan (
  tenant_id, fleet_id, org_id, plan_no, plan_date, source_point, destination_point, cargo_type,
  planned_trips, planned_volume, status, remark
)
SELECT
  1, f.id, f.org_id, 'FTP-2026-0001', DATE('2026-03-20'), '余杭东区临时堆场', '城北一号消纳场', '渣土',
  18, 680.00, 'ACTIVE', '运输计划演示数据'
FROM biz_fleet_profile f
WHERE f.tenant_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM biz_fleet_transport_plan p WHERE p.tenant_id = 1 AND p.plan_no = 'FTP-2026-0001'
  )
LIMIT 1;

INSERT INTO biz_fleet_dispatch_order (
  tenant_id, fleet_id, org_id, order_no, related_plan_no, apply_date, requested_vehicle_count,
  requested_driver_count, urgency_level, status, applicant_name, approved_by, approved_time,
  audit_remark, remark
)
SELECT
  1, f.id, f.org_id, 'FDO-2026-0001', 'FTP-2026-0001', DATE('2026-03-20'), 8,
  8, 'HIGH', 'APPROVED', '调度中心', '系统管理员', NOW(),
  '同意按计划出车', '调度申请演示数据'
FROM biz_fleet_profile f
WHERE f.tenant_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM biz_fleet_dispatch_order d WHERE d.tenant_id = 1 AND d.order_no = 'FDO-2026-0001'
  )
LIMIT 1;

INSERT INTO biz_fleet_finance_record (
  tenant_id, fleet_id, org_id, record_no, contract_no, statement_month, revenue_amount, cost_amount,
  other_amount, settled_amount, status, remark
)
SELECT
  1, f.id, f.org_id, 'FFR-2026-0001', 'TRANS-HT-2026-001', '2026-03', 186000.00, 126000.00,
  8600.00, 98000.00, 'CONFIRMED', '车队财务演示数据'
FROM biz_fleet_profile f
WHERE f.tenant_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM biz_fleet_finance_record r WHERE r.tenant_id = 1 AND r.record_no = 'FFR-2026-0001'
  )
LIMIT 1;
