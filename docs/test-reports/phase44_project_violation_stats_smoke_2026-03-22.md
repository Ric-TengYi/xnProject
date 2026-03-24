# Phase 44 项目违法统计冒烟测试报告

- 测试时间：2026-03-22 20:34 CST
- 测试范围：31 项目管理-报表统计（按车队 / 车牌 / 处理中队分类统计）
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：9
- 通过：9
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API list vehicles：获取车辆 8 台
- [PASS] API disable vehicle 浙A97610：创建违规记录 `ID=7`
- [PASS] API disable vehicle 浙A42345：创建违规记录 `ID=8`
- [PASS] API release one violation：形成已处理 / 待处理混合统计样本
- [PASS] API violation analysis：`/api/reports/projects/violations` 返回总违规 `4`、车队 `2`、车牌 `2`、处理中队 `2`
- [PASS] UI route `/projects/reports`：页面可达
- [PASS] UI route `/projects/reports?periodType=DAY`：带周期参数页面可达
- [PASS] API cleanup release：测试车辆已解禁，不残留禁用状态

## 结论

- 已新增项目违法统计真实聚合接口，基于违规记录、车辆主数据和组织数据输出车队、车牌、处理中队三组排行统计。
- `ProjectsReports` 已补入违法统计区块，与项目报表时间筛选联动展示。

## 产出文件

- JSON：`docs/test-reports/phase44_project_violation_stats_smoke_2026-03-22.json`
