# SQL 租户过滤审计报告

## 基本信息

- 审计日期：2026-04-08
- 审计范围：
  - `xngl-service-manager`
  - `xngl-service-web`
  - 所有 `@Select` 注解 SQL
  - `contractMapper/projectMapper/siteMapper/vehicleMapper` 的直接 `selectList/selectPage`
  - 报表聚合查询与报表控制器辅助查询
- 审计方法：
  - 使用 `rg` 静态扫描 `@Select(` 与指定 mapper 的直接查询调用
  - 对命中的实现逐一检查 `tenant_id` 条件、scope 收口链路与下游消费方式
  - 结合现有测试用例判断是否已有越权回归覆盖

## 结论摘要

- `@Select(` 扫描结果：未发现命中，当前审计范围内没有注解式手写 SQL。
- 合同、结算、导入、告警主体查询大多已显式附带 `tenant_id` 条件，或先通过 `MasterDataAccessScope` / `ContractAccessScope` 收敛可见 ID，再做聚合。
- 本次发现 1 处高风险、4 处中风险/低风险缺口：
  - 高风险集中在 `MiniProgramController` 的管理员项目查询分支，存在跨租户项目枚举面。
  - 中低风险主要是若干 resolver / report helper 仅依赖上游 ID 集合，没有同步补齐 `tenant_id` 作为防御性约束。

## 已确认安全路径

以下路径已确认具备显式 `tenant_id` 过滤，或在聚合前已通过 scope 进行可见范围收口：

- `xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractServiceImpl.java`
  - `pageContracts`、`pageContractsAdvanced`、`getContractStats`、`resolveAccessibleContractIds` 等查询均以 `tenantId` 为主条件，并叠加 `ContractAccessScope`。
- `xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractImportServiceImpl.java`
  - 导入预检、批次分页、错误列表、合同号去重均显式带 `tenantId`。
- `xngl-service-manager/src/main/java/com/xngl/manager/contract/SettlementServiceImpl.java`
  - 结算分页、统计、合同装载均以 `tenantId` 限定，`SettlementOrder` 还叠加 `ContractAccessScope`。
- `xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractReportServiceImpl.java`
  - `resolveAccessibleContractIds`、日报聚合与金额汇总查询均显式附带 `tenant_id` / `tenantId`，并结合 `ContractAccessScope`。
- `xngl-service-web/src/main/java/com/xngl/web/controller/OperationsReportController.java`
  - 项目日报 / 汇总先查询当前租户合同，再通过 `loadAccessibleProjectMap(...)` 依据 `MasterDataAccessScope` 过滤项目结果。
  - 对应已有 `OperationsReportControllerScopeTest` 覆盖“超出 project scope 的项目不出现在报表中”。
- `xngl-service-web/src/main/java/com/xngl/web/controller/DisposalsController.java`
  - 合同装载使用 `tenantId + projectId/siteId`。
- `xngl-service-web/src/main/java/com/xngl/web/controller/CheckinsController.java`
  - 合同装载使用 `tenantId + projectId/siteId`。
- `xngl-service-web/src/main/java/com/xngl/web/controller/AnalyticsController.java`
  - 合同、车辆列表均显式使用 `tenantId`。
- `xngl-service-web/src/main/java/com/xngl/web/controller/AlertsController.java`
  - 告警生成主链路对 `Contract`、`VehicleViolationRecord`、`AlertRule`、`AlertFence` 等主体查询均显式使用 `tenantId`。

## 发现明细

| 严重级别 | 位置 | 当前过滤方式 | 结论 | 建议 |
| --- | --- | --- | --- | --- |
| 高 | `xngl-service-web/src/main/java/com/xngl/web/controller/MiniProgramController.java:541-565,583-618` | 非管理员分支依赖 `accessibleProjectIds`；管理员分支直接 `projectMapper.selectList(new LambdaQueryWrapper<Project>())`，`loadAccessibleProjects(...)` 也未补 `tenantId` | `TENANT_ADMIN` / `ADMIN` / `SUPER_ADMIN` 可枚举全部租户项目 ID，并在项目列表查询中直接读取跨租户项目元数据，属于真实越权面 | 立即在管理员与普通用户两个分支统一补 `Project::getTenantId = user.getTenantId()`；补充小程序项目列表跨租户回归测试 |
| 中 | `xngl-service-web/src/main/java/com/xngl/web/support/ContractAccessScopeResolver.java:56-103` | 先通过 `Org::getTenantId` 收敛 `tenantOrgIds`，再按 `orgIds/projectIds` 扩展 `Project` / `Site`，但扩展查询本身未带 `tenantId` | 当前安全性依赖上游 scope 中的 `projectIds` 必须天然属于本租户；若权限配置或脏数据引入异租户 `projectId`，resolver 会把异租户 `Project/Site` 吸入合同访问域 | 在 `projectMapper.selectList(...)` 与 `siteMapper.selectList(...)` 上同时增加 `Project::getTenantId` / `Site::getTenantId` 条件 |
| 中 | `xngl-service-web/src/main/java/com/xngl/web/support/CollaborationAccessScopeResolver.java:102-160` | `expandVehicles/expandContracts/expandUsers` 已带 `tenantId`；`expandProjects/expandSites` 仅按 `orgIds/projectIds/siteIds` 扩展 | 协同访问域前半段缺少租户防御条件，存在与 `ContractAccessScopeResolver` 相同的信任链过长问题 | 对 `expandProjects(...)` 与 `expandSites(...)` 补齐 `tenantId` 条件，并增加跨租户污染测试 |
| 中 | `xngl-service-manager/src/main/java/com/xngl/manager/site/SiteServiceImpl.java:18-21`，`xngl-service-web/src/main/java/com/xngl/web/controller/SiteReportsController.java:181-214`，`xngl-service-web/src/main/java/com/xngl/web/controller/OperationsReportController.java:579-606` | 合同来源已按 `tenantId` 过滤，但 `siteService.list()` 返回全量 `Site`；下游按合同 `siteId` 映射站点信息 | 目前结果集主要受合同租户限制，但站点基础表未加租户边界，属于报表辅助查询的防御性缺口；一旦出现脏关联，可能泄露异租户站点名称/容量 | 为 `SiteService` 增加按租户查询方法，报表控制器改为显式按 `tenantId` 读取站点 |
| 低 | `xngl-service-web/src/main/java/com/xngl/web/controller/OperationsReportController.java:647-655`，`xngl-service-web/src/main/java/com/xngl/web/controller/SiteReportsController.java:286-294`，`xngl-service-web/src/main/java/com/xngl/web/controller/MiniProgramController.java:903-923` | 合同已先按租户收敛，但 `ContractTicket` / `Contract` 辅助查询仅按 `contractId` 或 `siteId` 集合过滤，没有再次附加 `tenantId` | 目前主要依赖前置合同/场地集合正确，不是首要漏洞，但欠缺防御性约束，增加脏数据情况下的串租户面 | 对辅助查询统一补 `tenantId` 条件，避免仅靠外键集合做边界控制 |
| 低 | `xngl-service-manager/src/main/java/com/xngl/manager/project/ProjectPaymentServiceImpl.java:176-190`，`xngl-service-web/src/main/java/com/xngl/web/controller/MiniReportsController.java:212-243`，`xngl-service-web/src/main/java/com/xngl/web/controller/UnitsController.java:486-487`，`xngl-service-web/src/main/java/com/xngl/web/controller/OrgsController.java:441-442` | 这些 `Project` 查询没有显式 `tenantId`，但输入 `orgIds` 或下游支付/合同记录本身来自当前租户 | 当前更像防御性缺口而非直接越权点；若上游 org/project 集合被污染，仍有被放大的可能 | 后续整改时一并为 `Project` 查询补 `tenantId` 条件，避免不同模块重复依赖“输入一定干净”的假设 |

## 现有测试证据

- `xngl-service-web/src/test/java/com/xngl/web/controller/OperationsReportControllerScopeTest.java`
  - 已覆盖项目日报、项目汇总、违章统计按 `MasterDataAccessScope` 排除不可见项目/组织。
- `xngl-service-web/src/test/java/com/xngl/web/controller/MasterDataPermissionControllerTest.java`
  - 已覆盖部分主数据详情越权/跨租户访问阻断。

以上测试说明“scope 过滤思路”已经进入回归体系，但尚未覆盖本报告列出的 `MiniProgramController` 管理员分支与 resolver/report helper 的显式 `tenant_id` 防御缺口。

## 整改优先级建议

1. 立即修复 `MiniProgramController` 项目查询链路的跨租户枚举问题，并补回归测试。
2. 第二批修复 `ContractAccessScopeResolver` / `CollaborationAccessScopeResolver` 的 `Project`、`Site` 扩展查询，缩短信任链。
3. 第三批统一清理报表 helper 与统计辅助查询中的“仅按 ID 集合过滤”模式，补齐 `tenant_id` 作为防御性约束。

## 备注

- 本报告基于静态代码审计与现有测试证据整理，未在本任务中新增业务代码修复。
- 若进入整改阶段，建议新增“跨租户脏数据/脏外键”回归测试，避免仅验证正常租户数据时出现漏网路径。
