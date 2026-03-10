# 用户体系共享模型规范

## 适用范围

本文件是 `xngl-service` 用户体系拆分产物的唯一共享模型源，服务于以下两份文档：

- `01-er-ddl-design.md`
- `02-api-dto-contract.md`

后续字段命名、权限码、接口路径、枚举值，如与本文件冲突，以本文件为准。

## 领域边界

用户体系一期范围固定为：

- 认证与会话
- 租户与组织
- 用户与角色
- 菜单与权限点
- 数据权限
- 审批参与人
- 登录日志与操作日志

核心实体如下：

- `sys_tenant`：租户边界
- `sys_org`：组织树
- `sys_user`：用户主档
- `sys_role`：角色主档
- `sys_menu`：菜单树
- `sys_permission`：权限点
- `sys_data_scope_rule`：角色级数据权限规则
- `sys_approval_actor_rule`：审批参与人规则
- `sys_login_log`：登录日志
- `sys_operation_log`：操作日志

## 统一命名规则

### 表命名

- 系统主数据与权限数据统一使用 `sys_` 前缀
- 关系表统一使用 `_rel` 后缀
- 日志表统一使用 `_log` 后缀
- 规则表统一使用 `_rule` 后缀

### 主键与审计字段

所有核心表统一使用以下基础字段：

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | `bigint unsigned` | 主键 |
| `create_time` | `datetime` | 创建时间 |
| `update_time` | `datetime` | 更新时间 |
| `deleted` | `tinyint(1)` | 逻辑删除标记 |

关系表如无更新语义，可只保留 `create_time`。

### 隔离字段

- 租户域表统一带 `tenant_id`
- 平台级共享资源使用 `tenant_scope` 表示适用范围，而不是伪造 `tenant_id=0`
- 受数据权限影响的业务表统一落 `tenant_id`

### 编码字段

| 语义 | 字段名 |
| --- | --- |
| 租户编码 | `tenant_code` |
| 组织编码 | `org_code` |
| 用户账号 | `username` |
| 角色编码 | `role_code` |
| 菜单编码 | `menu_code` |
| 权限编码 | `permission_code` |
| 审批业务编码 | `biz_type` |
| 审批节点编码 | `node_code` |

## 枚举冻结

### `tenant_type`

- `PLATFORM`
- `TRANSPORT_COMPANY`
- `CONSTRUCTION_COMPANY`
- `SITE_OPERATOR`

### `user_type`

- `PLATFORM`
- `TENANT_ADMIN`
- `EMPLOYEE`
- `DRIVER`

### `role_scope`

- `PLATFORM`
- `TENANT`
- `BUSINESS`

### `menu_type`

- `CATALOG`
- `MENU`
- `BUTTON`

### `permission_type`

- `MENU`
- `BUTTON`
- `API`
- `DATA_SCOPE`

### `scope_type`

- `ALL`
- `TENANT`
- `ORG_AND_CHILDREN`
- `SELF`
- `CUSTOM_ORG_SET`
- `CUSTOM_PROJECT_SET`

### `actor_type`

- `USER`
- `ROLE`
- `ORG_LEADER`
- `ORG_LEADER_LEVEL`
- `TENANT_ADMIN`
- `INITIATOR_SELF`
- `INITIATOR_MANAGER`

### `match_mode`

- `OR`
- `AND`

## 权限码规范

### 菜单码

菜单编码只描述树节点本身，字段落在 `sys_menu.menu_code`。

示例：

- `settings.organization`
- `settings.roles`
- `settings.approvals`
- `settings.logs`

### 权限码

统一落在 `sys_permission.permission_code`，不同类型遵循不同前缀。

| 类型 | 格式 | 示例 |
| --- | --- | --- |
| 菜单权限 | `menu:{route}` | `menu:settings:organization` |
| 按钮权限 | `btn:{module}:{action}` | `btn:user:create` |
| 接口权限 | `api:{module}:{action}` | `api:user:update` |
| 数据权限 | `data:{module}:{scope}` | `data:user:org_and_children` |

补充规则：

- 分隔符固定使用 `:`
- `module` 一律使用英文小写单词或短横线语义展开后的单词组
- `action` 固定为业务动作，不使用中文

## 接口路径规范

统一使用 `/api` 前缀，并按资源分组：

- `/api/auth/*`
- `/api/me/*`
- `/api/tenants/*`
- `/api/orgs/*`
- `/api/users/*`
- `/api/roles/*`
- `/api/menus/*`
- `/api/permissions/*`
- `/api/data-scopes/*`
- `/api/approval-actors/*`
- `/api/login-logs`
- `/api/operation-logs`

约束如下：

- 资源集合使用复数名词
- 明细使用 `/{id}`
- 状态切换使用 `/{id}/status`
- 关联关系更新使用 `/{id}/roles`、`/{id}/orgs`
- 预览/解析类能力使用动词资源尾缀，如 `/preview`、`/resolve`

## 通用返回结构

API 一律沿用当前项目已有的 `ApiResult<T>` 包装：

| 字段 | 说明 |
| --- | --- |
| `code` | 业务状态码，`0` 表示成功 |
| `message` | 结果说明 |
| `data` | 业务数据 |

分页结构统一命名：

| 字段 | 说明 |
| --- | --- |
| `pageNo` | 当前页 |
| `pageSize` | 每页条数 |
| `total` | 总数 |
| `records` | 当前页数据 |

## 鉴权头规范

- 请求头：`Authorization: Bearer {accessToken}`
- 刷新令牌字段：`refreshToken`
- 会话上下文至少包含：
  - `userId`
  - `tenantId`
  - `orgId`
  - `roleIds`
  - `dataScopeVersion`

## 页面与路由冻结

与前端 `xngl-web` 对齐的用户体系页面路由如下：

| 页面 | 路由 |
| --- | --- |
| 登录页 | `/login` |
| 组织人员 | `/settings/organization` |
| 角色权限 | `/settings/roles` |
| 审批配置 | `/settings/approvals` |
| 系统日志 | `/settings/logs` |

## 跨文档使用规则

- `01-er-ddl-design.md` 关注表、字段、索引、约束、迁移顺序
- `02-api-dto-contract.md` 关注接口、DTO、VO、错误码、联调映射
- 两份文档不得重新发明命名
- 如后续新增实体，必须先补本文件，再补两份分拆文档
