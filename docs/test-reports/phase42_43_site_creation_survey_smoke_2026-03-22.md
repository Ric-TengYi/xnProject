# Phase 42/43 场地创建与场地测绘冒烟测试报告

- 测试时间：2026-03-22 20:27 CST
- 测试范围：42 场地创建、43 场地测绘
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：11
- 通过：11
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API list sites：当前场地 1 个
- [PASS] API create secondary site：新建二级场地 `ID=2`，上级场地与借用地磅均回填为 `场地-001`
- [PASS] API site detail echo：`siteLevel / parentSiteName / weighbridgeSiteName` 回显正确
- [PASS] API create survey：测绘记录 `ID=1` 创建成功，结算方量自动计算为 `525.0`
- [PASS] API update survey：扣减方量修改后结算方量自动重算为 `500.0`，状态更新为 `CONFIRMED`
- [PASS] API list surveys：场地测绘列表返回最新记录
- [PASS] API delete survey：测绘记录删除成功
- [PASS] UI route `/sites`：页面可达
- [PASS] UI route `/sites/2?tab=survey`：测绘页签路由可达
- [PASS] UI route `/sites/basic-info`：基础信息页可达

## 结论

- 已完成二级场地创建、上级场地关联、借用地磅场地配置，以及详情页借用地磅/层级信息展示。
- 已完成场地测绘记录的新增、编辑、删除与结算方量自动计算，并在 `SiteDetail` 新增“场地测绘”页签。

## 产出文件

- JSON：`docs/test-reports/phase42_43_site_creation_survey_smoke_2026-03-22.json`
