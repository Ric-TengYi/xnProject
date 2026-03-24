# phase69_alerts_events_unified_regression_2026-03-23

- 日期：2026-03-23
- 通过：13
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| manual_event_create_runtime | PASS | event_id=45 |
| manual_event_update_runtime | PASS | phase69 事件联调-1774270275265-更新 |
| manual_event_flow_runtime | PASS | audit_logs=8 |
| manual_event_summary_export_runtime | PASS | text/csv;charset=UTF-8 |
| alerts_generate_runtime | PASS | created=1, updated=3, closed=0 |
| alerts_query_runtime | PASS | alert_id=23, total_rows=4 |
| alerts_handle_close_runtime | PASS | 浙A12345 线路偏航预警 |
| alerts_summary_analytics_export_runtime | PASS | summary_total=23, fences=9 |
| alert_rule_flow_runtime | PASS | rule_id=18 |
| alert_fence_flow_runtime | PASS | fence_id=14 |
| alert_push_flow_runtime | PASS | push_id=15 |
| alerts_events_pages_visible | PASS | 预警与监控中心 / 事件管理 / 预警配置 / 安全台账管理 |