CREATE TABLE IF NOT EXISTS sys_platform_integration_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  integration_code VARCHAR(32) NOT NULL COMMENT '对接编码',
  integration_name VARCHAR(64) NOT NULL COMMENT '对接名称',
  enabled INT NOT NULL DEFAULT 0 COMMENT '是否启用',
  vendor_name VARCHAR(64) NULL COMMENT '厂商/平台名',
  base_url VARCHAR(255) NULL COMMENT '基础地址',
  api_version VARCHAR(64) NULL COMMENT '版本号',
  client_id VARCHAR(128) NULL COMMENT '客户端ID',
  client_secret VARCHAR(255) NULL COMMENT '客户端密钥',
  access_key VARCHAR(128) NULL COMMENT '访问Key',
  access_secret VARCHAR(255) NULL COMMENT '访问Secret',
  callback_path VARCHAR(255) NULL COMMENT '回调地址',
  ext_json TEXT NULL COMMENT '扩展配置',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_platform_integration_code (tenant_id, integration_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台对接配置';

CREATE TABLE IF NOT EXISTS sys_sso_login_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  ticket VARCHAR(96) NOT NULL COMMENT '登录票据',
  target_platform VARCHAR(64) NULL COMMENT '目标平台',
  redirect_uri VARCHAR(500) NULL COMMENT '回跳地址',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  used_flag INT NOT NULL DEFAULT 0 COMMENT '是否已使用',
  used_time DATETIME NULL COMMENT '使用时间',
  ext_json TEXT NULL COMMENT '扩展信息',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sso_ticket (ticket, deleted),
  KEY idx_sso_ticket_user (tenant_id, user_id, used_flag),
  KEY idx_sso_ticket_expire (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证登录票据';

CREATE TABLE IF NOT EXISTS biz_dam_monitor_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  integration_code VARCHAR(32) NOT NULL DEFAULT 'DAM_MONITOR' COMMENT '对接编码',
  device_name VARCHAR(128) NULL COMMENT '设备名称',
  monitor_time DATETIME NOT NULL COMMENT '监测时间',
  online_status VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '在线状态',
  safety_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '安全等级',
  displacement_value DECIMAL(12,4) NULL COMMENT '位移值',
  water_level DECIMAL(12,4) NULL COMMENT '水位',
  rainfall DECIMAL(12,4) NULL COMMENT '降雨量',
  alarm_flag INT NOT NULL DEFAULT 0 COMMENT '是否告警',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_dam_record_site_time (site_id, monitor_time),
  KEY idx_dam_record_level (tenant_id, safety_level, online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='坝体监测记录';

INSERT INTO sys_platform_integration_config (
  tenant_id, integration_code, integration_name, enabled, vendor_name, base_url, api_version, callback_path, ext_json, remark
)
SELECT 1, 'SSO', '统一身份认证', 0, '统一认证中心', 'https://sso.local', 'v1', '/auth/sso/callback', '{"ticketExpireSeconds":300}', '跨平台免密单点登录配置'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_platform_integration_config
  WHERE tenant_id = 1 AND integration_code = 'SSO' AND deleted = 0
);

INSERT INTO sys_platform_integration_config (
  tenant_id, integration_code, integration_name, enabled, vendor_name, base_url, api_version, callback_path, ext_json, remark
)
SELECT 1, 'VIDEO', '视频对接', 0, 'iSecure Center', 'https://video.local', 'iSecure Center V1.7.0+', '/platform/video/callback', '{"streamMode":"HLS","vendorOptions":["iSecure Center","海康综合安防","大华视频平台"]}', '项目/场地监控平台接入配置'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_platform_integration_config
  WHERE tenant_id = 1 AND integration_code = 'VIDEO' AND deleted = 0
);

INSERT INTO sys_platform_integration_config (
  tenant_id, integration_code, integration_name, enabled, vendor_name, base_url, api_version, callback_path, ext_json, remark
)
SELECT 1, 'DAM_MONITOR', '坝体监测', 0, '坝体安全监测设备', 'https://dam.local', 'v1', '/platform/dam/callback', '{"pollingMinutes":10}', '坝体安全情况数据对接预留'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_platform_integration_config
  WHERE tenant_id = 1 AND integration_code = 'DAM_MONITOR' AND deleted = 0
);
