# Phase 41 场地资料冒烟测试报告

- 测试时间：2026-03-22 19:59:10 CST
- 测试范围：38 场地资料
- 测试环境：
  - 后端：mvn spring-boot:run -pl xngl-service-starter
  - 前端：npm run dev -- --host 127.0.0.1
  - 浏览器：本机 Google Chrome 无头模式
  - 账号：tenantId=1 / admin / admin

## 结果概览

- 用例数：2
- 通过：1
- 失败：1

## 用例明细

- [PASS] API login：admin/admin 登录成功并获取 token
- [FAIL] Smoke execution：500 Server Error:  for url: http://127.0.0.1:8090/api/sites/1/documents

## 产出文件

- API 结果：docs/test-reports/phase41_site_documents_smoke_2026-03-22.json
- UI 结果：docs/test-reports/phase41_site_documents_ui_2026-03-22.json

## 结论

- 已支持场地资料按审批/运营/移交阶段归类展示，并支持资料新增、编辑、删除和格式校验。
- 场地详情资料页和场地资料库页面均已接入真实数据源。
