# 功能联调测试报告（P1/P2 增量）

- 生成时间：2026-03-20T11:09:34
- 前端地址：`http://127.0.0.1:5173`
- 后端地址：`http://127.0.0.1:8090/api`
- 汇总：11/11 通过，0 失败

## 结果明细

| 类型 | 用例 | 结果 | 说明 |
|---|---|---|---|
| ui | /contracts/settlements | PASS | status=200 |
| ui | /projects/permits | PASS | status=200 |
| ui | /sites | PASS | status=200 |
| api | login | PASS | admin/admin 登录成功 |
| api | sites.detail.rules | PASS | siteType=STATE_OWNED, capacity=400000.0 |
| api | reports.projects.daily.nonzero | PASS | totalVolume=1200.0 |
| api | reports.sites.ranking.nonzero | PASS | used=1200.0 |
| api | settlements.site.classified | PASS | amount=46800.0, items=1 |
| api | settlements.project.lines | PASS | amount=37500.0, items=1 |
| api | permits.list | PASS | records=2 |
| api | permits.detail | PASS | permitNo=PZ-2026-001, status=ACTIVE |

## 结论

- 场地结算已支持分类规则、非零金额计算与明细行生成。
- 项目/场地报表已基于自动回填领票数据返回非零累计量。
- 处置证清单已接入真实接口与种子数据，可完成列表/详情联调。
