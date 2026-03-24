# Phase56 预警中心规则增强专项烟测

- 日期：2026-03-23
- 覆盖需求：`53-62 预警中心增强`
- 结果：`10 / 10 PASS`

## 覆盖内容
- 预警汇总按筛选条件联动
- 预警分析按筛选条件联动
- Top Risk 支持目标类型和状态过滤
- 自动预警生成扩展到 `SITE_CAPACITY_WARN` 与 `VEHICLE_ROUTE_DEVIATION`

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| 登录获取 token | PASS | 管理员账号登录成功 |
| 规则加载 | PASS | 场地容量与车辆偏航规则存在 |
| 汇总过滤 | PASS | `targetType=VEHICLE` 汇总只保留车辆口径 |
| 分析过滤 | PASS | `targetBuckets` 仅包含 `VEHICLE` |
| 场地阈值临时下调 | PASS | 验证前将 `SITE_CAPACITY_WARN` 阈值改为 0 |
| 自动生成 | PASS | `SITE`、`VEHICLE` 两类目标纳入生成 |
| 场地容量预警生成 | PASS | 生成 3 条 `SITE_CAPACITY_WARN` 结果 |
| 车辆偏航预警生成 | PASS | 生成 3 条 `VEHICLE_ROUTE_DEVIATION` 结果 |
| Top Risk 过滤 | PASS | `targetType=VEHICLE`、`status=PENDING` 过滤正常 |
| 阈值恢复 | PASS | 场地容量规则阈值恢复到原始值 |

## 验证命令
```bash
cd xngl-service && mvn -q -DskipTests compile
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
cd xngl-service && mvn spring-boot:run -pl xngl-service-starter
python3 phase56 smoke
```
