# phase65_vehicle_maintenance_detail_regression_2026-03-23

- 日期：2026-03-23
- 通过：8
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| maintenance_plan_create_runtime | PASS | plan_id=12 |
| maintenance_plan_detail_runtime | PASS | plan_no=MPLAN-1774270300579 |
| maintenance_execute_detail_runtime | PASS | record_id=6, cost=1300.0 |
| maintenance_record_detail_runtime | PASS | signoff=已签字 |
| maintenance_plan_history_runtime | PASS | history_count=1 |
| maintenance_record_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_maintenance_page_visible | PASS | 维保计划页面详情抽屉与执行历史可见 |