# Phase 51 Gov Portal / Weighbridge Smoke Test Report

## Scope
- `33 处置证同步`：政务网 mock 同步处置证/准运证、同步日志落库与处置证列表回查。
- `92 政务网数据对接`：平台对接中心新增 `GOV_PORTAL` 配置、同步统计与日志查询。
- `94 地磅数据对接`：平台对接中心新增地磅记录同步、本地控制指令下发与日志回查。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-web && npm run build` | 平台对接中心与处置证页面前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/049_gov_portal_and_weighbridge_sync.sql` | `049` patch 落库成功 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 政务网/地磅模块后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带政务网/地磅接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 平台对接概览 | `GET /api/platform-integrations/overview` 返回 `GOV_PORTAL/WEIGHBRIDGE` 统计字段，初始 `govSyncCount=0`、`weighbridgeRecordCount=0` | PASS |
| 平台对接配置 | `GET /api/platform-integrations/configs` 返回 `SSO/VIDEO/DAM_MONITOR/GOV_PORTAL/WEIGHBRIDGE` 五类配置 | PASS |
| 政务网同步 | `POST /api/platform-integrations/gov/mock-sync` 生成批次 `GOV-20260323095450-C43572`，新增 2 条证件 | PASS |
| 政务网同步日志 | `GET /api/platform-integrations/gov/sync-logs` 可回查批次 `GOV-20260323095450-C43572` | PASS |
| 处置证回查 | `GET /api/disposal-permits` 与详情接口可回查 `sourcePlatform=GOV_PORTAL`、`externalRefNo`、`syncBatchNo`、`lastSyncTime` | PASS |
| 地磅记录同步 | `POST /api/platform-integrations/weighbridge/mock-sync` 新增记录 `WB-20260323095450`，净重 `24.4`、折算方量 `15.25` | PASS |
| 地磅记录列表 | `GET /api/platform-integrations/weighbridge/records?siteId=1` 可回查新增过磅单 | PASS |
| 地磅控制指令 | `POST /api/platform-integrations/weighbridge/control-command` 生成批次 `CTRL-20260323095450-738398`，返回 `SUCCESS` | PASS |
| 地磅同步日志 | `GET /api/platform-integrations/sync-logs?integrationCode=WEIGHBRIDGE` 同时回查同步批次和控制批次 | PASS |
| 概览计数回增 | 再次查询概览后，`govSyncCount=1`、`weighbridgeRecordCount=1` | PASS |

## Conclusion
- `33/92/94` 已完成前后端闭环，平台对接中心现已统一承载政务网证件同步、地磅记录同步和地磅控制指令。
- 处置证列表页已接入同步来源与批次字段，政务网同步产生的处置证可直接在列表和详情页回查。
- `049` patch 已兼容当前本地 MySQL 版本，不再依赖 `ADD COLUMN IF NOT EXISTS` 多列语法。
