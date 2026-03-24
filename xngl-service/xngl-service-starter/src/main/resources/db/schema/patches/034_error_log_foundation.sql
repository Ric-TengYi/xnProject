CREATE TABLE IF NOT EXISTS sys_error_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL COMMENT '租户ID',
  user_id BIGINT NULL COMMENT '用户ID',
  username VARCHAR(64) NULL COMMENT '用户名',
  level VARCHAR(16) NOT NULL DEFAULT 'ERROR' COMMENT '日志级别',
  exception_type VARCHAR(255) NULL COMMENT '异常类型',
  error_message VARCHAR(500) NULL COMMENT '异常消息',
  request_uri VARCHAR(255) NULL COMMENT '请求地址',
  http_method VARCHAR(16) NULL COMMENT '请求方法',
  ip VARCHAR(64) NULL COMMENT '客户端IP',
  stack_trace TEXT NULL COMMENT '异常堆栈',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_sys_error_log_tenant_time (tenant_id, create_time),
  KEY idx_sys_error_log_user_time (user_id, create_time),
  KEY idx_sys_error_log_level_time (level, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统错误日志';

INSERT INTO sys_error_log (
  tenant_id,
  user_id,
  username,
  level,
  exception_type,
  error_message,
  request_uri,
  http_method,
  ip,
  stack_trace
)
SELECT
  1,
  6,
  'admin',
  'ERROR',
  'SeedException',
  '初始化示例错误日志，供错误日志页面联调使用',
  '/api/system/bootstrap',
  'SYSTEM',
  '127.0.0.1',
  'SeedException: 初始化示例错误日志'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_error_log);
