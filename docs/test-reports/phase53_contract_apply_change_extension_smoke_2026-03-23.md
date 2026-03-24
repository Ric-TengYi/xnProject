# Phase 53 Contract Apply / Change / Extension Smoke Test Report

## Scope
- `10 消纳合同发起`：在线普通合同、三方合同创建与提交审批，退回原因回查。
- `11 变更合同发起`：场地变更与部分方量变更申请、提交、审批通过后合同回写。
- `12 消纳延期`：延期申请、提交、审批通过后到期日和方量回写。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 合同申请/变更/延期后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 合同管理页在线申请/变更/延期入口前端构建通过 | PASS |
| `python3 - <<'PY' ...` | 在线合同、变更申请、延期申请核心链路 8/8 通过 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 在线普通合同发起 | 创建合同 `ON-*-A` 后提交审批成功，状态变为 `APPROVING` | PASS |
| 在线三方合同发起 | 创建三方合同 `ON-*-B` 后提交审批成功，`isThreeParty=true` | PASS |
| 审批退回原因 | 三方合同执行驳回后，详情接口返回 `rejectReason=资料不完整，需补充三方说明` | PASS |
| 变更申请发起 | 对合同 `54` 创建场地+方量变更申请成功 | PASS |
| 变更申请审批 | 审批通过后合同场地改为 `2`，方量改为 `878.0`，`changeVersion=1` | PASS |
| 延期申请发起 | 对合同 `54` 创建延期申请成功，延期到 `2027-01-15` 并增补方量 | PASS |
| 延期申请审批 | 审批通过后合同到期日更新为 `2027-01-15`，合同方量更新为 `903.0` | PASS |
| 申请列表回查 | 变更/延期列表均可按 `contractId` 查询到最新 `APPROVED` 记录 | PASS |

## Conclusion
- `10/11/12` 当前主链路已在合同管理页完成入口接入，并通过 API + 前端 build 验证在线发起、退回原因展示、变更审批和延期审批闭环。
- 合同在线发起使用 `sourceType=ONLINE` 创建，并在页面中直接提交审批；三方合同和驳回原因链路已可回查。
- 场地变更与部分方量变更采用统一变更申请入口，延期申请支持延期到期日和增补方量。
