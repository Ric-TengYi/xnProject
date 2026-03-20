# Permit Approval Dict SysParam Smoke 2026-03-20

- Passed: 25
- Failed: 0

- [PASS] api / login / admin/admin 登录成功
- [PASS] seed / base_refs / contractId=43 projectId=1 siteId=1 vehicle=浙A97610
- [PASS] permit / create / id=2034941461304213505 contractId=43
- [PASS] permit / update / permitType=TRANSPORT
- [PASS] permit / query / records=1
- [PASS] approval / rule_create / id=6
- [PASS] approval / rule_update / ruleType=USER
- [PASS] approval / rule_status / DISABLED
- [PASS] approval / rule_query / records=2
- [PASS] approval / material_create / id=4
- [PASS] approval / material_update / materialType=ZIP
- [PASS] approval / material_status / DISABLED
- [PASS] approval / material_query / records=2
- [PASS] dict / create / id=2034941461845278722
- [PASS] dict / update / dictValue=AUTO2
- [PASS] dict / status / DISABLED
- [PASS] dict / query / records=1
- [PASS] sysparam / create / id=2034941462046605314
- [PASS] sysparam / update / paramType=JSON
- [PASS] sysparam / status / DISABLED
- [PASS] sysparam / query / records=1
- [PASS] cleanup / temp_records / approval/dict/sysparam 临时数据已清理
- [PASS] frontend / index / status=200
- [PASS] frontend / route_registration / 四个配置/处置证页面路由已注册
- [PASS] frontend / menu_registration / 四个配置/处置证页面菜单已注册
