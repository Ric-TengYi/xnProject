# Phase 52 Disposal Permit Manual / Binding Smoke Test Report

## Scope
- `34 处置证新增`：手工新增、编辑、来源标记与主数据自动回填。
- `35 处置证关联`：项目/合同/场地/车辆关联校验，多维筛选与详情回查。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 处置证新增/关联模块后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 处置证清单页面前端构建通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 处置证新增/关联模块后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带处置证新增/关联增强接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 合同主数据选择 | 成功选取合同 `54`，项目 `1`，场地 `1` 作为关联校验基准 | PASS |
| 车辆主数据选择 | 成功选取车辆 `8`，车牌 `浙A97610` | PASS |
| 手工新增处置证 | `POST /api/disposal-permits` 创建 `PHASE52-P-*` 成功，自动回填项目/场地，来源标记为 `MANUAL` | PASS |
| 详情回查 | `GET /api/disposal-permits/{id}` 返回 `projectId=1`、`siteId=1`、`bindStatus=BOUND` | PASS |
| 多维筛选 | `GET /api/disposal-permits` 支持 `projectId/contractId/siteId/bindStatus/sourcePlatform/vehicleNo/keyword` 组合查询并命中新增记录 | PASS |
| 编辑处置证 | 更新后证件类型改为 `TRANSPORT`，状态按到期日自动计算为 `EXPIRING` | PASS |
| 合同/项目一致性校验 | 合同与项目不匹配时返回 `400` 和明确错误信息 | PASS |
| 车辆存在性校验 | 绑定不存在车辆时返回 `400` 和明确错误信息 | PASS |

## Conclusion
- `34/35` 已补齐手工新增处置证的真实主数据关联校验，合同可自动回填项目和场地，手工新增默认来源标记为 `MANUAL`。
- 处置证清单页已改为服务端真实筛选，支持项目、合同、场地、来源平台、绑定状态和车辆等维度组合查询。
- 当前这批已完成后端编译、前端 build、服务重启和专项 smoke，但页面级自动化回归仍待后续统一验收批次处理。
