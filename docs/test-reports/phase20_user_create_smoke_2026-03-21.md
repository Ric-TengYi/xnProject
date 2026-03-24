# Phase 20 User Create Smoke (2026-03-21)

- Total: 9
- Passed: 9
- Failed: 0

## Checks
- [PASS] API login / `admin/admin` 登录成功
- [PASS] User create / 新增人员接口成功创建测试账号
- [PASS] User detail binding / 详情返回主组织名称、所属组织列表、角色列表、`needResetPassword=1`
- [PASS] User relation endpoints / `/users/{id}/roles`、`/users/{id}/orgs` 可用
- [PASS] User list binding / 列表返回 `mainOrgName` 和 `roleNames`
- [PASS] Default password login / 未传密码时默认初始密码 `123456` 可登录
- [PASS] Frontend entry / `Organization` 已接入“新增人员”弹窗、主组织/角色选择和默认密码提示
- [PASS] Build verification / backend `mvn -q -DskipTests install` 通过
- [PASS] Build verification / frontend `npm run build` 通过

## Notes
- 人员创建链路已支持组织、角色关系同步落库，列表和详情都能直接回显组织名称与角色名称。
- API 删除为逻辑删除，冒烟结束后已额外执行物理清理，避免测试账号残留。
- 前端校验点：`xngl-web/src/pages/Organization.tsx:528`、`xngl-web/src/pages/Organization.tsx:636`、`xngl-web/src/pages/Organization.tsx:696`。
