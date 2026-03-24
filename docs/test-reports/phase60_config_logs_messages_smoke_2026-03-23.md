# Phase60 配置中心、日志、消息增强包烟测

- 日期：2026-03-23
- 覆盖需求：`Plan 4 = 配置中心 / 系统日志 / 消息中心增强`
- 结果：`28 / 28 PASS`

## 覆盖内容
- 系统日志：登录/操作/错误日志筛选与导出
- 消息中心：批量已读、导出、详情查看
- 配置中心：审批规则/材料/流程导出，字典/系统参数导出与筛选

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功并获取 token |
| health | PASS | {'status': 'UP', 'service': 'xngl-service'} |
| messages_list | PASS | records=10 |
| messages_summary | PASS | summary={'total': 13, 'unread': 0, 'read': 13} |
| messages_read_all | PASS | updated=0 |
| messages_export | PASS | bytes=2671 |
| login_logs_list | PASS | records=10 |
| login_logs_filtered | PASS | records=10 |
| login_logs_export | PASS | bytes=26704 |
| operation_logs_list | PASS | records=0 |
| operation_logs_export | PASS | bytes=111 |
| error_logs_list | PASS | records=10 |
| error_logs_export | PASS | bytes=10280 |
| data_dicts_list | PASS | records=0 |
| data_dicts_export | PASS | bytes=279 |
| sys_params_list | PASS | records=0 |
| sys_params_export | PASS | bytes=72 |
| approval_rules_list | PASS | records=1 |
| approval_rules_export | PASS | bytes=153 |
| approval_materials_list | PASS | records=3 |
| approval_materials_export | PASS | bytes=428 |
| approval_flows_list | PASS | records=3 |
| approval_flows_export | PASS | bytes=600 |
| ui_logs_page | PASS | title=系统日志, exportButtons=1 |
| ui_messages_page | PASS | title=消息管理, exportButtons=1 |
| ui_dictionary_page | PASS | title=数据字典, exportButtons=1 |
| ui_sys_params_page | PASS | title=系统参数, exportButtons=1 |
| ui_approval_page | PASS | title=审核审批配置, exportButtons=1 |

## 验证命令
```bash
cd xngl-service && mvn -q -DskipTests compile -pl xngl-service-web -am
cd xngl-web && npm run build
cd xngl-service && mvn spring-boot:run -pl xngl-service-starter
python3 scripts/phase60_config_logs_messages_smoke.py
```