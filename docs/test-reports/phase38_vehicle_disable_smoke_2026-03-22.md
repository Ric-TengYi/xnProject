# Phase 38 车辆禁用/解禁冒烟测试报告

- 测试时间：2026-03-22 16:42:35 CST
- 测试范围：`49 车辆禁用清单`、`50 车辆禁用/解禁`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：9
- 通过：9
- 失败：0

## 用例明细

- [PASS] API login：admin/admin 登录成功并获取 token
- [PASS] API violation list：违规车辆清单返回 5 条记录，包含禁用中状态
- [PASS] API disable vehicle：车辆 浙A97610 已创建禁用记录 6
- [PASS] API vehicle status after disable：车辆 浙A97610 状态已更新为禁用
- [PASS] API release vehicle：禁用记录 6 已提前解禁
- [PASS] API vehicle status after release：车辆 浙A97610 已恢复 ACTIVE 状态
- [PASS] API query released record：可按关键字回查自动化禁用/解禁记录
- [PASS] UI violation list page load：违规车辆清单页成功打开并展示真实违规记录
- [PASS] UI violation search filter：页面可按关键字检索并展示已解禁记录

## 产出文件

- API 结果：`docs/test-reports/phase38_vehicle_disable_smoke_2026-03-22.json`
- UI 结果：`docs/test-reports/phase38_vehicle_disable_ui_2026-03-22.json`

## 结论

- 已新增真实违规车辆清单与禁用记录表，支持按车牌/地点/状态筛选查询。
- 已支持单车禁用和提前解禁，且车辆主状态会随禁用/解禁动作同步更新。
