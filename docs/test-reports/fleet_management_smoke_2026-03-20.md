# Fleet Management Smoke 2026-03-20

- Passed: 15
- Failed: 0

- [PASS] api / login / admin/admin 登录成功
- [PASS] fleet / summary / totalFleets=7
- [PASS] fleet / profile_create / id=8 name=自动化车队01065143
- [PASS] fleet / profile_update / attendanceMode=AUTO
- [PASS] fleet / profile_query / total=1
- [PASS] fleet / plan_create / id=2 planNo=FTP-SMOKE-01065143
- [PASS] fleet / plan_update / status=COMPLETED
- [PASS] fleet / dispatch_create / id=2 orderNo=FDO-1774001065330
- [PASS] fleet / dispatch_approve / status=APPROVED
- [PASS] fleet / finance_create / id=2 recordNo=FFR-1774001065357
- [PASS] fleet / finance_update / status=SETTLED
- [PASS] fleet / report / matchedFleet=自动化车队01065143
- [PASS] frontend / index / status=200
- [PASS] frontend / route_registration / App.tsx 已注册车队管理路由
- [PASS] frontend / menu_registration / MainLayout 已注册车队管理菜单
