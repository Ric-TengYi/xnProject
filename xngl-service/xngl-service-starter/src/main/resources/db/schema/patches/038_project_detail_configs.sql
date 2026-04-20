CREATE TABLE IF NOT EXISTS biz_project_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  checkin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用打卡',
  checkin_account VARCHAR(64) NULL COMMENT '打卡账号',
  checkin_auth_scope VARCHAR(128) NULL COMMENT '授权范围',
  location_check_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用位置判断',
  location_radius_meters DECIMAL(10,2) NULL DEFAULT 200 COMMENT '位置判断半径',
  preload_volume DECIMAL(18,2) NULL DEFAULT 0 COMMENT '出土预扣值',
  route_geo_json TEXT NULL COMMENT '线路 GeoJSON',
  violation_fence_code VARCHAR(64) NULL COMMENT '违规围栏编码',
  violation_rule_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用违规围栏规则',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_project_config_tenant_project (tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目打卡与线路配置';

INSERT INTO biz_alert_fence (
  tenant_id, rule_code, fence_code, fence_name, fence_type, geo_json, buffer_meters, biz_scope, active_time_range, direction_rule, status
)
SELECT
  1,
  'PROJECT_VIOLATION_001',
  'PROJECT-FENCE-001',
  '项目-001 出土围栏',
  'ENTRY',
  '{"type":"Polygon","coordinates":[[[120.147600,30.261400],[120.154800,30.261400],[120.155900,30.266300],[120.149000,30.267200],[120.147600,30.261400]]]}',
  50,
  'PROJECT:1',
  '00:00-23:59',
  'BOTH',
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_fence WHERE tenant_id = 1 AND fence_code = 'PROJECT-FENCE-001'
);

INSERT INTO biz_project_config (
  tenant_id, project_id, checkin_enabled, checkin_account, checkin_auth_scope, location_check_required,
  location_radius_meters, preload_volume, route_geo_json, violation_fence_code, violation_rule_enabled, remark
)
SELECT
  1,
  1,
  1,
  'proj-punch-001',
  '建设单位/施工单位/司机联合授权',
  1,
  200,
  1500,
  '{"type":"LineString","coordinates":[[120.149300,30.263200],[120.156200,30.264400],[120.164500,30.261900],[120.172100,30.258500],[120.176520,30.257840]]}',
  'PROJECT-FENCE-001',
  1,
  '项目打卡、位置判断、预扣值、线路和违规围栏均启用'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_project_config WHERE tenant_id = 1 AND project_id = 1
);
