# Phase 26 单位统计冒烟测试报告

- 测试时间：2026-03-22 11:45 CST
- 测试范围：`8 单位统计`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：5
- 通过：5
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API unit detail：`/api/units/13` 返回单位详情成功
- [PASS] API unit projects：`/api/units/13/projects` 返回 1 条项目统计，项目名 `项目-001`
- [PASS] API unit contract groups：`/api/units/13/contract-groups?projectId=1` 返回 1 个按场地汇总分组，分组内包含合同明细数组
- [PASS] UI unit statistics drawer：单位管理页打开 `测试建设单位8917` 详情抽屉后，可见“项目列表”和“合同明细（按消纳场地汇总）”区块

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# API 校验单位项目统计与按场地汇总合同明细
PY
python3 -u - <<'PY'
# 页面打开单位详情抽屉并校验统计区块可见
PY
```

## 结论

- 单位统计已形成真实闭环：单位详情可查看项目列表，项目下合同明细可按消纳场地汇总展示。
- 前端单位详情抽屉已接入项目统计表和按场地分组的合同明细视图，满足需求中的单位维度钻取查看。
