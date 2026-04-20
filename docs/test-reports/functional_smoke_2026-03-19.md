# 功能联调测试报告

- 生成时间：2026-03-20T09:02:28
- 前端地址：`http://127.0.0.1:5173`
- 后端地址：`http://127.0.0.1:8090/api`
- 汇总：26/26 通过，0 失败

## 结果明细

| 类型 | 用例 | 结果 | 说明 |
|---|---|---|---|
| api | health | PASS | {"code": 200, "message": "OK", "data": {"service": "xngl-service", "status": "UP"}} |
| api | login | PASS | admin/admin 登录成功 |
| api | contracts.list | PASS | code=200, records=5 |
| api | projects.list | PASS | code=200, records=0 |
| api | projectPayments.list | PASS | code=200, records=0 |
| api | reports.projects.daily | PASS | code=200, records=0 |
| api | reports.projects.ranking | PASS | code=200, records=0 |
| api | reports.sites.ranking | PASS | code=200, records=0 |
| api | sites.list | PASS | code=200, records=0 |
| api | contracts.detail | PASS | contractId=43 |
| api | contractReceipts.create | PASS | receiptId=37 |
| api | contractReceipts.list | PASS | records=4, foundCreated=True |
| api | contractReceipts.cancel | PASS | receiptId=37 |
| api | contractReceipts.verifyCancelled | PASS | status=CANCELLED |
| ui | / | PASS | url=http://127.0.0.1:5173/, h1=综合数据大屏 |
| ui | /dashboard/projects | PASS | url=http://127.0.0.1:5173/dashboard/projects, h1=项目数据看板 |
| ui | /dashboard/sites | PASS | url=http://127.0.0.1:5173/dashboard/sites, h1=消纳场数据看板 |
| ui | /projects | PASS | url=http://127.0.0.1:5173/projects, h1=消纳项目清单 |
| ui | /projects/payments | PASS | url=http://127.0.0.1:5173/projects/payments, h1=项目交款管理 |
| ui | /projects/daily-report | PASS | url=http://127.0.0.1:5173/projects/daily-report, h1=项目日报 |
| ui | /contracts | PASS | url=http://127.0.0.1:5173/contracts, h1=合同与财务结算 |
| ui | /contracts/payments | PASS | url=http://127.0.0.1:5173/contracts/payments, h1=合同入账管理 |
| ui | /contracts/monthly-report | PASS | url=http://127.0.0.1:5173/contracts/monthly-report, h1=月报统计 |
| ui | /sites | PASS | url=http://127.0.0.1:5173/sites, h1=消纳场地管理 |
| ui | /sites/disposals | PASS | url=http://127.0.0.1:5173/sites/disposals, h1=全局消纳清单 |
| ui | /sites/basic-info | PASS | url=http://127.0.0.1:5173/sites/basic-info, h1=场地基础信息 |

## 结论

- 本轮 API 与前端页面烟测全部通过。
- 当前数据库中 `projects` / `sites` / 报表相关接口返回为空，页面以空态通过校验；合同链路存在真实数据并已完成入账新增/查询/冲销验证。
- 后端启动时仍有 `schema-sync` SQL 语法告警，需要后续修正对应 patch 语句。