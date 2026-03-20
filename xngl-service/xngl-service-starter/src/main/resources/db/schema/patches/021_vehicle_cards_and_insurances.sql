-- ============================================================
-- 021: 车辆油电卡与保险管理
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_vehicle_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  card_no VARCHAR(64) NOT NULL,
  card_type VARCHAR(32) NOT NULL COMMENT 'FUEL/ELECTRIC',
  provider_name VARCHAR(128) NULL,
  org_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  balance DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_recharge DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_consume DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_biz_vehicle_card_tenant_no (tenant_id, card_no),
  KEY idx_biz_vehicle_card_tenant_type (tenant_id, card_type),
  KEY idx_biz_vehicle_card_vehicle (vehicle_id),
  KEY idx_biz_vehicle_card_org (org_id)
) COMMENT='车辆油电卡';

CREATE TABLE IF NOT EXISTS biz_vehicle_insurance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  policy_no VARCHAR(64) NOT NULL,
  insurance_type VARCHAR(64) NOT NULL,
  insurer_name VARCHAR(128) NOT NULL,
  coverage_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  premium_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  claim_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_biz_vehicle_insurance_tenant_policy (tenant_id, policy_no),
  KEY idx_biz_vehicle_insurance_vehicle (vehicle_id),
  KEY idx_biz_vehicle_insurance_tenant_status (tenant_id, status),
  KEY idx_biz_vehicle_insurance_end_date (end_date)
) COMMENT='车辆保险记录';

INSERT INTO biz_vehicle_card (
  tenant_id, card_no, card_type, provider_name, org_id, vehicle_id,
  balance, total_recharge, total_consume, status, remark
)
SELECT
  1,
  'CARD-FUEL-0001',
  'FUEL',
  '中石化企业车队卡',
  v.org_id,
  v.id,
  5200.50,
  20000.00,
  14799.50,
  'NORMAL',
  '油卡演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A12345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_card c WHERE c.tenant_id = 1 AND c.card_no = 'CARD-FUEL-0001'
  );

INSERT INTO biz_vehicle_card (
  tenant_id, card_no, card_type, provider_name, org_id, vehicle_id,
  balance, total_recharge, total_consume, status, remark
)
SELECT
  1,
  'CARD-ELEC-0002',
  'ELECTRIC',
  '国家电网充电卡',
  v.org_id,
  v.id,
  150.00,
  5000.00,
  4850.00,
  'LOW_BALANCE',
  '电卡余额预警演示'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A22345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_card c WHERE c.tenant_id = 1 AND c.card_no = 'CARD-ELEC-0002'
  );

INSERT INTO biz_vehicle_card (
  tenant_id, card_no, card_type, provider_name, org_id, vehicle_id,
  balance, total_recharge, total_consume, status, remark
)
SELECT
  1,
  'CARD-FUEL-0003',
  'FUEL',
  '中国石油车队卡',
  o.id,
  NULL,
  0.00,
  0.00,
  0.00,
  'UNBOUND',
  '待绑定车辆演示卡'
FROM sys_org o
WHERE o.tenant_id = 1 AND o.org_code = 'ORG-TRANS-HJ'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_card c WHERE c.tenant_id = 1 AND c.card_no = 'CARD-FUEL-0003'
  );

INSERT INTO biz_vehicle_card (
  tenant_id, card_no, card_type, provider_name, org_id, vehicle_id,
  balance, total_recharge, total_consume, status, remark
)
SELECT
  1,
  'CARD-ELEC-0004',
  'ELECTRIC',
  '特来电企业卡',
  v.org_id,
  v.id,
  3400.00,
  10000.00,
  6600.00,
  'NORMAL',
  '电卡演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A32345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_card c WHERE c.tenant_id = 1 AND c.card_no = 'CARD-ELEC-0004'
  );

INSERT INTO biz_vehicle_insurance (
  tenant_id, vehicle_id, policy_no, insurance_type, insurer_name,
  coverage_amount, premium_amount, claim_amount, start_date, end_date, status, remark
)
SELECT
  1,
  v.id,
  'POL-2026-0001',
  '交强险',
  '中国人保财险杭州分公司',
  1220000.00,
  8600.00,
  0.00,
  DATE('2026-01-01'),
  DATE('2026-12-31'),
  'ACTIVE',
  '年度交强险演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A12345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_insurance i WHERE i.tenant_id = 1 AND i.policy_no = 'POL-2026-0001'
  );

INSERT INTO biz_vehicle_insurance (
  tenant_id, vehicle_id, policy_no, insurance_type, insurer_name,
  coverage_amount, premium_amount, claim_amount, start_date, end_date, status, remark
)
SELECT
  1,
  v.id,
  'POL-2026-0002',
  '商业险',
  '平安产险浙江分公司',
  2500000.00,
  12600.00,
  18000.00,
  DATE('2025-10-01'),
  DATE('2026-04-05'),
  'EXPIRING',
  '临近到期演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A22345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_insurance i WHERE i.tenant_id = 1 AND i.policy_no = 'POL-2026-0002'
  );

INSERT INTO biz_vehicle_insurance (
  tenant_id, vehicle_id, policy_no, insurance_type, insurer_name,
  coverage_amount, premium_amount, claim_amount, start_date, end_date, status, remark
)
SELECT
  1,
  v.id,
  'POL-2025-0003',
  '第三者责任险',
  '太平洋保险杭州中心支公司',
  3000000.00,
  9800.00,
  5200.00,
  DATE('2025-01-01'),
  DATE('2026-02-28'),
  'EXPIRED',
  '已过期演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A32345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_insurance i WHERE i.tenant_id = 1 AND i.policy_no = 'POL-2025-0003'
  );
