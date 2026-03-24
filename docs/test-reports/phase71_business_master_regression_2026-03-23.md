# phase71_business_master_regression_2026-03-23

- 日期：2026-03-23
- 通过：11
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| business_units_summary_query_runtime | PASS | unit_id=13, total_units=8 |
| business_units_upsert_cleanup_runtime | PASS | temp_unit_id=21 |
| business_projects_list_detail_runtime | PASS | project_id=1, contracts=17 |
| business_project_payments_runtime | PASS | project_id=1, before_total=0, payment_id=6 |
| business_disposal_permits_runtime | PASS | permit_id=2035906049197776897, status=EXPIRING |
| business_checkins_void_restore_runtime | PASS | ticket_id=9, ticket_no=MD-20260323090251-B02E57 |
| business_disposals_query_runtime | PASS | disposal_id=9, ticket_no=MD-20260323090251-B02E57 |
| business_sites_runtime | PASS | site_id=1, map_layers=2 |
| business_vehicles_runtime | PASS | vehicles=8, companies=4, fleets=7 |
| business_master_pages_visible | PASS | 单位管理 / 消纳项目清单 / 项目交款管理 / 处置证清单 / 打卡数据 / 消纳信息 / 消纳场地管理 / 全局消纳清单 / 场地基础信息 / 车辆与运力资源 |