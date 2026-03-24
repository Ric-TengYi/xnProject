# Phase 46 Mini Login / Sites Smoke Test Report

## Scope
- `110 小程序登录`：平台账号密码 + 手机短信二次校验登录
- `98 小程序消纳清单`：场地维度消纳清单查询
- `103 小程序消纳场`：场地基础信息与消纳数据查看

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序登录/场地接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序登录/场地接口后端安装打包通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../046_mini_program_auth_and_sites.sql"` | `mini_user_binding`、`mini_sms_code_record` 落库成功 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带最新 mini 接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 获取短信验证码 | `POST /api/mini/auth/send-sms-code` 返回 mockCode | PASS |
| 小程序登录 | `POST /api/mini/auth/login` 通过账号密码 + 短信码获取 JWT | PASS |
| 当前用户信息 | `GET /api/mini/me` 返回绑定状态 `BOUND` 与 openId | PASS |
| 可访问场地列表 | `GET /api/mini/sites` 返回 2 个可访问场地及当日统计 | PASS |
| 场地详情 | `GET /api/mini/sites/{siteId}` 返回基础信息、设备在线数和消纳统计 | PASS |
| 场地消纳清单 | `GET /api/mini/sites/{siteId}/disposals` 返回真实分页记录，总数 `6` | PASS |

## Conclusion
- 110/98/103 已形成可用后端闭环，补齐了小程序专属登录、场地权限与站点数据接口。
- 当前仓库没有独立小程序前端工程，因此本批交付以 `/api/mini/*` 接口为主，供后续小程序或 H5 容器直接接入。
