# Phase 40 场地人员配置冒烟测试报告

- 测试时间：2026-03-22 17:52:20 CST
- 测试范围：47 人员配置
- 测试环境：
  - 后端：mvn spring-boot:run -pl xngl-service-starter
  - 前端：npm run dev -- --host 127.0.0.1
  - 浏览器：本机 Google Chrome 无头模式
  - 账号：tenantId=1 / admin / admin

## 结果概览

- 用例数：8
- 通过：8
- 失败：0

## 用例明细

- [PASS] API login：admin/admin 登录成功并获取 token
- [PASS] API personnel candidates：候选人员 11 条，可用测试账号 test999
- [PASS] API create site personnel：场地人员 test999 创建成功，ID=5
- [PASS] API update site personnel：场地人员 test999 已更新为巡检员并停用
- [PASS] API site personnel echo：场地人员列表已回显最新岗位、班次和启停状态
- [PASS] API delete site personnel：场地人员 test999 已删除并完成清理
- [PASS] UI site config page load：场地配置页成功打开并展示人员配置卡片
- [PASS] UI site personnel echo：页面已回显 test999 的巡检员和停用状态

## 产出文件

- API 结果：docs/test-reports/phase40_site_personnel_config_smoke_2026-03-22.json
- UI 结果：docs/test-reports/phase40_site_personnel_config_ui_2026-03-22.json

## 结论

- 已支持场地人员基于系统用户进行绑定配置，支持新增、编辑、删除和状态启停。
- 场地详情配置页已回显真实人员账号、岗位、组织、班次和职责信息。
