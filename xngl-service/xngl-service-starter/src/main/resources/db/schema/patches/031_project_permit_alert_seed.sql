-- ============================================================
-- 031: 项目处置证有效期预警种子
-- ============================================================

INSERT INTO biz_alert_rule
  (tenant_id, rule_code, rule_name, rule_scene, metric_code, threshold_json, level, status, scope_type, remark)
SELECT
  1,
  'PROJECT_PERMIT_EXPIRING',
  '项目处置证有效期预警',
  'PROJECT',
  'permit_expire_days',
  '{"threshold":30,"unit":"day"}',
  'L2',
  'ENABLED',
  'GLOBAL',
  '项目关联处置证有效期提醒'
WHERE NOT EXISTS (
  SELECT 1 FROM biz_alert_rule WHERE tenant_id = 1 AND rule_code = 'PROJECT_PERMIT_EXPIRING'
);

INSERT INTO biz_alert_push_rule
  (tenant_id, rule_code, level, channel_types, receiver_type, receiver_expr, push_time_rule, escalation_minutes, status)
SELECT
  1,
  'PROJECT_PERMIT_EXPIRING',
  'L2',
  'INBOX,SMS',
  'ROLE',
  'manager,dispatcher',
  'IMMEDIATE',
  0,
  'ENABLED'
WHERE NOT EXISTS (
  SELECT 1
  FROM biz_alert_push_rule
  WHERE tenant_id = 1 AND rule_code = 'PROJECT_PERMIT_EXPIRING' AND level = 'L2'
);

INSERT INTO disposal_permit (
  tenant_id,
  permit_no,
  permit_type,
  project_id,
  contract_id,
  site_id,
  vehicle_no,
  issue_date,
  expire_date,
  approved_volume,
  used_volume,
  status,
  bind_status,
  remark
)
SELECT
  1,
  'PZ-2026-003',
  'DISPOSAL',
  c.project_id,
  c.id,
  c.site_id,
  '浙A67890',
  CURDATE(),
  DATE_ADD(CURDATE(), INTERVAL 10 DAY),
  3600.00,
  800.00,
  'EXPIRING',
  'BOUND',
  '项目处置证有效期预警演示数据'
FROM biz_contract c
WHERE c.tenant_id = 1
  AND c.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM disposal_permit WHERE permit_no = 'PZ-2026-003')
ORDER BY c.id
LIMIT 1;
