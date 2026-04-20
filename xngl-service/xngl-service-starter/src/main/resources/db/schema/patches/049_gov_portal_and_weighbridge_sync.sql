SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'disposal_permit'
      AND column_name = 'source_platform'
  ),
  'SELECT 1',
  'ALTER TABLE disposal_permit ADD COLUMN source_platform VARCHAR(32) NULL COMMENT ''来源平台'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'disposal_permit'
      AND column_name = 'external_ref_no'
  ),
  'SELECT 1',
  'ALTER TABLE disposal_permit ADD COLUMN external_ref_no VARCHAR(64) NULL COMMENT ''外部证件/流水号'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'disposal_permit'
      AND column_name = 'last_sync_time'
  ),
  'SELECT 1',
  'ALTER TABLE disposal_permit ADD COLUMN last_sync_time DATETIME NULL COMMENT ''最近同步时间'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'disposal_permit'
      AND column_name = 'sync_batch_no'
  ),
  'SELECT 1',
  'ALTER TABLE disposal_permit ADD COLUMN sync_batch_no VARCHAR(64) NULL COMMENT ''同步批次号'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_platform_sync_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  integration_code VARCHAR(32) NOT NULL COMMENT '对接编码',
  sync_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '同步方式',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
  batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
  total_count INT NOT NULL DEFAULT 0 COMMENT '总记录数',
  success_count INT NOT NULL DEFAULT 0 COMMENT '成功数',
  fail_count INT NOT NULL DEFAULT 0 COMMENT '失败数',
  status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态',
  operator_id BIGINT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) NULL COMMENT '操作人',
  request_payload TEXT NULL COMMENT '请求载荷',
  response_payload TEXT NULL COMMENT '响应载荷',
  remark VARCHAR(500) NULL COMMENT '备注',
  sync_time DATETIME NOT NULL COMMENT '同步时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_platform_sync_log_code_time (tenant_id, integration_code, sync_time),
  KEY idx_platform_sync_log_batch (tenant_id, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台对接同步日志';

CREATE TABLE IF NOT EXISTS biz_weighbridge_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  device_id BIGINT NULL COMMENT '设备ID',
  vehicle_no VARCHAR(32) NOT NULL COMMENT '车牌号',
  ticket_no VARCHAR(64) NOT NULL COMMENT '过磅单号',
  gross_weight DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '毛重',
  tare_weight DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '皮重',
  net_weight DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '净重',
  estimated_volume DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '折算方量',
  weigh_time DATETIME NOT NULL COMMENT '过磅时间',
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED' COMMENT '同步状态',
  control_command VARCHAR(64) NULL COMMENT '本地控制命令',
  integration_code VARCHAR(32) NOT NULL DEFAULT 'WEIGHBRIDGE' COMMENT '对接编码',
  source_type VARCHAR(32) NOT NULL DEFAULT 'DEVICE' COMMENT '来源类型',
  remark VARCHAR(500) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_weighbridge_record_site_time (tenant_id, site_id, weigh_time),
  KEY idx_weighbridge_record_ticket (tenant_id, ticket_no),
  KEY idx_weighbridge_record_vehicle (tenant_id, vehicle_no, weigh_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地磅同步记录';

INSERT INTO sys_platform_integration_config (
  tenant_id, integration_code, integration_name, enabled, vendor_name, base_url, api_version, callback_path, ext_json, remark
)
SELECT 1, 'GOV_PORTAL', '政务网数据对接', 0, '浙江政务服务网', 'https://gov.local', 'v1', '/platform/gov/callback',
       '{"defaultPermitType":"DISPOSAL","syncTypes":["DISPOSAL","TRANSPORT"],"autoSyncCron":"0 0/30 * * * ?"}',
       '准运证与处置证政务网同步配置'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_platform_integration_config
  WHERE tenant_id = 1 AND integration_code = 'GOV_PORTAL' AND deleted = 0
);

INSERT INTO sys_platform_integration_config (
  tenant_id, integration_code, integration_name, enabled, vendor_name, base_url, api_version, callback_path, ext_json, remark
)
SELECT 1, 'WEIGHBRIDGE', '地磅数据对接', 0, '地磅控制器', 'https://weighbridge.local', 'v1', '/platform/weighbridge/callback',
       '{"controlMode":"LOCAL_AGENT","samplingSeconds":10}',
       '地磅采集与本地控制模块配置'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_platform_integration_config
  WHERE tenant_id = 1 AND integration_code = 'WEIGHBRIDGE' AND deleted = 0
);
