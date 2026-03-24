SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'lng'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN lng DECIMAL(10,7) NULL COMMENT ''场地中心经度'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'lat'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN lat DECIMAL(10,7) NULL COMMENT ''场地中心纬度'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'biz_site' AND COLUMN_NAME = 'boundary_geo_json'
  ),
  'SELECT 1',
  'ALTER TABLE biz_site ADD COLUMN boundary_geo_json TEXT NULL COMMENT ''场地红线 GeoJSON'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS biz_site_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
  device_type VARCHAR(32) NOT NULL COMMENT '设备类型',
  provider VARCHAR(64) NULL COMMENT '设备厂家',
  ip_address VARCHAR(64) NULL COMMENT '设备IP',
  status VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '设备状态',
  lng DECIMAL(10,7) NULL COMMENT '设备经度',
  lat DECIMAL(10,7) NULL COMMENT '设备纬度',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_site_device_code (tenant_id, device_code),
  KEY idx_site_device_site_status (site_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地设备点位';

UPDATE biz_site
SET lng = 120.1765200,
    lat = 30.2578400,
    boundary_geo_json = '{"type":"Polygon","coordinates":[[[120.172600,30.255420],[120.180480,30.255420],[120.181720,30.258040],[120.179380,30.260960],[120.173280,30.260380],[120.172600,30.255420]]]}'
WHERE id = 1
  AND deleted = 0
  AND (lng IS NULL OR lat IS NULL OR boundary_geo_json IS NULL);

INSERT INTO biz_site_device (
  tenant_id, site_id, device_code, device_name, device_type, provider, ip_address, status, lng, lat, remark
)
SELECT 1, 1, 'SITE-DEV-001', '东门抓拍机', 'CAPTURE_CAMERA', '海康威视', '192.168.10.21', 'ONLINE', 120.1738400, 30.2561200, '场地东门进场抓拍'
WHERE EXISTS (SELECT 1 FROM biz_site WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM biz_site_device WHERE tenant_id = 1 AND device_code = 'SITE-DEV-001');

INSERT INTO biz_site_device (
  tenant_id, site_id, device_code, device_name, device_type, provider, ip_address, status, lng, lat, remark
)
SELECT 1, 1, 'SITE-DEV-002', '1号地磅', 'WEIGHBRIDGE', '梅特勒', '192.168.10.31', 'ONLINE', 120.1767800, 30.2572800, '主入口地磅设备'
WHERE EXISTS (SELECT 1 FROM biz_site WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM biz_site_device WHERE tenant_id = 1 AND device_code = 'SITE-DEV-002');

INSERT INTO biz_site_device (
  tenant_id, site_id, device_code, device_name, device_type, provider, ip_address, status, lng, lat, remark
)
SELECT 1, 1, 'SITE-DEV-003', '堆体全景球机', 'VIDEO_CAMERA', '大华', '192.168.10.41', 'OFFLINE', 120.1794800, 30.2596800, '场地高位全景监控'
WHERE EXISTS (SELECT 1 FROM biz_site WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM biz_site_device WHERE tenant_id = 1 AND device_code = 'SITE-DEV-003');
