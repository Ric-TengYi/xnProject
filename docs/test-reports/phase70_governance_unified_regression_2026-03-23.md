# phase70_governance_unified_regression_2026-03-23

- 日期：2026-03-23
- 通过：16
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| governance_role_create_runtime | PASS | role_id=15 |
| governance_role_update_runtime | PASS | Phase70 角色-更新 |
| governance_org_create_update_runtime | PASS | org_id=22 |
| governance_user_create_update_runtime | PASS | user_id=20 |
| governance_org_role_user_cleanup_runtime | PASS | user/org/role cleanup ok |
| governance_data_dict_crud_runtime | PASS | dict_id=2036062958697603074 |
| governance_approval_actor_rule_runtime | PASS | rule_id=10 |
| governance_approval_material_runtime | PASS | material_id=8 |
| governance_approval_flow_runtime | PASS | flow_id=2036062959125422081 |
| governance_approval_cleanup_runtime | PASS | approval cleanup ok |
| governance_sys_param_crud_runtime | PASS | sys_param_id=2036062959347720194 |
| governance_logs_export_runtime | PASS | login=5, operate=0, error=5 |
| governance_messages_runtime | PASS | messages=2 |
| governance_messages_cleanup_runtime | PASS | message cleanup ok |
| governance_pages_visible | PASS | 组织与人员管理 / 角色与权限管理 / 数据字典 / 审核审批配置 / 系统参数 / 系统日志 / 消息管理 |