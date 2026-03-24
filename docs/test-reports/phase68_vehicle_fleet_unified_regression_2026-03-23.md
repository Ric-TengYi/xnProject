# phase68_vehicle_fleet_unified_regression_2026-03-23

- 日期：2026-03-23
- 通过：29
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| vehicle_master_list_runtime | PASS | vehicles_total=8 |
| vehicle_master_stats_runtime | PASS | stats_keys=7 |
| vehicle_master_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_model_list_runtime | PASS | vehicle_models=3 |
| vehicle_model_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_insurance_list_runtime | PASS | insurance_total=4 |
| vehicle_insurance_summary_runtime | PASS | expiring=1, expired=1 |
| vehicle_insurance_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_maintenance_plans_runtime | PASS | maintenance_plans=8 |
| vehicle_maintenance_summary_runtime | PASS | plans=8, overdue=1 |
| vehicle_maintenance_records_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_repair_list_runtime | PASS | repairs_total=9 |
| vehicle_repair_summary_runtime | PASS | approved=1, completed=6 |
| vehicle_repair_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_cards_list_runtime | PASS | vehicle_cards=4 |
| vehicle_cards_summary_runtime | PASS | balance=8861.6 |
| vehicle_cards_transaction_summary_runtime | PASS | recharge=4333.32, consume=702.22 |
| vehicle_cards_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_cards_transaction_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_personnel_list_runtime | PASS | personnel_certificates=4 |
| vehicle_personnel_summary_runtime | PASS | unpaid=3400.0 |
| vehicle_personnel_export_runtime | PASS | text/csv;charset=UTF-8 |
| fleet_finance_list_runtime | PASS | fleet_finance=2 |
| fleet_finance_summary_runtime | PASS | outstanding=88000.0, profit=77900.0 |
| fleet_finance_export_runtime | PASS | text/csv;charset=UTF-8 |
| fleet_report_runtime | PASS | fleet_report_rows=9 |
| fleet_report_export_runtime | PASS | text/csv;charset=UTF-8 |
| vehicle_fleet_pages_visible | PASS | 车辆与运力资源 / 车型管理 / 维保计划 / 维修管理 / 油电卡管理 / 人证管理 / 车队管理 |