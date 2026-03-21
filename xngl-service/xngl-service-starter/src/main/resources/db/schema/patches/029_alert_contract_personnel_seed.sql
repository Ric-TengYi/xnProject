INSERT INTO sys_param
  (tenant_id, param_key, param_name, param_value, param_type, status, remark)
SELECT 1, 'alert.contract.expire.warn.days', '合同到期预警天数', '45', 'NUMBER', 'ENABLED', '合同预警默认值'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'alert.contract.expire.warn.days'
);

INSERT INTO sys_param
  (tenant_id, param_key, param_name, param_value, param_type, status, remark)
SELECT 1, 'alert.personnel.license.warn.days', '人员证照到期预警天数', '30', 'NUMBER', 'ENABLED', '人员预警默认值'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'alert.personnel.license.warn.days'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'CONTRACT_EXPIRING_SOON', '合同临期预警', 'CONTRACT', 'expire_days', '{"threshold":45,"unit":"day"}', 'L2', 'ENABLED', 'GLOBAL', '合同剩余期限提醒'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'CONTRACT_EXPIRING_SOON'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'CONTRACT_PAYMENT_OVERDUE', '合同应付款超期预警', 'CONTRACT', 'payment_overdue_days', '{"threshold":15,"unit":"day"}', 'L3', 'ENABLED', 'GLOBAL', '合同付款逾期提醒'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'CONTRACT_PAYMENT_OVERDUE'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'PERSONNEL_LICENSE_EXPIRING', '人员证照临期预警', 'USER', 'license_expire_days', '{"threshold":30,"unit":"day"}', 'L2', 'ENABLED', 'GLOBAL', '驾驶员证照到期提醒'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'PERSONNEL_LICENSE_EXPIRING'
);

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT 1, 'PERSONNEL_VIOLATION_SCORE', '人员违章风险预警', 'USER', 'violation_score', '{"threshold":70,"unit":"score"}', 'L3', 'ENABLED', 'GLOBAL', '针对习惯性违章驾驶行为'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'PERSONNEL_VIOLATION_SCORE'
);

INSERT INTO biz_alert_push_rule
  (tenant_id, rule_code, level, channel_types, receiver_type, receiver_expr, push_time_rule, escalation_minutes, status)
SELECT 1, 'CONTRACT_PAYMENT_OVERDUE', 'L3', 'INBOX,SMS', 'ROLE', 'finance,admin', 'IMMEDIATE', 30, 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_push_rule WHERE tenant_id = 1 AND rule_code = 'CONTRACT_PAYMENT_OVERDUE' AND level = 'L3'
);

INSERT INTO biz_alert_push_rule
  (tenant_id, rule_code, level, channel_types, receiver_type, receiver_expr, push_time_rule, escalation_minutes, status)
SELECT 1, 'PERSONNEL_VIOLATION_SCORE', 'L3', 'INBOX,SMS', 'ROLE', 'leader,safety_admin', 'IMMEDIATE', 10, 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_push_rule WHERE tenant_id = 1 AND rule_code = 'PERSONNEL_VIOLATION_SCORE' AND level = 'L3'
);

INSERT INTO biz_alert_event
  (tenant_id, alert_no, title, alert_type, rule_code, target_type, target_id, project_id, site_id, contract_id, level, alert_level, source_channel, content, snapshot_json, alert_status, status, occur_time)
SELECT
  1,
  'AL-20260321-001',
  CONCAT(COALESCE(c.contract_no, CONCAT('合同#', c.id)), ' 临期预警'),
  'CONTRACT_EXPIRE',
  'CONTRACT_EXPIRING_SOON',
  'CONTRACT',
  c.id,
  c.project_id,
  c.site_id,
  c.id,
  'L2',
  'L2',
  'SYSTEM',
  '合同剩余期限已进入预警窗口，需尽快安排续签或结算收口。',
  CONCAT('{"contractNo":"', COALESCE(c.contract_no, ''), '","expireDate":"', COALESCE(DATE_FORMAT(c.expire_date, '%Y-%m-%d'), ''), '"}'),
  'PENDING',
  0,
  DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM biz_contract c
WHERE c.tenant_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM biz_alert_event WHERE tenant_id = 1 AND alert_no = 'AL-20260321-001'
  )
ORDER BY c.id
LIMIT 1;

INSERT INTO biz_alert_event
  (tenant_id, alert_no, title, alert_type, rule_code, target_type, target_id, project_id, site_id, user_id, level, alert_level, source_channel, content, snapshot_json, alert_status, status, occur_time)
SELECT
  1,
  'AL-20260321-002',
  CONCAT(COALESCE(u.name, u.username, CONCAT('人员#', u.id)), ' 驾驶风险预警'),
  'PERSONNEL_RISK',
  'PERSONNEL_VIOLATION_SCORE',
  'USER',
  u.id,
  1,
  1,
  u.id,
  'L3',
  'L3',
  'SYSTEM',
  '人员安全风险评分连续升高，存在习惯性违章与证照临期风险。',
  CONCAT('{"userName":"', COALESCE(u.name, u.username, ''), '","score":78}'),
  'PENDING',
  0,
  DATE_SUB(NOW(), INTERVAL 90 MINUTE)
FROM sys_user u
WHERE u.tenant_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM biz_alert_event WHERE tenant_id = 1 AND alert_no = 'AL-20260321-002'
  )
ORDER BY u.id
LIMIT 1;
