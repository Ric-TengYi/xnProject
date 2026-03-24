# phase64_security_ledger_detail_regression_2026-03-23

- 日期：2026-03-23
- 通过：8
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| security_create_runtime | PASS | id=47, objectName=Demo Admin |
| security_filter_runtime | PASS | filtered_rows=1 |
| security_detail_timeline_runtime | PASS | timeline_count=1 |
| security_rectify_runtime | PASS | timeline_count=2 |
| security_export_runtime | PASS | text/csv;charset=UTF-8 |
| security_delete_runtime | PASS | deleted=47 |
| security_ledger_page_visible | PASS | 安全台账页面与详情时间线区块可见 |