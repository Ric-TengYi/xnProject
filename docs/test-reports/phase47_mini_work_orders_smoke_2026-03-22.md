# Phase 47 Mini Work Orders Smoke Test Report

## Scope
- `101 出土拍照`：拍照上传、车牌识别 mock、记录查询。
- `102 打卡异常申报`：创建异常申报并同步事件中心。
- `105 延期申报`：创建延期申请并同步事件中心。
- `109 问题反馈`：创建反馈、关闭反馈，并回写关联事件状态。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序工单接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序工单接口后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 工单接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 获取短信验证码 | `POST /api/mini/auth/send-sms-code` 返回 mockCode `576980` | PASS |
| 小程序登录 | `POST /api/mini/auth/login` 获取 JWT，场地数 `2` | PASS |
| 出土拍照创建 | `POST /api/mini/photos` 返回车牌 `浙A12345`、识别来源 `OCR_MOCK` | PASS |
| 打卡异常申报创建 | `POST /api/mini/checkin-exceptions` 返回关联事件 `26` | PASS |
| 延期申报创建 | `POST /api/mini/delay-applies` 返回关联事件 `27` | PASS |
| 问题反馈创建 | `POST /api/mini/feedbacks` 返回关联事件 `28` | PASS |
| 问题反馈关闭 | `PUT /api/mini/feedbacks/4/close` 状态更新为 `CLOSED` | PASS |
| 事件中心回查 | `GET /api/events?sourceChannel=MINI` 命中 `3` 条关联事件，`eventNo` 全部唯一 | PASS |

## Conclusion
- `101/102/105/109` 已形成可用后端闭环，拍照、申报、反馈均可直接通过 `/api/mini/*` 接口接入移动端。
- 本次修复将小程序事件号改为时间戳 + UUID 短后缀，消除了快速连续提交时的 `biz_manual_event.uk_manual_event_no` 冲突。
- 额外补强了 URL 编码车牌 mock 识别和带时区 ISO 时间解析，移动端提交兼容性更稳。
