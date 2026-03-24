# phase72_contract_settlement_regression_2026-03-23

- 日期：2026-03-23
- 通过：6
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| contracts_list_detail_assets_runtime | PASS | contract_id=54, approvals=2, materials=2 |
| contracts_receipts_create_cancel_cleanup_runtime | PASS | receipt_id=42, before=0, after_cancel=1 |
| contracts_reports_runtime | PASS | export_task_id=36, trend_months=12 |
| site_settlements_runtime | PASS | existing_site_settlement=21, temp_site_settlement=31 |
| contracts_settlement_pages_visible | PASS | 合同与财务结算 / 合同详情: / 合同入账管理 / 月报统计 / 结算管理 |