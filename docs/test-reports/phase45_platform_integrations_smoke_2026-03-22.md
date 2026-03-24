# Phase 45/93/95 Smoke Test Report

## Scope
- `45 坝体数据对接`：坝体监测配置预留、mock 同步、记录查询
- `93 视频对接`：视频平台配置、监控设备通道聚合
- `95 平台对接`：统一身份认证 SSO 票据生成与换取登录态

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 平台对接后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 平台对接后端安装打包通过 | PASS |
| `cd xngl-web && npm run build` | 平台对接前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../045_platform_integration_foundation.sql"` | 平台对接基础表和默认配置落库成功 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启成功，应用正常连库 | PASS |
| `cd xngl-web && npm run dev` | 前端开发服务启动成功，`5173` 可访问 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 管理员登录 | `admin/admin` 获取 JWT 成功 | PASS |
| 对接概览 | 返回 `enabledCount/totalCount/videoChannelCount/onlineDamSiteCount/activeSsoTicketCount` | PASS |
| 对接配置列表 | 返回 `SSO / VIDEO / DAM_MONITOR` 三类配置 | PASS |
| 视频配置更新 | `VIDEO` 配置写入并回读成功 | PASS |
| SSO 票据换登 | 生成票据后调用 `/api/auth/sso/exchange` 成功换取 admin 登录态 | PASS |
| 视频通道查询 | 基于场地设备聚合出 2 条视频通道 | PASS |
| 坝体 mock 同步 | 对场地 `1` 写入一条 `WARNING + alarmFlag=true` 记录并可回查 | PASS |

## Frontend Smoke
| Check | Result | Status |
|------|--------|--------|
| 路由可达 | `http://127.0.0.1:5173/settings/platform-integrations` 返回前端应用壳 | PASS |
| 浏览器自动化 | 当前机器缺少 Playwright Chromium 内核，`BrowserType.launch` 失败 | BLOCKED |

## Conclusion
- 45/93/95 已完成后端能力、前端页面、菜单路由和基础联调。
- 关键业务链路已通过 API 冒烟验证：SSO 票据、视频通道、坝体 mock 同步均可用。
- 剩余缺口仅是浏览器级自动化环境未就绪，不影响当前功能交付判断。
