# Phase 29 项目结算冒烟测试报告

- 测试时间：2026-03-22 12:18 CST
- 测试范围：`14 项目结算`
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

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API generate project settlement：项目结算单生成成功
- [PASS] API settlement detail：详情返回 `contractSummaries` 合同汇总数组和带 `contractNo` 的结算明细
- [PASS] API settlement list：项目结算列表支持按项目筛选并回查新生成结算单
- [PASS] API settlement approval：项目结算单提交并审批通过后，状态变为 `SETTLED / APPROVED`
- [PASS] UI page load：结算管理页面正常打开
- [PASS] UI contract summary：项目结算详情抽屉可见关联合同编号

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "INSERT INTO biz_contract_ticket ..."
python3 - <<'PY'
# API 生成项目结算单，验证合同汇总、列表、审批流转
PY
python3 -u - <<'PY'
# 前端结算管理页打开项目结算详情，校验关联合同汇总展示
PY
```

## 结论

- 项目结算已具备真实生成、列表、详情、审批流转能力，并补齐了“按关联合同汇总展示”的缺口。
- 结算详情接口现在会返回 `contractSummaries` 和明细行的 `contractNo`，前端详情抽屉已展示关联合同编号，满足需求中的“按关联合同汇总展示，并进行消纳数据结算”。
