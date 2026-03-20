# 功能联调测试报告

- 生成时间：2026-03-20T09:36:35
- 前端地址：`http://127.0.0.1:5173`
- 后端地址：`http://127.0.0.1:8090/api`
- 汇总：16/16 通过，0 失败

## 结果明细

| 类型 | 用例 | 结果 | 说明 |
|---|---|---|---|
| ui | / | PASS | status=200 |
| ui | /projects | PASS | status=200 |
| ui | /sites | PASS | status=200 |
| ui | /contracts/settlements | PASS | status=200 |
| api | health | PASS | {"code": 200, "message": "OK", "data": {"status": "UP", "service": "xngl-service"}} |
| api | login | PASS | admin/admin 登录成功 |
| api | projects.list | PASS | records=1 |
| api | sites.list | PASS | records=1 |
| api | reports.projects.daily | PASS | records=1 |
| api | reports.projects.ranking | PASS | records=1 |
| api | reports.sites.ranking | PASS | records=1 |
| api | settlements.stats | PASS | {"pendingAmount": 0, "settledAmount": 0.0, "totalOrders": 4, "draftOrders": 0, "pendingOrders": 0, "settledOrders": 2} |
| api | settlements.list | PASS | records=4 |
| api | settlements.detail | PASS | id=18, status=SETTLED |
| api | settlements.site.rejectFlow | PASS | approvalStatus=REJECTED, settlementStatus=REJECTED |
| api | settlements.project.approveFlow | PASS | approvalStatus=APPROVED, settlementStatus=SETTLED |

## 结论

- `projects` / `sites` 主数据与项目/场地报表接口已返回真实数据，不再是空态。
- 结算主链路已完成接口烟测：列表、详情、统计、项目结算审批通过、场地结算驳回均已验证。
- 前端开发服务可访问，结算页已改为真实 API 驱动；本次对前端以可达性与构建通过为主。
