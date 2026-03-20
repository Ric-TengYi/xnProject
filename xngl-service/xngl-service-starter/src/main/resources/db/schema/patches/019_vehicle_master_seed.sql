-- ============================================================
-- 019: 车辆主数据增强与演示数据
-- ============================================================

ALTER TABLE biz_vehicle ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID';
ALTER TABLE biz_vehicle ADD COLUMN vehicle_type VARCHAR(64) NULL COMMENT '车辆类型';
ALTER TABLE biz_vehicle ADD COLUMN brand VARCHAR(64) NULL COMMENT '品牌';
ALTER TABLE biz_vehicle ADD COLUMN model VARCHAR(64) NULL COMMENT '型号';
ALTER TABLE biz_vehicle ADD COLUMN energy_type VARCHAR(32) NULL COMMENT '能源类型';
ALTER TABLE biz_vehicle ADD COLUMN axle_count INT NULL COMMENT '轴数';
ALTER TABLE biz_vehicle ADD COLUMN dead_weight DECIMAL(10,2) NULL COMMENT '自重吨位';
ALTER TABLE biz_vehicle ADD COLUMN load_weight DECIMAL(10,2) NULL COMMENT '核载吨位';
ALTER TABLE biz_vehicle ADD COLUMN driver_name VARCHAR(64) NULL COMMENT '司机姓名';
ALTER TABLE biz_vehicle ADD COLUMN driver_phone VARCHAR(32) NULL COMMENT '司机电话';
ALTER TABLE biz_vehicle ADD COLUMN fleet_name VARCHAR(128) NULL COMMENT '车队名称';
ALTER TABLE biz_vehicle ADD COLUMN captain_name VARCHAR(64) NULL COMMENT '队长姓名';
ALTER TABLE biz_vehicle ADD COLUMN captain_phone VARCHAR(32) NULL COMMENT '队长电话';
ALTER TABLE biz_vehicle ADD COLUMN use_status VARCHAR(32) NULL DEFAULT 'ACTIVE' COMMENT '使用状态';
ALTER TABLE biz_vehicle ADD COLUMN running_status VARCHAR(32) NULL DEFAULT 'STOPPED' COMMENT '运行状态';
ALTER TABLE biz_vehicle ADD COLUMN current_speed DECIMAL(10,2) NULL DEFAULT 0 COMMENT '当前速度';
ALTER TABLE biz_vehicle ADD COLUMN current_mileage DECIMAL(12,2) NULL DEFAULT 0 COMMENT '当前里程';
ALTER TABLE biz_vehicle ADD COLUMN next_maintain_date DATE NULL COMMENT '下次保养日期';
ALTER TABLE biz_vehicle ADD COLUMN annual_inspection_expire_date DATE NULL COMMENT '年检到期日期';
ALTER TABLE biz_vehicle ADD COLUMN insurance_expire_date DATE NULL COMMENT '保险到期日期';
ALTER TABLE biz_vehicle ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注';

SET @create_idx_vehicle_tenant_status = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'biz_vehicle'
        AND index_name = 'idx_biz_vehicle_tenant_status'
    ),
    'SELECT 1',
    'CREATE INDEX idx_biz_vehicle_tenant_status ON biz_vehicle(tenant_id, status)'
  )
);
PREPARE stmt_vehicle_tenant_status FROM @create_idx_vehicle_tenant_status;
EXECUTE stmt_vehicle_tenant_status;
DEALLOCATE PREPARE stmt_vehicle_tenant_status;

SET @create_idx_vehicle_org_status = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'biz_vehicle'
        AND index_name = 'idx_biz_vehicle_org_status'
    ),
    'SELECT 1',
    'CREATE INDEX idx_biz_vehicle_org_status ON biz_vehicle(org_id, status)'
  )
);
PREPARE stmt_vehicle_org_status FROM @create_idx_vehicle_org_status;
EXECUTE stmt_vehicle_org_status;
DEALLOCATE PREPARE stmt_vehicle_org_status;

INSERT INTO sys_org (
  name,
  code,
  parent_id,
  sort_order,
  tenant_id,
  org_code,
  org_name,
  org_type,
  org_path,
  status
)
SELECT
  '宏基渣土运输公司',
  'ORG-TRANS-HJ',
  1,
  10,
  1,
  'ORG-TRANS-HJ',
  '宏基渣土运输公司',
  'TRANSPORT_COMPANY',
  '/1',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_org WHERE tenant_id = 1 AND org_code = 'ORG-TRANS-HJ'
);

INSERT INTO sys_org (
  name,
  code,
  parent_id,
  sort_order,
  tenant_id,
  org_code,
  org_name,
  org_type,
  org_path,
  status
)
SELECT
  '顺达土方工程队',
  'ORG-TRANS-SD',
  1,
  20,
  1,
  'ORG-TRANS-SD',
  '顺达土方工程队',
  'TRANSPORT_COMPANY',
  '/1',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_org WHERE tenant_id = 1 AND org_code = 'ORG-TRANS-SD'
);

INSERT INTO sys_org (
  name,
  code,
  parent_id,
  sort_order,
  tenant_id,
  org_code,
  org_name,
  org_type,
  org_path,
  status
)
SELECT
  '捷安运输',
  'ORG-TRANS-JA',
  1,
  30,
  1,
  'ORG-TRANS-JA',
  '捷安运输',
  'TRANSPORT_COMPANY',
  '/1',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_org WHERE tenant_id = 1 AND org_code = 'ORG-TRANS-JA'
);

INSERT INTO sys_org (
  name,
  code,
  parent_id,
  sort_order,
  tenant_id,
  org_code,
  org_name,
  org_type,
  org_path,
  status
)
SELECT
  '新思路运输',
  'ORG-TRANS-XS',
  1,
  40,
  1,
  'ORG-TRANS-XS',
  '新思路运输',
  'TRANSPORT_COMPANY',
  '/1',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_org WHERE tenant_id = 1 AND org_code = 'ORG-TRANS-XS'
);

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A12345',
  'VIN-HJ-2026001',
  o.id,
  '重型自卸货车',
  '中国重汽',
  'ZZ3257N3647A',
  'DIESEL',
  4,
  15.20,
  32.00,
  '刘强',
  '13800000001',
  '宏基第一先锋车队',
  '张建国',
  '13800138001',
  1,
  'ACTIVE',
  'MOVING',
  46.50,
  126580.00,
  DATE('2026-04-20'),
  DATE('2026-09-30'),
  DATE('2026-10-31'),
  120.1912450,
  30.2632100,
  NOW(),
  '示例车辆：运输主力车'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-HJ'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A12345');

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A12346',
  'VIN-HJ-2026002',
  o.id,
  '重型自卸货车',
  '陕汽',
  'SX32586V384',
  'DIESEL',
  4,
  14.80,
  30.00,
  '王志明',
  '13800000002',
  '宏基夜间突击队',
  '李志强',
  '13900139002',
  2,
  'MAINTENANCE',
  'STOPPED',
  0.00,
  118200.00,
  DATE('2026-03-25'),
  DATE('2026-05-15'),
  DATE('2026-04-18'),
  120.1734220,
  30.2498540,
  NOW(),
  '示例车辆：维修中'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-HJ'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A12346');

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A12347',
  'VIN-HJ-2026003',
  o.id,
  '重型自卸货车',
  '解放',
  'CA3250P66K2T1E5',
  'DIESEL',
  4,
  15.00,
  31.00,
  '陈亮',
  '13800000003',
  '宏基第一先锋车队',
  '张建国',
  '13800138001',
  3,
  'DISABLED',
  'OFFLINE',
  0.00,
  132450.00,
  DATE('2026-03-10'),
  DATE('2026-03-18'),
  DATE('2026-03-16'),
  120.1602130,
  30.2415600,
  NOW(),
  '示例车辆：禁用待整改'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-HJ'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A12347');

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A22345',
  'VIN-SD-2026001',
  o.id,
  '中型自卸货车',
  '福田',
  'BJ3185DJPFA',
  'DIESEL',
  4,
  12.50,
  24.00,
  '周海峰',
  '13800000011',
  '顺达一队',
  '王海波',
  '13700137003',
  1,
  'ACTIVE',
  'MOVING',
  38.20,
  86500.00,
  DATE('2026-05-08'),
  DATE('2026-11-20'),
  DATE('2026-12-18'),
  120.2234500,
  30.2876500,
  NOW(),
  '示例车辆：正常运营'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-SD'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A22345');

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A22346',
  'VIN-SD-2026002',
  o.id,
  '中型自卸货车',
  '福田',
  'BJ3185DJPFB',
  'ELECTRIC',
  4,
  12.00,
  22.00,
  '李海',
  '13800000012',
  '顺达二队',
  '钱峰',
  '13700137004',
  4,
  'ACTIVE',
  'STOPPED',
  0.00,
  64200.00,
  DATE('2026-04-05'),
  DATE('2026-08-10'),
  DATE('2026-07-20'),
  120.2098750,
  30.2925410,
  NOW(),
  '示例车辆：待命电车'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-SD'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A22346');

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A32345',
  'VIN-JA-2026001',
  o.id,
  '重型自卸货车',
  '东风',
  'DFH3250A13',
  'DIESEL',
  4,
  14.60,
  28.00,
  '赵鹏',
  '13800000021',
  '捷安特种运输队',
  '赵铁柱',
  '13600136004',
  1,
  'ACTIVE',
  'MOVING',
  42.30,
  95300.00,
  DATE('2026-04-01'),
  DATE('2026-06-30'),
  DATE('2026-07-15'),
  120.2677410,
  30.1987630,
  NOW(),
  '示例车辆：特种运输'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-JA'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A32345');

INSERT INTO biz_vehicle (
  tenant_id,
  plate_no,
  vin,
  org_id,
  vehicle_type,
  brand,
  model,
  energy_type,
  axle_count,
  dead_weight,
  load_weight,
  driver_name,
  driver_phone,
  fleet_name,
  captain_name,
  captain_phone,
  status,
  use_status,
  running_status,
  current_speed,
  current_mileage,
  next_maintain_date,
  annual_inspection_expire_date,
  insurance_expire_date,
  lng,
  lat,
  gps_time,
  remark
)
SELECT
  1,
  '浙A42345',
  'VIN-XS-2026001',
  o.id,
  '重型自卸货车',
  '上汽红岩',
  'CQ3256HTG384',
  'DIESEL',
  4,
  15.50,
  33.00,
  '孙涛',
  '13800000031',
  '新思路快运一队',
  '孙大伟',
  '13500135005',
  1,
  'ACTIVE',
  'OFFLINE',
  0.00,
  102800.00,
  DATE('2026-06-20'),
  DATE('2026-12-31'),
  DATE('2026-12-31'),
  120.3124560,
  30.2212330,
  NOW(),
  '示例车辆：当前离线'
FROM sys_org o
WHERE o.tenant_id = 1
  AND o.org_code = 'ORG-TRANS-XS'
  AND NOT EXISTS (SELECT 1 FROM biz_vehicle WHERE plate_no = '浙A42345');
