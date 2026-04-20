# Phase 49 Mini Account Security Smoke Test Report

## Scope
- `107 密码修改`：短信验证码改密、旧密码校验改密。
- `108 账号绑定`：微信 `openId` 绑定、绑定后免密登录。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序账号安全接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序账号安全接口后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 密码修改/账号绑定接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 登录验证码发送 | `POST /api/mini/auth/send-sms-code` 返回手机号 `13800000001` 与 mockCode | PASS |
| 小程序账号登录 | `POST /api/mini/auth/login` 返回 token、用户信息与 `2` 个可访问场地 | PASS |
| 改密验证码发送 | `POST /api/mini/account/send-password-code` 返回 `PASSWORD_CHANGE` 验证码 | PASS |
| 短信验证码改密 | `POST /api/mini/account/password` 使用短信码将密码改为 `Admin@20260322` 成功 | PASS |
| 新密码二次登录 | 使用新密码重新发送验证码并登录成功 | PASS |
| 微信账号绑定 | `POST /api/mini/account/bind` 成功绑定 `mini-openid-admin-phase49` | PASS |
| OpenID 免密登录 | `POST /api/mini/auth/openid-login` 成功直接换取登录 token | PASS |
| 旧密码校验改密 | `POST /api/mini/account/password` 使用 `oldPassword` 将密码恢复为 `admin` 成功 | PASS |
| 原密码恢复验证 | 使用恢复后的 `admin` 再次发送验证码并登录成功 | PASS |

## Conclusion
- `107/108` 已形成完整后端闭环，小程序支持短信验证码改密、旧密码改密、微信账号绑定和 OpenID 免密登录。
- 本次烟测使用本地管理员账号完成往返改密，并已在测试结束后恢复为原密码 `admin`。
