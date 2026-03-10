# 用户体系 API/DTO 契约文档

## 1. 文档定位

本文件面向前后端联调评审，聚焦：

- 接口分组
- 请求 DTO 与响应 DTO
- 鉴权头与分页结构
- 错误码建议
- 页面到 API 映射

共享命名、枚举、权限码和路径规范统一引用 `00-shared-model.md`。

## 2. 基础约定

### 2.1 统一响应

所有接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 2.2 分页结构

```json
{
  "pageNo": 1,
  "pageSize": 20,
  "total": 128,
  "records": []
}
```

### 2.3 鉴权约定

- 请求头：`Authorization: Bearer {accessToken}`
- 刷新接口请求体字段：`refreshToken`
- 登录成功后，前端应继续拉取：
  - `GET /api/me`
  - `GET /api/me/permissions`
  - `GET /api/me/menus`

### 2.4 通用错误码建议

| 错误码 | 含义 |
| --- | --- |
| `400` | 参数错误 |
| `401` | 未登录或凭证无效 |
| `403` | 无权限访问 |
| `404` | 资源不存在 |
| `409` | 编码或唯一约束冲突 |
| `423` | 账号锁定 |
| `500` | 系统异常 |

## 3. 接口分组总览

| 分组 | 资源前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/auth` | 登录、刷新、登出 |
| 当前用户 | `/api/me` | 当前登录用户、权限、菜单 |
| 租户 | `/api/tenants` | 平台侧租户管理 |
| 组织 | `/api/orgs` | 组织树与组织详情 |
| 用户 | `/api/users` | 人员账号、组织关系、角色绑定 |
| 角色 | `/api/roles` | 角色主档、角色权限、角色数据范围 |
| 菜单 | `/api/menus` | 菜单树与路由配置 |
| 权限点 | `/api/permissions` | 按钮权限、接口权限、数据权限点 |
| 数据权限 | `/api/data-scopes` | 模板与预览 |
| 审批参与人 | `/api/approval-actors` | 审批规则与解析 |
| 审计 | `/api/login-logs`、`/api/operation-logs` | 日志查询 |

## 4. 认证与会话契约

### 4.1 `POST /api/auth/login`

用途：账号密码登录。

请求 DTO：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | `string` | 是 | 登录账号 |
| `password` | `string` | 是 | 密码 |
| `loginType` | `string` | 是 | 固定为 `ACCOUNT` |
| `captchaToken` | `string` | 否 | 预留验证码票据 |

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | `string` | 访问令牌 |
| `refreshToken` | `string` | 刷新令牌 |
| `tokenType` | `string` | 固定 `Bearer` |
| `expiresIn` | `number` | 秒数 |
| `permissionVersion` | `number` | 权限快照版本 |
| `user` | `LoginUserDto` | 用户信息 |

`LoginUserDto`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `string` | 用户 ID |
| `tenantId` | `string` | 租户 ID |
| `orgId` | `string` | 主组织 ID |
| `username` | `string` | 账号 |
| `name` | `string` | 姓名 |
| `userType` | `string` | 用户类型 |

### 4.2 `POST /api/auth/login-by-code`

用途：验证码登录，二期保留。

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `mobile` | `string` | 是 |
| `code` | `string` | 是 |
| `loginType` | `string` | 是，固定 `CODE` |

响应结构与 `login` 相同。

### 4.3 `POST /api/auth/refresh`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `refreshToken` | `string` | 是 |

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | `string` | 新访问令牌 |
| `refreshToken` | `string` | 新刷新令牌 |
| `expiresIn` | `number` | 秒数 |
| `permissionVersion` | `number` | 权限版本 |

### 4.4 `POST /api/auth/logout`

请求体：无。

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |

### 4.5 `GET /api/me`

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `string` | 用户 ID |
| `tenantId` | `string` | 租户 ID |
| `orgId` | `string` | 主组织 ID |
| `username` | `string` | 账号 |
| `name` | `string` | 姓名 |
| `userType` | `string` | 用户类型 |
| `roleCodes` | `string[]` | 角色编码列表 |
| `tenantType` | `string` | 租户类型 |

### 4.6 `GET /api/me/permissions`

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `buttonCodes` | `string[]` | 按钮权限码 |
| `apiCodes` | `string[]` | 接口权限码 |
| `dataScopes` | `DataScopeBriefDto[]` | 数据范围摘要 |
| `permissionVersion` | `number` | 权限版本 |

### 4.7 `GET /api/me/menus`

响应 DTO：`MenuTreeNodeDto[]`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `string` | 菜单 ID |
| `menuCode` | `string` | 菜单编码 |
| `menuName` | `string` | 菜单名称 |
| `routePath` | `string` | 路由路径 |
| `icon` | `string` | 图标 |
| `children` | `MenuTreeNodeDto[]` | 子节点 |

## 5. 租户契约

### 5.1 `GET /api/tenants`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantName` | `string` | 名称关键字 |
| `tenantType` | `string` | 租户类型 |
| `status` | `string` | 状态 |
| `pageNo` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |

响应 DTO：`PageResult<TenantListItemDto>`

`TenantListItemDto`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `string` | 租户 ID |
| `tenantCode` | `string` | 编码 |
| `tenantName` | `string` | 名称 |
| `tenantType` | `string` | 类型 |
| `status` | `string` | 状态 |
| `contactName` | `string` | 联系人 |
| `contactMobile` | `string` | 联系方式 |
| `expireTime` | `string` | 到期时间 |

### 5.2 `GET /api/tenants/{id}`

响应 DTO：`TenantDetailDto`

在列表字段基础上增加：

- `businessLicenseNo`
- `address`
- `remark`
- `createTime`
- `updateTime`

### 5.3 `POST /api/tenants`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantCode` | `string` | 是 |
| `tenantName` | `string` | 是 |
| `tenantType` | `string` | 是 |
| `contactName` | `string` | 否 |
| `contactMobile` | `string` | 否 |
| `expireTime` | `string` | 否 |
| `businessLicenseNo` | `string` | 否 |
| `address` | `string` | 否 |
| `remark` | `string` | 否 |

### 5.4 `PUT /api/tenants/{id}`

请求 DTO 与新增一致。

### 5.5 `PUT /api/tenants/{id}/status`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `status` | `string` | 是 |
| `reason` | `string` | 否 |

### 5.6 `GET /api/tenants/{id}/summary`

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantId` | `string` | 租户 ID |
| `orgCount` | `number` | 组织数 |
| `userCount` | `number` | 用户数 |
| `roleCount` | `number` | 角色数 |
| `status` | `string` | 状态 |

## 6. 组织人员契约

### 6.1 `GET /api/orgs/tree`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantId` | `string` | 租户 ID |
| `keyword` | `string` | 关键字 |
| `status` | `string` | 状态 |

响应 DTO：`OrgTreeNodeDto[]`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `string` | 组织 ID |
| `orgCode` | `string` | 组织编码 |
| `orgName` | `string` | 组织名称 |
| `parentId` | `string` | 父节点 |
| `leaderUserId` | `string` | 负责人 |
| `leaderName` | `string` | 负责人姓名 |
| `childrenCount` | `number` | 子节点数 |
| `children` | `OrgTreeNodeDto[]` | 子节点 |

### 6.2 `GET /api/orgs/{id}`

响应 DTO：`OrgDetailDto`

增加字段：

- `orgType`
- `orgPath`
- `sortOrder`
- `status`

### 6.3 `POST /api/orgs`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantId` | `string` | 是 |
| `orgCode` | `string` | 是 |
| `orgName` | `string` | 是 |
| `parentId` | `string` | 是 |
| `orgType` | `string` | 是 |
| `leaderUserId` | `string` | 否 |
| `sortOrder` | `number` | 否 |

### 6.4 `PUT /api/orgs/{id}`

请求 DTO 与新增一致。

### 6.5 `PUT /api/orgs/{id}/leader`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `leaderUserId` | `string` | 是 |

### 6.6 `PUT /api/orgs/{id}/status`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `status` | `string` | 是 |

### 6.7 `DELETE /api/orgs/{id}`

约束：

- 存在子组织时不可删除
- 存在有效用户挂接时不可删除

### 6.8 `GET /api/users`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | `string` | 姓名/账号/手机号 |
| `tenantId` | `string` | 租户 ID |
| `orgId` | `string` | 组织 ID |
| `status` | `string` | 状态 |
| `roleId` | `string` | 角色 ID |
| `pageNo` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |

响应 DTO：`PageResult<UserListItemDto>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `string` | 用户 ID |
| `username` | `string` | 账号 |
| `name` | `string` | 姓名 |
| `mobile` | `string` | 手机 |
| `mainOrgId` | `string` | 主组织 ID |
| `mainOrgName` | `string` | 主组织名称 |
| `roleNames` | `string[]` | 角色名称 |
| `status` | `string` | 状态 |
| `lastLoginTime` | `string` | 最后登录时间 |

### 6.9 `GET /api/users/{id}`

响应 DTO：`UserDetailDto`

在列表字段基础上增加：

- `tenantId`
- `email`
- `userType`
- `orgs`
- `roles`
- `needResetPassword`
- `lockStatus`

### 6.10 `POST /api/users`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantId` | `string` | 是 |
| `username` | `string` | 是 |
| `name` | `string` | 是 |
| `mobile` | `string` | 否 |
| `email` | `string` | 否 |
| `userType` | `string` | 是 |
| `mainOrgId` | `string` | 是 |
| `orgIds` | `string[]` | 否 |
| `roleIds` | `string[]` | 否 |
| `password` | `string` | 否，一期可后端生成初始密码 |

### 6.11 `PUT /api/users/{id}`

请求 DTO：除 `username` 外与新增基本一致。

### 6.12 `PUT /api/users/{id}/status`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `status` | `string` | 是 |
| `reason` | `string` | 否 |

### 6.13 `PUT /api/users/{id}/reset-password`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `newPassword` | `string` | 否 |
| `forceReset` | `boolean` | 是 |

### 6.14 `PUT /api/users/{id}/orgs`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `mainOrgId` | `string` | 是 |
| `orgIds` | `string[]` | 是 |

### 6.15 `PUT /api/users/{id}/roles`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `roleIds` | `string[]` | 是 |

### 6.16 `GET /api/users/{id}/roles`

响应 DTO：`RoleOptionDto[]`

### 6.17 `GET /api/users/{id}/orgs`

响应 DTO：`OrgOptionDto[]`

## 7. 角色权限契约

### 7.1 `GET /api/roles`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantId` | `string` | 租户 ID |
| `keyword` | `string` | 名称/编码 |
| `roleScope` | `string` | 范围 |
| `status` | `string` | 状态 |
| `pageNo` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |

响应 DTO：`PageResult<RoleListItemDto>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `string` | 角色 ID |
| `roleCode` | `string` | 编码 |
| `roleName` | `string` | 名称 |
| `roleScope` | `string` | 范围 |
| `roleCategory` | `string` | 分类 |
| `status` | `string` | 状态 |
| `builtinFlag` | `boolean` | 是否内置 |

### 7.2 `GET /api/roles/{id}`

响应 DTO：`RoleDetailDto`

### 7.3 `POST /api/roles`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantId` | `string` | 是 |
| `roleCode` | `string` | 是 |
| `roleName` | `string` | 是 |
| `roleScope` | `string` | 是 |
| `roleCategory` | `string` | 否 |
| `description` | `string` | 否 |
| `dataScopeTypeDefault` | `string` | 否 |

### 7.4 `PUT /api/roles/{id}`

请求 DTO 与新增一致。

### 7.5 `DELETE /api/roles/{id}`

约束：

- 已绑定用户的角色不可直接物理删除
- 建议走逻辑删除

### 7.6 `GET /api/roles/{id}/permissions`

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `menuIds` | `string[]` | 已选菜单 |
| `permissionIds` | `string[]` | 已选权限点 |
| `buttonCodes` | `string[]` | 按钮码预览 |
| `apiCodes` | `string[]` | 接口码预览 |

### 7.7 `PUT /api/roles/{id}/permissions`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `menuIds` | `string[]` | 否 |
| `permissionIds` | `string[]` | 否 |

### 7.8 `GET /api/roles/{id}/data-scopes`

响应 DTO：`RoleDataScopeDto[]`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bizModule` | `string` | 业务模块 |
| `scopeType` | `string` | 范围类型 |
| `orgIds` | `string[]` | 自定义组织集合 |
| `projectIds` | `string[]` | 自定义项目集合 |

### 7.9 `PUT /api/roles/{id}/data-scopes`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `rules` | `RoleDataScopeDto[]` | 是 |

## 8. 菜单与权限点契约

### 8.1 `GET /api/menus/tree`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantScope` | `string` | 范围 |
| `status` | `string` | 状态 |

响应 DTO：`MenuTreeNodeDto[]`

### 8.2 `GET /api/menus/{id}`

响应 DTO：`MenuDetailDto`

### 8.3 `POST /api/menus`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantScope` | `string` | 是 |
| `menuCode` | `string` | 是 |
| `menuName` | `string` | 是 |
| `parentId` | `string` | 是 |
| `menuType` | `string` | 是 |
| `routePath` | `string` | 否 |
| `componentPath` | `string` | 否 |
| `icon` | `string` | 否 |
| `permissionCode` | `string` | 否 |
| `sortOrder` | `number` | 否 |
| `visibleFlag` | `boolean` | 否 |

### 8.4 `PUT /api/menus/{id}`

请求 DTO 与新增一致。

### 8.5 `DELETE /api/menus/{id}`

约束：

- 有子节点时不可删除

### 8.6 `GET /api/roles/{id}/menu-tree`

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `checkedMenuIds` | `string[]` | 已选菜单节点 |
| `tree` | `MenuTreeNodeDto[]` | 菜单树 |

### 8.7 `GET /api/permissions`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `permissionType` | `string` | 权限类型 |
| `moduleCode` | `string` | 模块编码 |
| `keyword` | `string` | 名称/编码关键字 |
| `pageNo` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |

响应 DTO：`PageResult<PermissionItemDto>`

### 8.8 `GET /api/permissions/{id}`

响应 DTO：`PermissionDetailDto`

### 8.9 `POST /api/permissions`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantScope` | `string` | 是 |
| `permissionCode` | `string` | 是 |
| `permissionName` | `string` | 是 |
| `permissionType` | `string` | 是 |
| `moduleCode` | `string` | 否 |
| `resourceRef` | `string` | 否 |
| `httpMethod` | `string` | 否 |
| `apiPath` | `string` | 否 |

### 8.10 `PUT /api/permissions/{id}`

请求 DTO 与新增一致。

### 8.11 `DELETE /api/permissions/{id}`

约束：

- 已分配给角色时不可直接删除

## 9. 数据权限契约

### 9.1 `GET /api/data-scopes/templates`

响应 DTO：`DataScopeTemplateDto[]`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `scopeType` | `string` | 范围类型 |
| `scopeName` | `string` | 展示名称 |
| `supportsOrgSelection` | `boolean` | 是否支持组织选项 |
| `supportsProjectSelection` | `boolean` | 是否支持项目选项 |

### 9.2 `POST /api/data-scopes/preview`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantId` | `string` | 是 |
| `roleIds` | `string[]` | 否 |
| `scopeRules` | `RoleDataScopeDto[]` | 是 |

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `orgIds` | `string[]` | 可见组织 |
| `projectIds` | `string[]` | 可见项目 |
| `summary` | `string` | 文字摘要 |

## 10. 审批参与人契约

### 10.1 `GET /api/approval-actors/rules`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantId` | `string` | 租户 ID |
| `bizType` | `string` | 业务类型 |
| `nodeCode` | `string` | 节点编码 |

响应 DTO：`ApprovalActorRuleDto[]`

### 10.2 `POST /api/approval-actors/rules`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantId` | `string` | 是 |
| `bizType` | `string` | 是 |
| `nodeCode` | `string` | 是 |
| `actorType` | `string` | 是 |
| `actorRefId` | `string` | 否 |
| `matchMode` | `string` | 是 |
| `priority` | `number` | 否 |
| `actorSnapshotFlag` | `boolean` | 否 |

### 10.3 `PUT /api/approval-actors/rules/{id}`

请求 DTO 与新增一致。

### 10.4 `DELETE /api/approval-actors/rules/{id}`

请求体：无。

### 10.5 `POST /api/approval-actors/resolve`

请求 DTO：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `tenantId` | `string` | 是 |
| `bizType` | `string` | 是 |
| `nodeCode` | `string` | 是 |
| `initiatorId` | `string` | 是 |
| `orgId` | `string` | 否 |

响应 DTO：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `users` | `ResolvedUserDto[]` | 解析出的用户列表 |
| `resolvedBy` | `string[]` | 命中的规则来源 |

`ResolvedUserDto`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `string` | 用户 ID |
| `name` | `string` | 姓名 |
| `orgId` | `string` | 组织 ID |
| `roleCodes` | `string[]` | 角色编码 |

## 11. 日志查询契约

### 11.1 `GET /api/login-logs`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantId` | `string` | 租户 ID |
| `username` | `string` | 登录账号 |
| `ip` | `string` | IP |
| `successFlag` | `boolean` | 成功标记 |
| `startTime` | `string` | 开始时间 |
| `endTime` | `string` | 结束时间 |
| `pageNo` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |

响应 DTO：`PageResult<LoginLogItemDto>`

### 11.2 `GET /api/operation-logs`

查询参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tenantId` | `string` | 租户 ID |
| `operator` | `string` | 操作人 |
| `module` | `string` | 模块 |
| `action` | `string` | 动作 |
| `bizType` | `string` | 业务类型 |
| `startTime` | `string` | 开始时间 |
| `endTime` | `string` | 结束时间 |
| `pageNo` | `number` | 页码 |
| `pageSize` | `number` | 每页条数 |

响应 DTO：`PageResult<OperationLogItemDto>`

### 11.3 `GET /api/operation-logs/{id}`

响应 DTO：`OperationLogDetailDto`

## 12. 页面联调映射

| 前端页面 | 主要接口 |
| --- | --- |
| `Login.tsx` | `POST /api/auth/login`、`POST /api/auth/login-by-code`、`GET /api/me` |
| `Organization.tsx` | `GET /api/orgs/tree`、`GET /api/users`、`POST /api/users`、`PUT /api/users/{id}`、`PUT /api/users/{id}/status`、`PUT /api/users/{id}/roles` |
| `RolesManagement.tsx` | `GET /api/roles`、`GET /api/roles/{id}/permissions`、`PUT /api/roles/{id}/permissions`、`GET /api/roles/{id}/data-scopes`、`PUT /api/roles/{id}/data-scopes`、`GET /api/menus/tree` |
| `ApprovalConfig.tsx` | `GET /api/approval-actors/rules`、`POST /api/approval-actors/rules`、`POST /api/approval-actors/resolve` |
| `SystemLogs.tsx` | `GET /api/login-logs`、`GET /api/operation-logs` |

## 13. 与当前后端骨架的关系

当前后端已经存在的基础接口能力：

- `POST /api/auth/login`
- `GET /api/me`

其余接口目前仍属于契约冻结阶段，后续可按本文件逐组补齐 Controller、DTO、Manager 和 Mapper。
