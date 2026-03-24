CREATE TABLE IF NOT EXISTS biz_site_operation_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  site_id BIGINT NOT NULL COMMENT '场地ID',
  queue_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启排号',
  max_queue_count INT NOT NULL DEFAULT 0 COMMENT '最大排队数量',
  manual_disposal_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许人工消纳',
  range_check_radius DECIMAL(10,2) NULL DEFAULT 0 COMMENT '范围检测半径',
  duration_limit_minutes INT NOT NULL DEFAULT 0 COMMENT '消纳时长限制',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_site_operation_config (tenant_id, site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地运营配置';

INSERT INTO biz_site_operation_config (
  tenant_id, site_id, queue_enabled, max_queue_count, manual_disposal_enabled, range_check_radius, duration_limit_minutes, remark
)
SELECT 1, 1, 1, 50, 1, 200, 45, '默认启用排号、人工消纳、范围检测和时长限制'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_site_operation_config WHERE tenant_id = 1 AND site_id = 1
);
