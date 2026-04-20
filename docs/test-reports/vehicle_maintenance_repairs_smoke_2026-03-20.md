# Vehicle Maintenance / Repairs Smoke 2026-03-20

- Passed: 17
- Failed: 0
- Note: 本机未安装 Playwright 浏览器，前端冒烟改为通过 dev server 与 Vite 转译源码校验路由、菜单和入口可访问性。

- [PASS] api / login / admin/admin 登录成功
- [PASS] api / vehicles_seed / vehicleId=8 plateNo=浙A97610
- [PASS] maintenance / summary / totalPlans=2
- [PASS] maintenance / create_plan / planNo=MPLAN-1773997138588
- [PASS] maintenance / update_plan / 烟测保养-更新
- [PASS] maintenance / execute_plan / recordNo=MREC-1773997138634
- [PASS] maintenance / records_query / records=1
- [PASS] repair / summary / totalOrders=2
- [PASS] repair / create_order / orderNo=REP-1773997138704
- [PASS] repair / update_order / MEDIUM
- [PASS] repair / approve_order / APPROVED
- [PASS] repair / complete_order / COMPLETED
- [PASS] repair / create_reject_order / orderNo=REP-1773997138757
- [PASS] repair / reject_order / REJECTED
- [PASS] frontend / dev_server_index / GET / 返回 index.html 且天地图脚本已注入
- [PASS] frontend / /vehicles/maintenance / Vite 源码路由已注册
- [PASS] frontend / /vehicles/repairs / Vite 源码路由与菜单已注册
