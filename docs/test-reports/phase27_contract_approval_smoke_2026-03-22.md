# Phase 27 合同审批冒烟测试报告

- 测试时间：2026-03-22 12:08 CST
- 测试范围：`9 合同审批`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：10
- 通过：10
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API contract create：创建审批测试合同 `HT-APPROVAL-APPROVE-1774151741`、`HT-APPROVAL-REJECT-1774151741`、`HT-APPROVAL-UI-1774151741`
- [PASS] API submit approval：提交审批后合同进入 `APPROVING`，同步生成审批记录和提交审批文书
- [PASS] API approve contract：审批通过后合同进入 `EFFECTIVE / APPROVED`，审批记录和通过文书同步落库
- [PASS] API material download：审批文书下载成功，内容包含合同编号与审批动作
- [PASS] API reject contract：审批驳回后合同进入 `REJECTED / REJECTED`，驳回原因回写详情并生成驳回文书
- [PASS] UI auth bootstrap：通过 `add_init_script` 预置 token 与用户信息，进入前端鉴权态
- [PASS] UI submit action：合同详情页提交审批后，可见“审批通过”动作按钮
- [PASS] UI approval timeline：审批流程页签展示提交审批时间线记录
- [PASS] UI materials list：办事材料页签展示系统自动生成的提交审批文书

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# API 创建审批测试合同，验证提交/通过/驳回、审批记录、文书下载
PY
python3 -u - <<'PY'
# 前端合同详情页验证提交审批后的按钮、时间线和材料列表
PY
```

## 结论

- 合同审批已形成真实闭环：`submit/approve/reject` 会写入 `biz_contract_approval_record`，并自动生成审批文书写入 `biz_contract_material`。
- 合同详情页已接入提交审批入口、审批后状态刷新、审批流程时间线和办事材料下载链路，满足“在线审批 + 文书生成”的当前需求。
