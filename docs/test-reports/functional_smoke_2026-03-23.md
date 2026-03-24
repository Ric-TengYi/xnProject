# 功能联调测试报告

- 生成时间：2026-03-23T11:39:19
- 前端地址：`http://127.0.0.1:5173`
- 后端地址：`http://127.0.0.1:8090/api`
- 汇总：19/19 通过，0 失败

## 结果明细

| 类型 | 用例 | 结果 | 说明 |
|---|---|---|---|
| api | health | PASS | `{"code":200,"message":"OK","data":{"service":"xngl-service","status":"UP"}}` |
| api | login | PASS | `admin/admin` 登录成功 |
| ui | /sites/documents | PASS | `status=200` |
| ui | /sites/reports | PASS | `status=200` |
| ui | /vehicles/violations | PASS | `status=200` |
| ui | /alerts | PASS | `status=200` |
| ui | /vehicles/fleet | PASS | `status=200` |
| api | siteDocuments.summary | PASS | `rows=2` |
| api | siteReports.summary | PASS | `periodType=CUSTOM`，`reportPeriod=2026-03-01 ~ 2026-03-23`，`siteCount=2` |
| api | siteReports.trend | PASS | `points=5` |
| api | violations.list | PASS | `records=5` |
| api | violations.summary | PASS | `totalCount=9`，`releasedCount=6` |
| api | violations.detail | PASS | `id=9`，`plateNo=浙A97610`，`fleetName=测试联调车队` |
| api | alerts.summary | PASS | 车辆预警汇总返回 `vehicleCount=3`、`highRiskCount=3` |
| api | alerts.analytics | PASS | `targetBuckets=1` |
| api | alerts.topRisk | PASS | `rows=2` |
| api | fleetTracking.summary | PASS | `totalVehicles=8`，`deliveringVehicles=1` |
| api | fleetTracking.list | PASS | `records=8` |
| api | fleetTracking.history | PASS | `浙A12345` 返回 `pointCount=4`、`totalDistanceKm=1.97` |

## 结论

- 近四个批次的关键模块已经形成闭环：场地资料/场地报表、违法清单、预警中心增强、送货跟踪均已完成后端接口、前端接入与统一回归。
- 当前开发服务与后端服务均基于最新代码运行，`FleetManagement` 已补齐车队维度的实时跟踪、轨迹回放和停留明细视图。
- 页面自动化浏览器烟测未纳入本报告；本次前端以构建通过和页面可达性校验为主。
