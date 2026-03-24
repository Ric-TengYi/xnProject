# Phase 50 Mini Vehicle / Safety Smoke Test Report

## Scope
- `104 手动消纳`：人工消纳记录创建、列表、详情与场地消纳清单联动。
- `111 车辆检查`：车辆检查记录创建、列表与详情。
- `112 车辆实时查询`：实时车辆列表、历史轨迹、停留点查询、电子围栏配置。
- `113 安全教育`：课程列表、学习开始、随机人脸校验、学习完成与记录查询。

## Build And Runtime
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序车辆/安全模块后端编译通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../048_mini_program_vehicle_and_safety.sql` | `048` patch 落库成功 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序车辆/安全模块安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 车辆/安全接口重启成功 | PASS |

## API Smoke
| Check | Result | Status |
|------|--------|--------|
| 手动消纳创建 | `POST /api/mini/manual-disposals` 创建记录 `1`，生成联动 ticket `9` | PASS |
| 手动消纳回查 | 列表、详情与 `GET /api/mini/sites/1/disposals` 均可回查新增记录 | PASS |
| 车辆实时列表 | `GET /api/mini/vehicles/realtime` 返回车辆 `7` 的实时位置 | PASS |
| 车辆轨迹与停留 | `GET /api/mini/vehicles/7/track-history` 返回 `1` 个轨迹点；停留查询接口返回成功 | PASS |
| 车辆检查 | `POST /api/mini/vehicle-inspections` 创建检查记录，列表/详情回查成功 | PASS |
| 电子围栏 | 创建并更新车辆围栏 `Phase50` 成功，列表可见 `1` 条围栏 | PASS |
| 安全教育课程 | `GET /api/mini/safety-education/courses` 返回已发布课程 | PASS |
| 安全教育学习 | 学习开始、人脸校验、完成学习与学习记录列表全链路通过 | PASS |

## Conclusion
- `104/111/112/113` 已完成小程序后端闭环，覆盖人工消纳补录、车辆检查、车辆实时查询与安全教育学习。
- `104` 已接入真实 `biz_contract_ticket`，人工消纳提交后会直接反映到小程序场地消纳清单。
- `112` 车辆实时查询复用既有车辆轨迹与围栏数据模型，移动端已可进行车辆实时位置、轨迹、停留点与电子围栏配置查询。
