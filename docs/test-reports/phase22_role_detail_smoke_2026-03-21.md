# Phase 22 Role Detail Smoke (2026-03-21)

- Total: 11
- Passed: 11
- Failed: 0

## Checks
- [PASS] API login / `admin/admin` 登录成功
- [PASS] Role seed / 创建角色详情测试数据成功
- [PASS] Menu seed / 获取权限树菜单节点成功
- [PASS] Role detail query / 角色详情查询成功
- [PASS] Role update / 角色基础信息编辑成功
- [PASS] Role permission scope save / 权限树和数据范围保存成功
- [PASS] Role detail after update / 详情正确回显更新后的角色范围、描述、默认数据范围
- [PASS] Role permissions backfill / 权限树回显和数据范围回显正确
- [PASS] Role delete / 角色删除成功
- [PASS] Role deleted invisible / 删除后详情返回 `404`
- [PASS] Frontend entry + build / `RolesManagement` 已接入详情描述、编辑角色、删除角色，且前后端构建通过

## Notes
- 修复了角色数据范围更新时的唯一索引冲突：`sys_data_scope_rule` 改为按 `role_id` 物理删除后重建，避免逻辑删除残留占用唯一键。
- 前端校验点：`xngl-web/src/pages/RolesManagement.tsx:269`、`xngl-web/src/pages/RolesManagement.tsx:289`、`xngl-web/src/pages/RolesManagement.tsx:324`。
- 冒烟过程中遗留的旧测试角色已统一清理。
