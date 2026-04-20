-- ============================================================
-- 024: 车辆维修、维保计划与执行记录
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_vehicle_maintenance_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  plan_no VARCHAR(64) NOT NULL,
  plan_type VARCHAR(64) NOT NULL,
  cycle_type VARCHAR(32) NOT NULL COMMENT 'DAY/MONTH/KM',
  cycle_value INT NOT NULL DEFAULT 30,
  last_maintain_date DATE NULL,
  next_maintain_date DATE NULL,
  last_odometer DECIMAL(18,2) NOT NULL DEFAULT 0,
  next_odometer DECIMAL(18,2) NULL,
  responsible_name VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_maintenance_plan_no (tenant_id, plan_no),
  KEY idx_vehicle_maintenance_plan_vehicle (vehicle_id),
  KEY idx_vehicle_maintenance_plan_status (tenant_id, status)
) COMMENT='车辆维保计划';

CREATE TABLE IF NOT EXISTS biz_vehicle_maintenance_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NULL,
  vehicle_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  record_no VARCHAR(64) NOT NULL,
  maintain_type VARCHAR(64) NOT NULL,
  service_date DATE NOT NULL,
  odometer DECIMAL(18,2) NOT NULL DEFAULT 0,
  vendor_name VARCHAR(128) NULL,
  cost_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  items VARCHAR(500) NULL,
  operator_name VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DONE',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_maintenance_record_no (tenant_id, record_no),
  KEY idx_vehicle_maintenance_record_vehicle (vehicle_id),
  KEY idx_vehicle_maintenance_record_plan (plan_id),
  KEY idx_vehicle_maintenance_record_date (service_date)
) COMMENT='车辆维保执行记录';

CREATE TABLE IF NOT EXISTS biz_vehicle_repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  order_no VARCHAR(64) NOT NULL,
  urgency_level VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  repair_reason VARCHAR(255) NOT NULL,
  repair_content VARCHAR(500) NULL,
  budget_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  apply_date DATE NOT NULL,
  applicant_name VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
  audit_remark VARCHAR(500) NULL,
  approved_by VARCHAR(64) NULL,
  approved_time DATETIME NULL,
  completed_date DATE NULL,
  vendor_name VARCHAR(128) NULL,
  actual_amount DECIMAL(18,2) NULL,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_repair_order_no (tenant_id, order_no),
  KEY idx_vehicle_repair_order_vehicle (vehicle_id),
  KEY idx_vehicle_repair_order_status (tenant_id, status),
  KEY idx_vehicle_repair_order_apply_date (apply_date)
) COMMENT='车辆维修申请单';

INSERT INTO biz_vehicle_maintenance_plan (
  tenant_id, vehicle_id, org_id, plan_no, plan_type, cycle_type, cycle_value,
  last_maintain_date, next_maintain_date, last_odometer, next_odometer, responsible_name, status, remark
)
SELECT
  1, v.id, v.org_id, 'MPLAN-2026-0001', '常规保养', 'MONTH', 3,
  DATE('2025-12-20'), DATE('2026-03-25'), 12000, 18000, '张队', 'ACTIVE', '季度保养计划演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A12345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_maintenance_plan p WHERE p.tenant_id = 1 AND p.plan_no = 'MPLAN-2026-0001'
  );

INSERT INTO biz_vehicle_maintenance_plan (
  tenant_id, vehicle_id, org_id, plan_no, plan_type, cycle_type, cycle_value,
  last_maintain_date, next_maintain_date, last_odometer, next_odometer, responsible_name, status, remark
)
SELECT
  1, v.id, v.org_id, 'MPLAN-2026-0002', '轮胎检查', 'KM', 8000,
  DATE('2025-11-10'), DATE('2026-02-28'), 28000, 36000, '李班长', 'ACTIVE', '里程维保计划演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A22345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_maintenance_plan p WHERE p.tenant_id = 1 AND p.plan_no = 'MPLAN-2026-0002'
  );

INSERT INTO biz_vehicle_maintenance_record (
  tenant_id, plan_id, vehicle_id, org_id, record_no, maintain_type, service_date,
  odometer, vendor_name, cost_amount, items, operator_name, status, remark
)
SELECT
  1, p.id, p.vehicle_id, p.org_id, 'MREC-2026-0001', p.plan_type, DATE('2025-12-20'),
  12000, '杭州城北机务站', 1850.00, '机油更换/滤芯保养', '王师傅', 'DONE', '历史维保演示数据'
FROM biz_vehicle_maintenance_plan p
WHERE p.tenant_id = 1 AND p.plan_no = 'MPLAN-2026-0001'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_maintenance_record r WHERE r.tenant_id = 1 AND r.record_no = 'MREC-2026-0001'
  );

INSERT INTO biz_vehicle_repair_order (
  tenant_id, vehicle_id, org_id, order_no, urgency_level, repair_reason, repair_content,
  budget_amount, apply_date, applicant_name, status, audit_remark, approved_by, approved_time,
  completed_date, vendor_name, actual_amount, remark
)
SELECT
  1, v.id, v.org_id, 'REP-2026-0001', 'HIGH', '制动片磨损异常', '更换前后制动片并检查制动油路',
  3200.00, DATE('2026-03-18'), '张队', 'APPROVED', '同意进厂维修', '系统管理员', NOW(),
  NULL, NULL, NULL, '待安排维修演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A12345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_repair_order o WHERE o.tenant_id = 1 AND o.order_no = 'REP-2026-0001'
  );

INSERT INTO biz_vehicle_repair_order (
  tenant_id, vehicle_id, org_id, order_no, urgency_level, repair_reason, repair_content,
  budget_amount, apply_date, applicant_name, status, audit_remark, approved_by, approved_time,
  completed_date, vendor_name, actual_amount, remark
)
SELECT
  1, v.id, v.org_id, 'REP-2026-0002', 'MEDIUM', '空调制冷不足', '检查压缩机及冷媒压力',
  1200.00, DATE('2026-03-10'), '李班长', 'COMPLETED', '已完成维修', '系统管理员', DATE_ADD(NOW(), INTERVAL -7 DAY),
  DATE('2026-03-12'), '杭州顺达汽修厂', 980.00, '已完成维修演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A22345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_repair_order o WHERE o.tenant_id = 1 AND o.order_no = 'REP-2026-0002'
  );
