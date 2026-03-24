# Phase58 油电卡与车队财务报表专项烟测

- 日期：2026-03-23
- 覆盖需求：`130 油（电）卡管理`、`131 人证管理`、`132 财务管理`、`133 报表管理`
- 结果：`18 / 18 PASS`

## 覆盖内容
- 油电卡台账筛选、导出、充值、消费确认、流水查询与导出
- 人证费用/到期筛选与导出
- 车队财务管理汇总筛选、导出
- 车队报表筛选、导出与前端路由可达性

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| health | PASS | /api/health => UP |
| login | PASS | admin/admin 登录成功并获取 token |
| vehicle_cards_list | PASS | records=4 |
| vehicle_cards_summary | PASS | totalCards=4, totalBalance=8806.05 |
| vehicle_cards_export | PASS | bytes=612 |
| vehicle_card_recharge | PASS | card=CARD-ELEC-0002, balance=272.21 |
| vehicle_card_consume | PASS | card=CARD-ELEC-0002, balance=261.1 |
| vehicle_card_transactions_summary | PASS | total=4, recharge=133.32, consume=22.22 |
| vehicle_card_transactions_list | PASS | records=4 |
| vehicle_card_transactions_export | PASS | bytes=643 |
| vehicle_personnel_expire_summary | PASS | total=1, expiring=1 |
| vehicle_personnel_export | PASS | bytes=690 |
| fleet_finance_summary | PASS | totalRecords=2, profit=77900.0, outstanding=88000.0 |
| fleet_finance_export | PASS | bytes=484 |
| fleet_report | PASS | rows=9 |
| fleet_report_export | PASS | bytes=664 |
| ui_vehicle_cards_route | PASS | /vehicles/cards => 200 |
| ui_fleet_route | PASS | /vehicles/fleet => 200 |

## 验证命令
```bash
cd xngl-service && mvn -q -DskipTests compile
cd xngl-web && npm run build
mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/051_vehicle_card_transactions.sql
cd xngl-service && mvn -q -DskipTests install
cd xngl-service && mvn spring-boot:run -pl xngl-service-starter
python3 - <<'PY' ...
```
