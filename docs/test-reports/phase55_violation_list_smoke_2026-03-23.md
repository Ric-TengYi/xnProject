# Phase55 违法清单专项烟测

- 日期：2026-03-23
- 覆盖需求：`30 违法清单`
- 结果：`8 / 8 PASS`

## 覆盖内容
- 违法清单分页与筛选
- 违法清单统计汇总
- 违法详情抽屉对应后端详情接口
- 违法禁用与提前解禁闭环

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| 登录获取 token | PASS | 管理员账号登录成功 |
| 违法汇总接口 | PASS | 返回总数、待处理、已处理、禁用中、已解禁、涉事车辆数 |
| 违法列表筛选 | PASS | `violationType=证件过期` 过滤命中 1 条 |
| 违法详情查询 | PASS | 返回单位、车型、司机、车队、速度、里程等信息 |
| 选择禁用车辆 | PASS | 选中可操作车辆 `浙A97610` |
| 创建禁用记录 | PASS | 新增闯禁区违法记录成功 |
| 禁用详情回查 | PASS | 新建记录详情可回查 |
| 提前解禁 | PASS | `releaseReason=phase55 release` |

## 验证命令
```bash
cd xngl-service && mvn -q -DskipTests compile
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
cd xngl-service && mvn spring-boot:run -pl xngl-service-starter
python3 phase55 smoke
```
