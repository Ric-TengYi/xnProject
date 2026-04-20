-- ============================================================
-- 025: 车队人证管理
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_vehicle_personnel_certificate (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  person_name VARCHAR(64) NOT NULL,
  mobile VARCHAR(32) NULL,
  role_type VARCHAR(32) NOT NULL DEFAULT 'DRIVER',
  id_card_no VARCHAR(64) NULL,
  driver_license_no VARCHAR(64) NULL,
  driver_license_expire_date DATE NULL,
  transport_license_no VARCHAR(64) NULL,
  transport_license_expire_date DATE NULL,
  fee_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  fee_due_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_vehicle_personnel_org (tenant_id, org_id),
  KEY idx_vehicle_personnel_vehicle (tenant_id, vehicle_id),
  KEY idx_vehicle_personnel_status (tenant_id, status),
  KEY idx_vehicle_personnel_driver_expire (driver_license_expire_date),
  KEY idx_vehicle_personnel_transport_expire (transport_license_expire_date)
) COMMENT='车队人证与相关费用台账';

INSERT INTO biz_vehicle_personnel_certificate (
  tenant_id, org_id, vehicle_id, person_name, mobile, role_type, id_card_no,
  driver_license_no, driver_license_expire_date, transport_license_no, transport_license_expire_date,
  fee_amount, paid_amount, fee_due_date, status, remark
)
SELECT
  1, v.org_id, v.id, '张建国', '13800000011', 'DRIVER', '3301********0011',
  'A1-330100198504120011', DATE('2027-05-18'), 'HZ-DRIVER-001', DATE('2026-09-30'),
  3600.00, 2400.00, DATE('2026-06-30'), 'ACTIVE', '驾驶员人证演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A12345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_personnel_certificate t
    WHERE t.tenant_id = 1 AND t.person_name = '张建国' AND t.driver_license_no = 'A1-330100198504120011'
  );

INSERT INTO biz_vehicle_personnel_certificate (
  tenant_id, org_id, vehicle_id, person_name, mobile, role_type, id_card_no,
  driver_license_no, driver_license_expire_date, transport_license_no, transport_license_expire_date,
  fee_amount, paid_amount, fee_due_date, status, remark
)
SELECT
  1, v.org_id, v.id, '李春雷', '13800000022', 'CAPTAIN', '3301********0022',
  'A2-330100198609150022', DATE('2026-04-10'), 'HZ-CAPTAIN-009', DATE('2026-04-08'),
  4200.00, 4200.00, DATE('2026-04-05'), 'ACTIVE', '即将到期人证演示数据'
FROM biz_vehicle v
WHERE v.tenant_id = 1 AND v.plate_no = '浙A22345'
  AND NOT EXISTS (
    SELECT 1 FROM biz_vehicle_personnel_certificate t
    WHERE t.tenant_id = 1 AND t.person_name = '李春雷' AND t.driver_license_no = 'A2-330100198609150022'
  );
