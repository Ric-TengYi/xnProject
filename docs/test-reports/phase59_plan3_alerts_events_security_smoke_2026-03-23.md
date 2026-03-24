# Phase59 预警中心、事件管理、安全台账增强包烟测

- 日期：2026-03-23
- 覆盖需求：`Plan 3 = 预警中心 / 事件管理 / 安全台账增强`
- 结果：`17 / 17 PASS`

## 覆盖内容
- 预警中心：规则编码筛选、时间区间筛选、超期筛选、详情、导出、自动预警刷新
- 事件管理：筛选联动汇总、超期/时间区间筛选、详情、导出、附件字段透传
- 安全台账：危险等级/超期/时间区间筛选、联动汇总、详情、导出、附件字段透传

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| health | PASS | /api/health => {'service': 'xngl-service', 'status': 'UP'} |
| login | PASS | admin/admin 登录成功并获取 token |
| alerts_generate | PASS | created=0, updated=4, closed=0 |
| alerts_list | PASS | records=15 |
| alerts_filtered_summary | PASS | filteredTotal=2, ruleCode=VEHICLE_ROUTE_DEVIATION |
| alerts_detail | PASS | alertId=15 |
| alerts_export | PASS | bytes=767 |
| events_create | PASS | eventId=31 |
| events_filtered_list | PASS | records=1 |
| events_filtered_summary | PASS | filteredTotal=1, overdue=1 |
| events_detail | PASS | auditLogs=1 |
| events_export | PASS | bytes=540 |
| security_create | PASS | inspectionId=20 |
| security_filtered_list | PASS | records=1 |
| security_filtered_summary | PASS | overdueRectify=1, failCount=1 |
| security_detail | PASS | title=Plan3安全台账烟测-1774245388 |
| security_export | PASS | bytes=596 |

## 验证命令
```bash
cd xngl-service && mvn -q -DskipTests compile -pl xngl-service-web -am
cd xngl-web && npm run build
cd xngl-service && mvn -q -DskipTests install
cd xngl-service && mvn spring-boot:run -pl xngl-service-starter
python3 - <<'PY' ...
```