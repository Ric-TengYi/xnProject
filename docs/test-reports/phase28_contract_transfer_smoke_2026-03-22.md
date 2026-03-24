# Phase 28 内拨申请冒烟测试报告

- 测试时间：2026-03-22 12:13 CST
- 测试范围：`13 内拨申请`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：8
- 通过：8
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API create transfer：内拨申请创建成功，状态 `DRAFT`
- [PASS] API transfer list/detail：列表与详情接口均可回查内拨申请，返回源/目标合同号
- [PASS] API approve transfer：提交并审批通过后，申请状态变为 `APPROVED`，源/目标合同金额和方量同步联动调整
- [PASS] API reject transfer：提交并驳回后，申请状态变为 `REJECTED`，驳回原因被保留
- [PASS] UI page load：`/contracts/transfers` 页面正常打开
- [PASS] UI create modal：点击“发起内拨申请”可打开创建弹窗
- [PASS] UI detail drawer：点击表格“详情”可打开内拨申请详情抽屉

## 执行命令

```bash
cd xngl-web && npm run build
python3 - <<'PY'
# API 验证内拨申请创建、详情、提交、通过、驳回与合同联动调整
PY
python3 -u - <<'PY'
# 前端验证内拨申请页面、创建弹窗与详情抽屉
PY
```

## 结论

- 内拨申请已形成可用闭环：后端支持列表、创建、详情、提交、通过、驳回，并在审批通过后同步调整源合同和目标合同的金额/方量。
- 前端已新增独立“内拨申请”页面，接入列表、创建弹窗、详情抽屉和状态动作入口，满足当前需求中“申请列表、创建、详情、状态流转”的落地目标。
