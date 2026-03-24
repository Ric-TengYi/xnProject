# Phase 25 线下合同录入冒烟测试报告

- 测试时间：2026-03-22 11:35 CST
- 测试范围：`6 线下合同录入`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：6
- 通过：6
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API offline contract create：创建线下三方补录合同 `HT-OFFLINE-API-1774150393` 成功，返回合同 ID=`47`
- [PASS] API offline status：线下补录合同详情状态为 `contractStatus=EFFECTIVE`、`approvalStatus=APPROVED`
- [PASS] API three-party binding：合同详情回查到 `sourceType=OFFLINE`、`isThreeParty=true`、`partyId=6`
- [PASS] API pricing fields：合同详情回查 `unitPriceInside=18.8`、`unitPriceOutside=21.6`
- [PASS] UI offline entry modal：合同清单页点击“线下合同录入”可打开补录弹窗，页面可见 `价格区域 / 区内单价 / 区外单价 / 三方合同` 字段

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# API 创建线下三方补录合同并回查详情
PY
python3 -u - <<'PY'
# 前端合同清单页打开线下合同录入弹窗，校验关键字段可见
PY
```

## 结论

- 线下合同录入已形成可用闭环：后端支持普通/三方补录、`partyId` 关联、区内/区外单价落库，且 `OFFLINE` 来源合同创建后直接进入 `APPROVED/EFFECTIVE` 状态。
- 前端合同清单页已接入线下补录入口弹窗，具备价格区域和三方合同所需字段，满足后续继续扩展审批/编辑链路的基础。
