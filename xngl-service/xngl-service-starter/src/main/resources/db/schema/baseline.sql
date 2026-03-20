-- 基线表结构：每次启动校对时，缺失则创建；已有表不会改动
-- 大表（如车辆 GPS）的字段缺失时仅提示不执行，见配置 app.schema-sync.warn-only-tables

CREATE TABLE IF NOT EXISTS schema_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  script_name VARCHAR(255) NOT NULL UNIQUE,
  checksum VARCHAR(64),
  executed_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_org (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  code VARCHAR(50),
  parent_id BIGINT,
  tenant_id BIGINT,
  org_code VARCHAR(64),
  org_name VARCHAR(128),
  org_type VARCHAR(32),
  org_path VARCHAR(500),
  leader_user_id BIGINT,
  leader_name_cache VARCHAR(128),
  contact_person VARCHAR(64),
  contact_phone VARCHAR(32),
  address VARCHAR(255),
  unified_social_code VARCHAR(64),
  remark VARCHAR(500),
  sort_order INT DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50),
  name VARCHAR(100),
  description VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255),
  name VARCHAR(100),
  mobile VARCHAR(20),
  org_id BIGINT,
  status INT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_project (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200),
  code VARCHAR(50),
  address VARCHAR(500),
  status INT DEFAULT 0,
  org_id BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_site (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200),
  code VARCHAR(50),
  address VARCHAR(500),
  project_id BIGINT,
  status INT DEFAULT 0,
  org_id BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_site_settlement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  site_id BIGINT NOT NULL,
  settlement_no VARCHAR(64) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  settlement_date DATE NOT NULL,
  total_volume DECIMAL(18,2) NOT NULL DEFAULT 0,
  unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  adjust_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  settlement_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
  remark VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_biz_site_settlement_no (settlement_no),
  KEY idx_biz_site_settlement_site_status_date (site_id, settlement_status, settlement_date),
  KEY idx_biz_site_settlement_site_period (site_id, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_vehicle (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT,
  plate_no VARCHAR(20),
  vin VARCHAR(50),
  org_id BIGINT,
  vehicle_type VARCHAR(64),
  brand VARCHAR(64),
  model VARCHAR(64),
  energy_type VARCHAR(32),
  axle_count INT,
  dead_weight DECIMAL(10,2),
  load_weight DECIMAL(10,2),
  driver_name VARCHAR(64),
  driver_phone VARCHAR(32),
  fleet_name VARCHAR(128),
  captain_name VARCHAR(64),
  captain_phone VARCHAR(32),
  status INT DEFAULT 0,
  use_status VARCHAR(32) DEFAULT 'ACTIVE',
  running_status VARCHAR(32) DEFAULT 'STOPPED',
  current_speed DECIMAL(10,2),
  current_mileage DECIMAL(12,2),
  next_maintain_date DATE,
  annual_inspection_expire_date DATE,
  insurance_expire_date DATE,
  lng DECIMAL(10,7),
  lat DECIMAL(10,7),
  gps_time DATETIME,
  remark VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_biz_vehicle_tenant_status (tenant_id, status),
  KEY idx_biz_vehicle_org_status (org_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_contract (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50),
  name VARCHAR(200),
  project_id BIGINT,
  party_id BIGINT,
  amount DECIMAL(18,2),
  status INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_alert_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200),
  level VARCHAR(20),
  related_id BIGINT,
  related_type VARCHAR(50),
  status INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  module VARCHAR(50),
  action VARCHAR(50),
  content VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
