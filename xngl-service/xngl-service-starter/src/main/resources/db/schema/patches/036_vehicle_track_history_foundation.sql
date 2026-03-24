CREATE TABLE IF NOT EXISTS biz_vehicle_track_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  plate_no VARCHAR(20) NOT NULL,
  lng DECIMAL(10,7) NOT NULL,
  lat DECIMAL(10,7) NOT NULL,
  speed DECIMAL(10,2) NULL,
  direction DECIMAL(10,2) NULL,
  locate_time DATETIME NOT NULL,
  source_type VARCHAR(32) NULL DEFAULT 'GPS',
  remark VARCHAR(255) NULL,
  create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NULL DEFAULT 0,
  KEY idx_vehicle_track_tenant_vehicle_time (tenant_id, vehicle_id, locate_time)
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 1, '浙A12345', 120.1762450, 30.2512100, 32.50, 18.00, '2026-03-20 08:10:00', 'GPS', '早高峰运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 1 AND locate_time = '2026-03-20 08:10:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 1, '浙A12345', 120.1812450, 30.2552100, 36.00, 24.00, '2026-03-20 09:00:00', 'GPS', '早高峰运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 1 AND locate_time = '2026-03-20 09:00:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 1, '浙A12345', 120.1862450, 30.2585100, 28.20, 35.00, '2026-03-20 09:45:00', 'GPS', '早高峰运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 1 AND locate_time = '2026-03-20 09:45:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 1, '浙A12345', 120.1912450, 30.2632100, 19.60, 48.00, '2026-03-20 11:24:16', 'GPS', '当前最新定位'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 1 AND locate_time = '2026-03-20 11:24:16'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 4, '浙A22345', 120.2014500, 30.2716500, 26.00, 42.00, '2026-03-20 08:15:00', 'GPS', '场地运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 4 AND locate_time = '2026-03-20 08:15:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 4, '浙A22345', 120.2081100, 30.2793200, 31.20, 55.00, '2026-03-20 09:10:00', 'GPS', '场地运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 4 AND locate_time = '2026-03-20 09:10:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 4, '浙A22345', 120.2158000, 30.2834500, 23.50, 61.00, '2026-03-20 10:05:00', 'GPS', '场地运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 4 AND locate_time = '2026-03-20 10:05:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 4, '浙A22345', 120.2234500, 30.2876500, 16.80, 68.00, '2026-03-20 11:24:16', 'GPS', '当前最新定位'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 4 AND locate_time = '2026-03-20 11:24:16'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 6, '浙A32345', 120.2517410, 30.1847630, 18.00, 12.00, '2026-03-20 08:05:00', 'GPS', '园区运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 6 AND locate_time = '2026-03-20 08:05:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 6, '浙A32345', 120.2567410, 30.1889630, 25.00, 18.00, '2026-03-20 08:55:00', 'GPS', '园区运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 6 AND locate_time = '2026-03-20 08:55:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 6, '浙A32345', 120.2618410, 30.1938630, 22.40, 26.00, '2026-03-20 09:40:00', 'GPS', '园区运输轨迹'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 6 AND locate_time = '2026-03-20 09:40:00'
);

INSERT INTO biz_vehicle_track_point (
  tenant_id, vehicle_id, plate_no, lng, lat, speed, direction, locate_time, source_type, remark
)
SELECT 1, 6, '浙A32345', 120.2677410, 30.1987630, 12.30, 31.00, '2026-03-20 11:24:16', 'GPS', '当前最新定位'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_vehicle_track_point WHERE tenant_id = 1 AND vehicle_id = 6 AND locate_time = '2026-03-20 11:24:16'
);
