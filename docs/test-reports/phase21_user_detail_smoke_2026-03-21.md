# Phase 21 User Detail Smoke (2026-03-21)

- Total: 10
- Passed: 10
- Failed: 0

## Checks
- [PASS] API login / `admin/admin` 登录成功
- [PASS] User seed / 创建详情测试用户成功
- [PASS] User detail query / 详情返回主组织、所属组织、角色关联展示
- [PASS] User update / 编辑接口更新姓名、手机号、邮箱、用户类型、组织、角色成功
- [PASS] User detail after update / 更新后详情正确回显新组织和新角色
- [PASS] User list after update / 列表已同步展示更新后的 `mainOrgName` 和 `roleNames`
- [PASS] User delete / 删除接口执行成功
- [PASS] User deleted invisible / 删除后详情返回 `404`
- [PASS] Frontend entry / `Organization` 已接入人员详情抽屉、编辑弹窗和删除确认
- [PASS] Build verification / backend `mvn -q -DskipTests install` 与 frontend `npm run build` 通过

## Notes
- 前端已在同一页面收口“新增、详情、编辑、删除”四类操作，减少来回跳页。
- 删除接口为逻辑删除，冒烟结束后已对测试账号执行物理清理。
- 前端校验点：`xngl-web/src/pages/Organization.tsx:451`、`xngl-web/src/pages/Organization.tsx:740`、`xngl-web/src/pages/Organization.tsx:808`。
