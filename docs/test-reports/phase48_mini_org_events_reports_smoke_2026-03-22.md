# Phase 48 Mini Org / Events / Reports Smoke Test Report

## Scope
- `100 出土单位`：当前单位、项目列表、项目详情与项目消纳清单。
- `99 事件上报`：简易事件创建、列表、详情与审核日志。
- `106 统计报表`：项目/场地报表摘要、趋势、导出与分享链接。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序单位/事件/报表接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序单位/事件/报表接口后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 单位/事件/报表接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 当前出土单位 | `GET /api/mini/excavation-orgs/current` 返回单位 `浙江广源建设工程有限公司` | PASS |
| 单位项目列表 | `GET /api/mini/excavation-orgs/projects` 返回 `1` 个项目 | PASS |
| 项目详情/消纳清单 | `GET /api/mini/excavation-orgs/projects/1` 返回 `1` 个场地汇总 | PASS |
| 简易事件上报 | `POST /api/mini/events` 返回事件号 `ME-20260322215756-E7502A67` | PASS |
| 简易事件详情 | `GET /api/mini/events/30` 返回 `1` 条审核日志 | PASS |
| 项目报表摘要 | `GET /api/mini/reports/projects/summary` 返回期内方量 `1200.0` | PASS |
| 项目报表趋势 | `GET /api/mini/reports/projects/trend` 返回 `3` 个趋势点 | PASS |
| 场地报表摘要 | `GET /api/mini/reports/sites/summary` 返回期内方量 `1200.0` | PASS |
| 场地报表趋势 | `GET /api/mini/reports/sites/trend` 返回 `3` 个趋势点 | PASS |
| 项目报表导出/分享 | `POST /api/mini/reports/projects/export` + `GET /api/mini/reports/share-link/15` 返回分享下载路径 | PASS |
| 场地报表导出/分享 | `POST /api/mini/reports/sites/export` + `GET /api/mini/reports/share-link/16` 返回分享下载路径 | PASS |

## Conclusion
- `100/99/106` 已形成可用后端闭环，小程序已具备单位看项目、事件上报、项目/场地报表查询与导出分享能力。
- `100` 当前单位优先回显真实业务单位类型，避免管理员默认落到平台根组织。
- `106` 复用现有项目/场地报表与导出任务链路，小程序侧只做权限校验和轻量分享包装。
