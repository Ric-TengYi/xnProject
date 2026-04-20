# Phase 39 场地设备配置/运营配置冒烟测试报告

- 测试时间：2026-03-22 17:02:47 CST
- 测试范围：`44 设备配置`、`46 运营配置`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：7
- 通过：7
- 失败：0

## 用例明细

- [PASS] API login：admin/admin 登录成功并获取 token
- [PASS] API create site device：场地设备 SITE-DEV-AUTO-1774170159 创建成功
- [PASS] API update site device：场地设备 SITE-DEV-AUTO-1774170159 已更新为离线地磅
- [PASS] API update site operation config：场地运营配置已保存并返回新配置值
- [PASS] API site detail config echo：场地详情已回显新增设备与最新运营配置
- [PASS] UI site config page load：场地配置页成功打开并展示真实设备列表
- [PASS] UI operation config echo：运营配置表单已回显最新等待数、范围半径、时长和备注

## 产出文件

- API 结果：`docs/test-reports/phase39_site_device_operation_smoke_2026-03-22.json`
- UI 结果：`docs/test-reports/phase39_site_device_operation_ui_2026-03-22.json`

## 结论

- 已支持场地设备新增/编辑，设备列表会在场地详情配置页实时回显。
- 已支持场地运营配置保存，排号、人工消纳、范围检测和消纳时长规则已具备真实持久化。
