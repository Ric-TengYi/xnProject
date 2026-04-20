ALTER TABLE biz_alert_event ADD COLUMN tenant_id BIGINT NULL DEFAULT 1 AFTER id;
ALTER TABLE biz_alert_event ADD COLUMN alert_no VARCHAR(64) NULL AFTER tenant_id;
ALTER TABLE biz_alert_event ADD COLUMN alert_type VARCHAR(64) NULL AFTER title;
ALTER TABLE biz_alert_event ADD COLUMN rule_code VARCHAR(64) NULL AFTER alert_type;
ALTER TABLE biz_alert_event ADD COLUMN target_type VARCHAR(32) NULL AFTER rule_code;
ALTER TABLE biz_alert_event ADD COLUMN target_id BIGINT NULL AFTER target_type;
ALTER TABLE biz_alert_event ADD COLUMN project_id BIGINT NULL AFTER target_id;
ALTER TABLE biz_alert_event ADD COLUMN site_id BIGINT NULL AFTER project_id;
ALTER TABLE biz_alert_event ADD COLUMN vehicle_id BIGINT NULL AFTER site_id;
ALTER TABLE biz_alert_event ADD COLUMN user_id BIGINT NULL AFTER vehicle_id;
ALTER TABLE biz_alert_event ADD COLUMN contract_id BIGINT NULL AFTER user_id;
ALTER TABLE biz_alert_event ADD COLUMN alert_level VARCHAR(20) NULL AFTER level;
ALTER TABLE biz_alert_event ADD COLUMN source_channel VARCHAR(32) NULL AFTER contract_id;
ALTER TABLE biz_alert_event ADD COLUMN content VARCHAR(1000) NULL AFTER source_channel;
ALTER TABLE biz_alert_event ADD COLUMN latest_position_json VARCHAR(1000) NULL AFTER content;
ALTER TABLE biz_alert_event ADD COLUMN snapshot_json VARCHAR(2000) NULL AFTER latest_position_json;
ALTER TABLE biz_alert_event ADD COLUMN handle_remark VARCHAR(500) NULL AFTER snapshot_json;
ALTER TABLE biz_alert_event ADD COLUMN alert_status VARCHAR(32) NULL DEFAULT 'PENDING' AFTER handle_remark;
ALTER TABLE biz_alert_event ADD COLUMN occur_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER alert_status;
ALTER TABLE biz_alert_event ADD COLUMN resolve_time DATETIME NULL AFTER occur_time;

CREATE TABLE IF NOT EXISTS sys_data_dict (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  dict_type VARCHAR(64) NOT NULL,
  dict_code VARCHAR(64) NOT NULL,
  dict_label VARCHAR(128) NOT NULL,
  dict_value VARCHAR(128) NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_data_dict_code (tenant_id, dict_type, dict_code),
  KEY idx_sys_data_dict_type_status (tenant_id, dict_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_param (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  param_key VARCHAR(128) NOT NULL,
  param_name VARCHAR(128) NOT NULL,
  param_value VARCHAR(1000) NULL,
  param_type VARCHAR(64) NOT NULL DEFAULT 'STRING',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_param_key (tenant_id, param_key),
  KEY idx_sys_param_type_status (tenant_id, param_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  rule_scene VARCHAR(64) NOT NULL,
  metric_code VARCHAR(64) NOT NULL,
  threshold_json VARCHAR(1000) NULL,
  level VARCHAR(32) NOT NULL DEFAULT 'L2',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  scope_type VARCHAR(32) NOT NULL DEFAULT 'GLOBAL',
  remark VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_alert_rule_code (tenant_id, rule_code),
  KEY idx_alert_rule_scene_status (tenant_id, rule_scene, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_alert_fence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  rule_code VARCHAR(64) NULL,
  fence_code VARCHAR(64) NOT NULL,
  fence_name VARCHAR(128) NOT NULL,
  fence_type VARCHAR(32) NOT NULL,
  geo_json VARCHAR(2000) NULL,
  buffer_meters DECIMAL(10,2) NULL DEFAULT 0,
  biz_scope VARCHAR(255) NULL,
  active_time_range VARCHAR(64) NULL,
  direction_rule VARCHAR(32) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_alert_fence_code (tenant_id, fence_code),
  KEY idx_alert_fence_rule_status (tenant_id, rule_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_alert_push_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  rule_code VARCHAR(64) NOT NULL,
  level VARCHAR(32) NOT NULL,
  channel_types VARCHAR(255) NOT NULL,
  receiver_type VARCHAR(32) NOT NULL DEFAULT 'ROLE',
  receiver_expr VARCHAR(255) NOT NULL,
  push_time_rule VARCHAR(64) NULL,
  escalation_minutes INT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_alert_push_rule_level (tenant_id, level, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_manual_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  event_no VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(2000) NULL,
  source_channel VARCHAR(32) NOT NULL DEFAULT 'WEB',
  project_id BIGINT NULL,
  site_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  reporter_id BIGINT NULL,
  reporter_name VARCHAR(64) NULL,
  priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  current_audit_node VARCHAR(64) NULL,
  occur_time DATETIME NULL,
  report_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  close_time DATETIME NULL,
  close_remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_manual_event_no (tenant_id, event_no),
  KEY idx_manual_event_status_time (tenant_id, status, report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_manual_event_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  event_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  action VARCHAR(32) NOT NULL,
  result_status VARCHAR(32) NOT NULL,
  auditor_id BIGINT NULL,
  auditor_name VARCHAR(64) NULL,
  comment VARCHAR(500) NULL,
  audit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  KEY idx_manual_event_audit_event (tenant_id, event_id, audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_security_inspection (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  inspection_no VARCHAR(64) NOT NULL,
  object_type VARCHAR(32) NOT NULL,
  object_id BIGINT NULL,
  title VARCHAR(200) NOT NULL,
  check_scene VARCHAR(64) NULL,
  check_type VARCHAR(64) NULL,
  result_level VARCHAR(32) NOT NULL DEFAULT 'PASS',
  issue_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  project_id BIGINT NULL,
  site_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  user_id BIGINT NULL,
  inspector_id BIGINT NULL,
  inspector_name VARCHAR(64) NULL,
  description VARCHAR(2000) NULL,
  rectify_deadline DATETIME NULL,
  rectify_remark VARCHAR(500) NULL,
  rectify_time DATETIME NULL,
  check_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  next_check_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_security_inspection_no (tenant_id, inspection_no),
  KEY idx_security_inspection_type_status (tenant_id, object_type, status, check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT 1, 'alert_level', 'L1', '低风险', 'L1', 1, 'ENABLED', '系统默认'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'alert_level' AND dict_code = 'L1'
);

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT 1, 'alert_level', 'L2', '中风险', 'L2', 2, 'ENABLED', '系统默认'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'alert_level' AND dict_code = 'L2'
);

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT 1, 'alert_level', 'L3', '高风险', 'L3', 3, 'ENABLED', '系统默认'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'alert_level' AND dict_code = 'L3'
);

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT 1, 'event_type', 'DELAY', '延期申报', 'DELAY', 1, 'ENABLED', '事件管理'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'event_type' AND dict_code = 'DELAY'
);

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT 1, 'event_type', 'SITE', '场地事件', 'SITE', 2, 'ENABLED', '事件管理'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'event_type' AND dict_code = 'SITE'
);

INSERT INTO sys_data_dict
  (tenant_id, dict_type, dict_code, dict_label, dict_value, sort, status, remark)
SELECT 1, 'event_type', 'REPORT', '违规举报', 'REPORT', 3, 'ENABLED', '事件管理'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_data_dict WHERE tenant_id = 1 AND dict_type = 'event_type' AND dict_code = 'REPORT'
);

INSERT INTO sys_param
  (tenant_id, param_key, param_name, param_value, param_type, status, remark)
SELECT 1, 'permit.expire.warn.days', '处置证到期预警天数', '30', 'NUMBER', 'ENABLED', '系统参数'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'permit.expire.warn.days'
);

INSERT INTO sys_param
  (tenant_id, param_key, param_name, param_value, param_type, status, remark)
SELECT 1, 'alert.vehicle.deviation.meters', '车辆偏航预警阈值', '200', 'NUMBER', 'ENABLED', '预警配置默认值'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'alert.vehicle.deviation.meters'
);

INSERT INTO sys_param
  (tenant_id, param_key, param_name, param_value, param_type, status, remark)
SELECT 1, 'security.site.inspect.cycle.days', '场地安全巡检周期', '7', 'NUMBER', 'ENABLED', '安全台账默认值'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'security.site.inspect.cycle.days'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'SITE_CAPACITY_WARN', '场地容量预警', 'SITE', 'capacity_usage', '{"threshold":80,"unit":"%"}', 'L2', 'ENABLED', 'GLOBAL', '系统默认'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'SITE_CAPACITY_WARN'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'VEHICLE_ROUTE_DEVIATION', '车辆偏航预警', 'VEHICLE', 'deviation_meter', '{"threshold":200,"unit":"m"}', 'L3', 'ENABLED', 'GLOBAL', '系统默认'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'VEHICLE_ROUTE_DEVIATION'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'PROJECT_PROGRESS_LAG', '项目进度预警', 'PROJECT', 'progress_rate', '{"threshold":65,"unit":"%"}', 'L2', 'ENABLED', 'GLOBAL', '系统默认'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'PROJECT_PROGRESS_LAG'
);

INSERT INTO biz_alert_fence
  (tenant_id, rule_code, fence_code, fence_name, fence_type, geo_json, buffer_meters, biz_scope, active_time_range, direction_rule, status)
SELECT 1, 'VEHICLE_ROUTE_DEVIATION', 'FENCE-EAST-001', '东区临时消纳场(入场)', 'ENTRY', '{"center":[120.169,30.267],"radius":350}', 50, 'SITE:1', '06:00-22:00', 'IN', 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_fence WHERE tenant_id = 1 AND fence_code = 'FENCE-EAST-001'
);

INSERT INTO biz_alert_fence
  (tenant_id, rule_code, fence_code, fence_name, fence_type, geo_json, buffer_meters, biz_scope, active_time_range, direction_rule, status)
SELECT 1, 'VEHICLE_ROUTE_DEVIATION', 'FENCE-ROAD-001', '环南路停留超时监控', 'STAY', '{"polyline":[[120.170,30.268],[120.176,30.271]]}', 80, 'ROAD:RING_SOUTH', '00:00-23:59', 'BOTH', 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_fence WHERE tenant_id = 1 AND fence_code = 'FENCE-ROAD-001'
);

INSERT INTO biz_alert_push_rule
  (tenant_id, rule_code, level, channel_types, receiver_type, receiver_expr, push_time_rule, escalation_minutes, status)
SELECT 1, 'VEHICLE_ROUTE_DEVIATION', 'L3', 'INBOX,SMS,WEBHOOK', 'ROLE', 'admin,leader', 'IMMEDIATE', 15, 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_push_rule WHERE tenant_id = 1 AND rule_code = 'VEHICLE_ROUTE_DEVIATION' AND level = 'L3'
);

INSERT INTO biz_alert_push_rule
  (tenant_id, rule_code, level, channel_types, receiver_type, receiver_expr, push_time_rule, escalation_minutes, status)
SELECT 1, 'PROJECT_PROGRESS_LAG', 'L2', 'INBOX,SMS', 'ROLE', 'manager', 'IMMEDIATE', 0, 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_push_rule WHERE tenant_id = 1 AND rule_code = 'PROJECT_PROGRESS_LAG' AND level = 'L2'
);

INSERT INTO biz_manual_event
  (tenant_id, event_no, event_type, title, content, source_channel, project_id, site_id, vehicle_id, reporter_id, reporter_name, priority, status, current_audit_node, occur_time, report_time)
SELECT 1, 'EV-20260320-001', 'DELAY', '项目因暴雨申请延期 3 天', '受连续暴雨影响，现场无法正常出土，申请延长处置周期。', 'WEB', 1, 1, 1, 1, 'Demo Admin', 'HIGH', 'PENDING_AUDIT', 'MANUAL_EVENT_AUDIT', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_manual_event WHERE tenant_id = 1 AND event_no = 'EV-20260320-001'
);

INSERT INTO biz_manual_event
  (tenant_id, event_no, event_type, title, content, source_channel, project_id, site_id, reporter_id, reporter_name, priority, status, occur_time, report_time)
SELECT 1, 'EV-20260320-002', 'SITE', '东区消纳场消防设施检查异常', '巡检发现入口 2 号灭火器压力不足，需要更换。', 'WEB', 1, 1, 1, 'Demo Admin', 'MEDIUM', 'PROCESSING', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 36 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_manual_event WHERE tenant_id = 1 AND event_no = 'EV-20260320-002'
);

INSERT INTO biz_manual_event_audit_log
  (tenant_id, event_id, node_code, action, result_status, auditor_id, auditor_name, comment, audit_time)
SELECT 1, e.id, 'MANUAL_EVENT_AUDIT', 'SUBMIT', 'PENDING_AUDIT', 1, 'Demo Admin', '提交审核', e.report_time
FROM biz_manual_event e
WHERE e.tenant_id = 1
  AND e.event_no = 'EV-20260320-001'
  AND NOT EXISTS (
    SELECT 1 FROM biz_manual_event_audit_log l WHERE l.tenant_id = 1 AND l.event_id = e.id AND l.action = 'SUBMIT'
  );

INSERT INTO biz_security_inspection
  (tenant_id, inspection_no, object_type, object_id, title, check_scene, check_type, result_level, issue_count, status, project_id, site_id, inspector_id, inspector_name, description, rectify_deadline, check_time, next_check_time)
SELECT 1, 'SEC-SITE-20260320-001', 'SITE', 1, '东区临时消纳场消防设施检查', 'FIRE', 'ROUTINE', 'FAIL', 1, 'OPEN', 1, 1, 1, 'Demo Admin', '入口 2 号灭火器压力不足，需立即更换。', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_security_inspection WHERE tenant_id = 1 AND inspection_no = 'SEC-SITE-20260320-001'
);

INSERT INTO biz_security_inspection
  (tenant_id, inspection_no, object_type, object_id, title, check_scene, check_type, result_level, issue_count, status, vehicle_id, inspector_id, inspector_name, description, check_time, next_check_time)
SELECT 1, 'SEC-VEH-20260320-001', 'VEHICLE', 1, '浙A12345 出车前安全检查', 'VEHICLE_OPERATION', 'SHIFT', 'PASS', 0, 'CLOSED', 1, 1, 'Demo Admin', '车辆灯光、轮胎、随车证件检查通过。', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_security_inspection WHERE tenant_id = 1 AND inspection_no = 'SEC-VEH-20260320-001'
);

INSERT INTO biz_security_inspection
  (tenant_id, inspection_no, object_type, object_id, title, check_scene, check_type, result_level, issue_count, status, user_id, inspector_id, inspector_name, description, rectify_deadline, check_time, next_check_time)
SELECT 1, 'SEC-PER-20260320-001', 'PERSON', 1, '现场司机劳保穿戴检查', 'PERSONNEL', 'RANDOM', 'FAIL', 1, 'RECTIFYING', 1, 1, 'Demo Admin', '发现反光背心未按要求穿戴，已责令整改。', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_security_inspection WHERE tenant_id = 1 AND inspection_no = 'SEC-PER-20260320-001'
);

INSERT INTO biz_alert_event
  (tenant_id, alert_no, title, alert_type, rule_code, target_type, target_id, project_id, site_id, vehicle_id, contract_id, level, alert_level, source_channel, content, latest_position_json, snapshot_json, alert_status, status, occur_time)
SELECT 1, 'AL-20260320-001', '浙A12345 偏航预警', 'ROUTE_DEVIATION', 'VEHICLE_ROUTE_DEVIATION', 'VEHICLE', 1, 1, 1, 1, 39, 'L3', 'L3', 'GPS', '车辆偏离预设线路 260 米，存在违规运输风险。', '{"lng":120.171,"lat":30.269}', '{"vehicle":"浙A12345","project":"项目-001","site":"场地-001"}', 'PENDING', 0, DATE_SUB(NOW(), INTERVAL 15 MINUTE)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_event WHERE tenant_id = 1 AND alert_no = 'AL-20260320-001'
);

INSERT INTO biz_alert_event
  (tenant_id, alert_no, title, alert_type, rule_code, target_type, target_id, project_id, site_id, vehicle_id, level, alert_level, source_channel, content, snapshot_json, alert_status, status, occur_time)
SELECT 1, 'AL-20260320-002', '场地容量预警', 'SITE_CAPACITY', 'SITE_CAPACITY_WARN', 'SITE', 1, 1, 1, 1, 'L2', 'L2', 'SITE_MONITOR', '场地容量使用率已达到 82%，建议控制进场节奏。', '{"site":"场地-001","capacityUsage":82}', 'PROCESSING', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_event WHERE tenant_id = 1 AND alert_no = 'AL-20260320-002'
);

INSERT INTO biz_alert_event
  (tenant_id, alert_no, title, alert_type, rule_code, target_type, target_id, project_id, level, alert_level, source_channel, content, snapshot_json, alert_status, status, occur_time)
SELECT 1, 'AL-20260320-003', '项目进度预警', 'PROJECT_PROGRESS', 'PROJECT_PROGRESS_LAG', 'PROJECT', 1, 1, 'L2', 'L2', 'REPORT', '项目近 7 日消纳进度低于计划值，需核查资源调度。', '{"project":"项目-001","planRate":75,"actualRate":61}', 'PENDING', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_event WHERE tenant_id = 1 AND alert_no = 'AL-20260320-003'
);
