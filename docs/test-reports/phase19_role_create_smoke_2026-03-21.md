# Phase 19 Role Create Smoke (2026-03-21)

- Total: 9
- Passed: 9
- Failed: 0

## Checks
- [PASS] API login / `admin/admin` 登录成功，`userId=6`
- [PASS] Role create / 创建临时角色成功，默认 `status=ENABLED`
- [PASS] Role detail / 详情返回 `roleScope=TENANT`、`dataScopeTypeDefault=ORG_AND_CHILDREN`
- [PASS] Permission defaults / `/roles/{id}/permissions` 默认返回空菜单和空权限集合
- [PASS] Data scope defaults / `/roles/{id}/data-scope-rules` 默认返回 `ALL + ORG_AND_CHILDREN`
- [PASS] DB verify / `sys_data_scope_rule` 已真实落库 `biz_module=ALL scope_type=ORG_AND_CHILDREN`
- [PASS] Frontend entry / 角色页路由与“新增角色”弹窗已接入 `RolesManagement`
- [PASS] Build verification / backend `mvn -q -DskipTests install` 通过
- [PASS] Build verification / frontend `npm run build` 通过

## Notes
- 冒烟过程中发现 `DataScopeRule` 实体仍把兼容字段映射成真实列，导致查询 `rule_type/rule_value/resource_code` 报错；已修复为非持久化字段后回归通过。
- 冒烟创建了临时角色并完成库表校验，随后已清理测试数据。
- 前端入口校验点：`xngl-web/src/App.tsx:102`、`xngl-web/src/pages/RolesManagement.tsx:167`、`xngl-web/src/pages/RolesManagement.tsx:246`。
