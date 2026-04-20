# Phase 30 消息推送冒烟测试报告

- 测试时间：2026-03-22 12:32 CST
- 测试范围：`97 消息推送`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：12
- 通过：12
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API contract approval push：在线合同提交并审批通过后触发审批通知
- [PASS] API transfer rejection push：内拨申请提交并驳回后触发审批通知
- [PASS] API settlement approval push：项目结算提交并审批通过后触发审批通知
- [PASS] DB message records：3 类审批结果各生成 `SYSTEM + SMS` 两条消息记录，共 6 条
- [PASS] API message summary：消息汇总总数与未读数按预期增长
- [PASS] API message list：消息列表可回查本次新增审批通知
- [PASS] API mark read：标记已读后未读计数正确减少
- [PASS] UI page load：消息管理页面正常打开
- [PASS] UI contract message：页面可见合同审批通知 `HT-MSG-1774153918`
- [PASS] UI transfer message：页面可见内拨驳回通知 `NB202603221231581340`
- [PASS] UI settlement message：页面可见结算审批通知 `JS202603221231581126`

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
python3 - <<'PY'
# 真实走合同审批、内拨驳回、项目结算审批，验证 biz_message_record、/api/messages、/api/messages/summary
PY
python3 -u - <<'PY'
# 通过 localStorage 注入 token 打开 /messages，验证三类审批通知在页面可见
PY
```

## 结论

- 审批结果通知链路已打通，合同审批、内拨申请、项目结算在审批完成后都会写入 `biz_message_record`。
- 每个审批结果会同时生成 `SYSTEM` 和 `SMS` 两条消息记录，消息中心列表、汇总和已读处理均可正常工作。
