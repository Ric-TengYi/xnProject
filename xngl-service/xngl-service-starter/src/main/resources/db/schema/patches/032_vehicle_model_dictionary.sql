-- ============================================================
-- 032: 车型字典库
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_vehicle_model (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  model_code VARCHAR(64) NOT NULL,
  brand VARCHAR(64) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  vehicle_type VARCHAR(64) NULL,
  axle_count INT NULL,
  seat_count INT NULL DEFAULT 2,
  dead_weight DECIMAL(12,2) NOT NULL DEFAULT 0,
  load_weight DECIMAL(12,2) NOT NULL DEFAULT 0,
  energy_type VARCHAR(32) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vehicle_model_code (tenant_id, model_code),
  KEY idx_vehicle_model_brand_status (tenant_id, brand, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO biz_vehicle_model
  (tenant_id, model_code, brand, model_name, vehicle_type, axle_count, seat_count, dead_weight, load_weight, energy_type, status, remark)
SELECT 1, 'HOWO-8X4', '中国重汽', '豪沃 TX 8x4', '重型自卸货车', 4, 2, 15.80, 31.00, 'DIESEL', 'ENABLED', '平台默认车型'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_model WHERE tenant_id = 1 AND model_code = 'HOWO-8X4'
);

INSERT INTO biz_vehicle_model
  (tenant_id, model_code, brand, model_name, vehicle_type, axle_count, seat_count, dead_weight, load_weight, energy_type, status, remark)
SELECT 1, 'SHACMAN-6X4', '陕汽重卡', '德龙 X5000 6x4', '中型自卸货车', 3, 2, 12.60, 18.50, 'DIESEL', 'ENABLED', '平台默认车型'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_model WHERE tenant_id = 1 AND model_code = 'SHACMAN-6X4'
);

INSERT INTO biz_vehicle_model
  (tenant_id, model_code, brand, model_name, vehicle_type, axle_count, seat_count, dead_weight, load_weight, energy_type, status, remark)
SELECT 1, 'FOTON-EV-4X2', '福田汽车', '欧曼智蓝 4x2', '轻型自卸货车', 2, 2, 8.90, 10.00, 'ELECTRIC', 'ENABLED', '平台默认新能源车型'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_model WHERE tenant_id = 1 AND model_code = 'FOTON-EV-4X2'
);
