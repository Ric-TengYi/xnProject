# Phase61 Plan5 待验收模块统一回归

- 日期：2026-03-23
- 覆盖需求：`Plan 5 第一批 = 平台对接 / 合同管理 / 处置证统一页面回归与补验收`
- 结果：`37 / 37 PASS`

## 覆盖内容
- 平台对接：概览、配置、视频通道、同步日志、SSO 票据、地磅记录
- 合同管理：合同清单/详情、导出、导入预检、变更/延期/内拨列表、入账、结算、月报
- 处置证：清单、详情、页面可达性

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功并获取 token |
| health | PASS | {'status': 'UP', 'service': 'xngl-service'} |
| platform_overview | PASS | enabled=1 |
| platform_configs | PASS | configs=5 |
| platform_video_channels | PASS | channels=2 |
| platform_sync_logs | PASS | logs=3 |
| platform_gov_sync_logs | PASS | govLogs=1 |
| platform_weighbridge_records | PASS | records=1 |
| platform_sso_ticket | PASS | ticket=572361d2..., target=PLAN5 |
| contracts_stats | PASS | effective=10 |
| contracts_list | PASS | records=17 |
| contracts_detail | PASS | contractNo=ON-1774233394-A |
| contracts_approval_records | PASS | records=1 |
| contracts_materials | PASS | materials=1 |
| contracts_invoices | PASS | invoices=0 |
| contracts_tickets | PASS | tickets=0 |
| contracts_receipts_by_contract | PASS | receipts=0 |
| contracts_export | PASS | taskId=32, file=contracts_20260323194139.csv, bytes=4195 |
| contracts_import_preview | PASS | batchId=8, valid=0, errors=1 |
| contract_change_list | PASS | records=1 |
| contract_extension_list | PASS | records=1 |
| contract_transfer_list | PASS | records=4 |
| contract_receipts_list | PASS | records=8 |
| settlements_stats | PASS | totalOrders=10 |
| settlements_list | PASS | records=10 |
| contract_monthly_summary | PASS | month=2026-03 |
| contract_monthly_trend | PASS | points=6 |
| contract_monthly_types | PASS | rows=3 |
| permits_list | PASS | records=12 |
| permits_detail | PASS | permitNo=PHASE52-P-1774232802 |
| ui_platform_integrations | PASS | title=平台对接中心, tables=4, url=http://127.0.0.1:5173/settings/platform-integrations |
| ui_contracts | PASS | title=合同与财务结算, tables=3, url=http://127.0.0.1:5173/contracts |
| ui_contract_transfers | PASS | title=内拨申请, tables=1, url=http://127.0.0.1:5173/contracts/transfers |
| ui_contract_payments | PASS | title=合同入账管理, tables=1, url=http://127.0.0.1:5173/contracts/payments |
| ui_settlements | PASS | title=结算管理, tables=1, url=http://127.0.0.1:5173/contracts/settlements |
| ui_monthly_report | PASS | title=月报统计, tables=1, url=http://127.0.0.1:5173/contracts/monthly-report |
| ui_project_permits | PASS | title=处置证清单, tables=1, url=http://127.0.0.1:5173/projects/permits |

## 验证命令
```bash
python3 scripts/phase61_plan5_pending_acceptance_regression.py
```