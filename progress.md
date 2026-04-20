# Progress Log

## Session: 2026-03-17

### Phase 1: 环境准备与项目启动
- **Status:** in_progress
- **Started:** 2026-03-17 22:30
- Actions taken:
  - 分析项目结构
  - 确认端口配置（后端8090，前端5173）
  - 创建测试计划文件
- Files created/modified:
  - task_plan.md (created)
  - findings.md (created)
  - progress.md (created)

## Test Results
| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
|      |       |          |        |        |

## Error Log
| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
|           |       | 1       |            |

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| Where am I? | Phase 1 - 环境准备 |
| Where am I going? | Phase 2-8 用户体系测试 |
| What's the goal? | 全面测试用户体系，发现并修复问题 |
| What have I learned? | 见 findings.md |
| What have I done? | 见上方 actions taken |

## Session: 2026-03-19

### Phase 2: 3.18 需求梳理与 P0 主链路闭环
- **Status:** in_progress
- **Started:** 2026-03-19
- Actions taken:
  - 基于最新 134 项需求重建状态表并落盘 `docs/tech-plans/requirements_status_2026-03-19.md`
  - 重写 `task_plan.md`，按 `P0/P1/P2/P3` 形成新版路线图
  - 打通项目列表/详情真实接口与前端展示
  - 打通合同列表/详情/入账管理真实接口前端联调
  - 打通场地列表/详情/基础信息/消纳清单的真实接口与空态展示
  - 将场地消纳清单后端改为基于 `biz_contract_ticket` 的真实分页查询
  - 将合同月报页面改为真实联调 `monthly/summary`、`monthly/trend`、`monthly/types`
  - 接入项目交款页面真实接口，打通项目交款列表筛选、汇总展示、交款登记、撤销流程
  - 接入项目/场地看板真实排行接口，修正报表 API 字段映射与页面空态
  - 解决后端报表控制器重复映射导致的启动失败，移除重复 `ProjectReportController`
  - 完成前后端启动与本地联调，生成测试报告 `docs/test-reports/functional_smoke_2026-03-19.md`
- Files created/modified:
  - `docs/tech-plans/requirements_status_2026-03-19.md`
  - `task_plan.md`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ProjectsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/project/ProjectListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/project/ProjectDetailDto.java`
  - `xngl-web/src/utils/projectApi.ts`
  - `xngl-web/src/utils/contractApi.ts`
  - `xngl-web/src/utils/contractReceipts.ts`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/pages/ProjectsManagement.tsx`
  - `xngl-web/src/pages/ProjectDetail.tsx`
  - `xngl-web/src/pages/ContractsManagement.tsx`
  - `xngl-web/src/pages/ContractDetail.tsx`
  - `xngl-web/src/pages/ContractsPayments.tsx`
  - `xngl-web/src/pages/SitesManagement.tsx`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `xngl-web/src/pages/SitesBasicInfo.tsx`
  - `xngl-web/src/pages/SitesDisposals.tsx`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SitesController.java`
  - `xngl-web/src/pages/MonthlyReport.tsx`
  - `xngl-web/src/pages/DashboardProjects.tsx`
  - `xngl-web/src/pages/DashboardSites.tsx`
  - `xngl-web/src/pages/ProjectsPayments.tsx`
  - `xngl-web/src/utils/reportApi.ts`
  - `xngl-web/src/utils/projectPaymentApi.ts`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ProjectReportController.java` (deleted)
  - `docs/test-reports/functional_smoke_2026-03-19.md`
  - `docs/test-reports/functional_smoke_2026-03-19.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 编译通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 安装通过 | PASS |
| `cd xngl-web && npm run build` | 构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 最新后端启动通过 | PASS |
| `cd xngl-web && npm run dev -- --host 127.0.0.1 --port 5173` | 前端开发服务启动通过 | PASS |
| `python3 -u <functional smoke>` | 26/26 通过，报告已落盘 | PASS |

## Session: 2026-03-20

### Phase 3: 主数据补齐、Schema Patch 修复、结算页联调
- **Status:** in_progress
- **Started:** 2026-03-20
- Actions taken:
  - 补齐 `projects/sites` 主数据种子 patch，落盘 `014_project_site_seed_from_contracts.sql`
  - 修正 schema-sync 对 MySQL 不兼容的 `ADD COLUMN IF NOT EXISTS` 与索引 patch 兼容逻辑
  - 重启后端并观察启动日志，`schema-sync baseline executed` 后未再出现此前 SQL 语法告警
  - 验证 `/projects`、`/sites`、`/reports/projects/*`、`/reports/sites/*` 已返回真实数据，不再为空态
  - 为场地主数据补充 `site_type/capacity/settlement_mode` 等字段，并新增分类结算规则 patch
  - 为报表与结算补充 3/4/6 月合同领票演示数据，打通非零累计量与结算明细生成
  - 新增 `xngl-web/src/utils/settlementApi.ts`，统一封装结算列表、详情、统计、生成、提交、审批、驳回接口
  - 将 `xngl-web/src/pages/Settlements.tsx` 从静态 mock 改为真实结算页，打通筛选、详情 Drawer、项目/场地结算生成、提交审批、审批通过、驳回
  - 落地场地分类结算：国有场地按月申请、集体场地按比例+服务费、工程场地按单价、短驳场地按服务费
  - 新增处置证表与种子数据，接通 `ProjectsPermits` 页面真实列表/详情
  - 重新执行结算主链路 API 烟测，并输出新报告 `docs/test-reports/functional_smoke_2026-03-20.md`
  - 追加 P1/P2 增量联调报告 `docs/test-reports/functional_smoke_2026-03-20_p1p2.md`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/schema/SchemaSyncRunner.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/012_contract_settlement_tables.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/013_contract_enhanced_tables.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/014_project_site_seed_from_contracts.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/015_site_master_and_settlement_rules.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/016_contract_ticket_seed_for_reports.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/017_contract_ticket_seed_extra_periods.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/018_disposal_permit_table_and_seed.sql`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/disposal/entity/DisposalPermit.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/SettlementServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SettlementController.java`
  - `xngl-web/src/utils/settlementApi.ts`
  - `xngl-web/src/utils/permitApi.ts`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/pages/Settlements.tsx`
  - `xngl-web/src/pages/ProjectsPermits.tsx`
  - `xngl-web/src/pages/SitesManagement.tsx`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `xngl-web/src/pages/SitesBasicInfo.tsx`
  - `docs/test-reports/functional_smoke_2026-03-20.md`
  - `docs/test-reports/functional_smoke_2026-03-20.json`
  - `docs/test-reports/functional_smoke_2026-03-20_p1p2.md`
  - `docs/test-reports/functional_smoke_2026-03-20_p1p2.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-web && npm run build` | 结算页接入后前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 重启后 `schema-sync baseline executed` 后未见 SQL 告警 | PASS |
| `python3 -u <settlement smoke>` | 项目结算生成/提交/审批通过链路通过 | PASS |
| `python3 -u <settlement smoke>` | 场地结算生成/提交/驳回链路通过 | PASS |
| `python3 -u docs/test-reports/generate functional smoke` | 16/16 通过，报告已落盘 `docs/test-reports/functional_smoke_2026-03-20.md` | PASS |
| `python3 -u <p1/p2 smoke>` | 11/11 通过，报告已落盘 `docs/test-reports/functional_smoke_2026-03-20_p1p2.md` | PASS |

### Phase 4: 车辆主数据与车队聚合联调
- **Status:** in_progress
- **Started:** 2026-03-20
- Actions taken:
  - 新增 `019_vehicle_master_seed.sql`，补齐 `biz_vehicle` 主数据字段、运输单位种子与车辆演示数据
  - 手动执行车辆 patch 尾段，完成索引创建、运输单位回填与 7 条主数据种子插入
  - 将 `Vehicle` 实体扩展到车辆类型、品牌型号、载重、司机、车队、保养/保险/GPS 等字段
  - 重写 `VehiclesController`，打通 `/api/vehicles` 列表/详情/新增/更新、`/stats`、`/company-capacity`、`/fleets`
  - 新增车辆 DTO 与前端 `vehicleApi.ts`，统一车辆主档、运力汇总、车队聚合接口封装
  - 将 `VehiclesManagement` 从静态 mock 改为真实联调，接入统计卡片、分页列表、详情 Drawer、运输单位运力汇总与新增车辆弹窗
  - 将 `FleetManagement` 从静态卡片改为真实车队聚合展示，按车队显示车辆数、出车状态、司机数、预警数与核载能力
  - 通过 API 自动化创建测试车辆 `浙A97610`，验证车辆新增链路
  - 生成车辆后端烟测报告 `docs/test-reports/vehicle_backend_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/Vehicle.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleStatsDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleCompanyCapacityDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleFleetSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleUpsertDto.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/baseline.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/019_vehicle_master_seed.sql`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/pages/VehiclesManagement.tsx`
  - `xngl-web/src/pages/FleetManagement.tsx`
  - `docs/test-reports/vehicle_backend_smoke_2026-03-20.md`
  - `docs/test-reports/vehicle_backend_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 车辆主数据后端编译/安装通过 | PASS |
| `cd xngl-web && npm run build` | 车辆/车队前端构建通过 | PASS |
| `python3 -u <vehicle api smoke>` | 登录、车辆统计、列表、运力聚合、车队聚合、创建接口 11/11 通过 | PASS |

## Session: 2026-03-23

### Phase 54: 场地资料与场地报表补强
- **Status:** completed
- **Started:** 2026-03-23
- Actions taken:
  - 新增 `050_site_documents_lifecycle_seed.sql`，补齐建设阶段和移交阶段演示资料
  - 场地资料汇总接口新增 `approvalType` 过滤，关键字匹配补充阶段编码
  - 场地资料配置接口增强生命周期阶段校验、资料类型归档校验、默认审批类型推导与建设期资料格式校验
  - `SitesDocuments` 页面增加建设阶段页签、审批类型过滤、场地树检索与归档统计卡片
  - `SiteDetail` 文档区扩展到审批/建设/运营/移交四阶段，并补建设期资料类型选项
  - `SiteReportsController` 新增 `CUSTOM` 自定义时间区间、短区间按日趋势和本地 CSV 导出任务生成
  - `SitesReports` 页面新增自定义时间范围查询与导出轮询下载闭环
  - 完成专项烟测并落盘 `docs/test-reports/phase54_site_documents_and_reports_smoke_2026-03-23.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteDocumentsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteConfigsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteReportsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniReportsController.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/050_site_documents_lifecycle_seed.sql`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/utils/reportApi.ts`
  - `xngl-web/src/pages/SitesDocuments.tsx`
  - `xngl-web/src/pages/SitesReports.tsx`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `docs/test-reports/phase54_site_documents_and_reports_smoke_2026-03-23.md`
  - `docs/test-reports/phase54_site_documents_and_reports_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../050_site_documents_lifecycle_seed.sql` | 生命周期资料种子 patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests compile` | 后端编译通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 前端构建通过 | PASS |
| `python3 -u <phase54 smoke>` | 场地资料/场地报表 10/10 通过 | PASS |

### Phase 55: 违法清单补强
- **Status:** completed
- **Started:** 2026-03-23
- Actions taken:
  - 新增违法清单汇总接口 `/api/vehicles/violations/summary`
  - 新增违法详情接口 `/api/vehicles/violations/{id}`，补充车辆、司机、车队、速度、里程等回查字段
  - `ViolationsList` 页面改为真实汇总统计口径，新增所属单位列与详情抽屉
  - 保持禁用 / 提前解禁闭环不变，并纳入专项烟测
  - 完成专项烟测并落盘 `docs/test-reports/phase55_violation_list_smoke_2026-03-23.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleViolationsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleViolationSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleViolationDetailDto.java`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/pages/ViolationsList.tsx`
  - `docs/test-reports/phase55_violation_list_smoke_2026-03-23.md`
  - `docs/test-reports/phase55_violation_list_smoke_2026-03-23.json`

### Phase 56: 预警中心规则增强
- **Status:** completed
- **Started:** 2026-03-23
- Actions taken:
  - 预警列表、汇总、分析、Top Risk 增加筛选条件联动能力
  - 自动预警生成扩展到 `SITE_CAPACITY_WARN` 和 `VEHICLE_ROUTE_DEVIATION`
  - `AlertsMonitor` 页面改为使用筛选态的汇总、分析和 Top Risk 数据
  - 专项烟测中通过临时下调场地容量阈值验证自动预警触发，并在结束后恢复原值
  - 完成专项烟测并落盘 `docs/test-reports/phase56_alerts_rules_and_generation_smoke_2026-03-23.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AlertsController.java`
  - `xngl-web/src/utils/alertApi.ts`
  - `xngl-web/src/pages/AlertsMonitor.tsx`
  - `docs/test-reports/phase56_alerts_rules_and_generation_smoke_2026-03-23.md`
  - `docs/test-reports/phase56_alerts_rules_and_generation_smoke_2026-03-23.json`

### Phase 5: 单位统一模型与单位管理页
- **Status:** in_progress
- **Started:** 2026-03-20
- Actions taken:
  - 新增 `020_unit_master_seed.sql`，为 `sys_org` 补齐联系人、联系电话、地址、统一社会信用代码、备注字段
  - 补充建设单位、施工单位演示数据，并将项目/合同回挂到真实单位主数据
  - 新增 `/api/units`、`/api/units/summary`、详情、新增、更新接口，统一建设/施工/运输单位查询与维护
  - 补充单位 DTO：列表、详情、汇总、保存 DTO
  - 新增前端 `unitApi.ts` 与 `UnitsManagement` 页面，接入真实列表、筛选、详情 Drawer、单位新增/编辑
  - 将“单位管理”加入左侧菜单与系统设置路由
  - 执行单位管理 API 烟测并落盘 `docs/test-reports/unit_management_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/organization/Org.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/UnitsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitUpsertDto.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/baseline.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/020_unit_master_seed.sql`
  - `xngl-web/src/utils/unitApi.ts`
  - `xngl-web/src/pages/UnitsManagement.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/unit_management_smoke_2026-03-20.md`
  - `docs/test-reports/unit_management_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../020_unit_master_seed.sql` | 单位主数据 patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 单位管理后端编译/安装通过 | PASS |
| `cd xngl-web && npm run build` | 单位管理前端构建通过 | PASS |
| `python3 -u <unit management smoke>` | 单位汇总、列表、详情、创建接口 9/9 通过 | PASS |


### Phase 6: 场地报表、总体分析、运力分析、油电卡与保险联调
- **Status:** in_progress
 
### Phase 12: 车型管理真实联调
- **Status:** completed
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `biz_vehicle_model` 车型字典表实体、Mapper、控制器与 `032_vehicle_model_dictionary.sql`
  - 落地 `/api/vehicle-models` 列表、详情、新增、编辑、状态切换、删除接口
  - 新增前端 `vehicleModelApi.ts` 与 `VehicleModelsManagement` 页面，完成车型管理 CRUD 台账
  - 将“车型管理”接入 `App` 路由和左侧“车辆与运力”菜单
  - 在 `VehiclesManagement` 中接入启用车型字典，打通车辆档案与车型库的数据衔接
  - 执行 MySQL patch、重启后端、完成 API + UI 烟测并落盘 `docs/test-reports/phase12_vehicle_models_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleModel.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleModelMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleModelsController.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/032_vehicle_model_dictionary.sql`
  - `xngl-web/src/utils/vehicleModelApi.ts`
  - `xngl-web/src/pages/VehicleModelsManagement.tsx`
  - `xngl-web/src/pages/VehiclesManagement.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase12_vehicle_models_smoke_2026-03-21.md`
  - `docs/test-reports/phase12_vehicle_models_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 车型管理后端编译/安装通过 | PASS |
| `cd xngl-web && npm run build` | 车型管理前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../032_vehicle_model_dictionary.sql` | 车型字典 patch 执行通过 | PASS |
| `python3 -u <phase12 vehicle models smoke>` | API + UI 10/10 通过，报告已落盘 | PASS |

### Phase 13: 车辆信息维护真实闭环
- **Status:** completed
- **Started:** 2026-03-21
- Actions taken:
  - 为 `/api/vehicles` 增加 `vehicleType` 筛选与删除接口，补齐车辆档案维护所需后端能力
  - 扩展 `vehicleApi.ts`，新增车辆删除能力并同步前端查询参数
  - 将 `VehiclesManagement` 升级为完整维护台：支持车型模板带参、新增、编辑、删除、VIN/年检/自重/负责人/里程等字段维护
  - 打通高级筛选中的车型条件，与车型库数据联动形成真实维护闭环
  - 重启后端并输出 `docs/test-reports/phase13_vehicle_maintenance_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/pages/VehiclesManagement.tsx`
  - `docs/test-reports/phase13_vehicle_maintenance_smoke_2026-03-21.md`
  - `docs/test-reports/phase13_vehicle_maintenance_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 车辆维护后端编译/安装通过 | PASS |
| `cd xngl-web && npm run build` | 车辆维护前端构建通过 | PASS |
| `python3 -u <phase13 vehicle maintenance smoke>` | API + UI 12/12 通过，报告已落盘 | PASS |

### Phase 14: 组织类型独立管理
- **Status:** completed
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `033_org_type_dictionary_seed.sql`，补齐“执法组织 / 公司单位组织”默认类型字典
  - 重构 `Organization` 页面为“组织人员 / 组织类型”双页签，新增组织类型统计、搜索、创建、编辑、启停、删除能力
  - 复用 `/api/data-dicts` 实现组织类型独立管理，形成最小后端改动的真实闭环
  - 完成 API + UI 烟测并落盘 `docs/test-reports/phase14_org_types_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/033_org_type_dictionary_seed.sql`
  - `xngl-web/src/pages/Organization.tsx`
  - `docs/test-reports/phase14_org_types_smoke_2026-03-21.md`
  - `docs/test-reports/phase14_org_types_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../033_org_type_dictionary_seed.sql` | 组织类型字典种子执行通过 | PASS |
| `cd xngl-web && npm run build` | 组织类型页面前端构建通过 | PASS |
| `python3 -u <phase14 org types smoke>` | API + UI 10/10 通过，报告已落盘 | PASS |
- **Started:** 2026-03-20
- Actions taken:
  - 新增 `/api/reports/sites/*` 场地报表接口，打通场地日/月/年汇总、列表、趋势与导出任务创建
  - 新增 `/api/reports/dashboard/*`、`/api/reports/vehicles/capacity-analysis`，打通总体分析与运力分析真实聚合
  - 将 `Dashboard`、`VehicleCapacityAnalysis`、`SitesReports`、`VehicleTracking` 接入真实数据源，完成天地图底图下的车辆追踪联调
  - 新增 `021_vehicle_cards_and_insurances.sql`，补齐油电卡与保险主数据表及演示数据
  - 新增 `/api/vehicle-cards`、`/api/vehicle-insurances` 真实接口，支持汇总、列表、绑定/解绑、充值、编辑、保险维护
  - 将 `VehiclesCards` 从静态 mock 改为真实联调页面，并新增 `VehicleInsurances` 页面与菜单路由
  - 修复油电卡解绑时 `vehicle_id/org_id` 不能置空的问题，为 `VehicleCard` 增加 `FieldStrategy.ALWAYS`
  - 清理 8090/8091 旧 Java 进程，重新以最新代码启动后端 8090 与前端 5173 开发服务
  - 生成联调报告 `docs/test-reports/vehicle_cards_insurance_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteReportsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AnalyticsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleCardsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleInsurancesController.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/vehicle/controller/VehicleInsuranceController.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleCard.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleInsuranceRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleCardMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleInsuranceRecordMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/*`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleCard*.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleInsurance*.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/021_vehicle_cards_and_insurances.sql`
  - `xngl-web/src/utils/reportApi.ts`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/utils/vehicleCardApi.ts`
  - `xngl-web/src/utils/vehicleInsuranceApi.ts`
  - `xngl-web/src/pages/Dashboard.tsx`
  - `xngl-web/src/pages/SitesReports.tsx`
  - `xngl-web/src/pages/VehicleCapacityAnalysis.tsx`
  - `xngl-web/src/pages/VehicleTracking.tsx`
  - `xngl-web/src/pages/VehiclesCards.tsx`
  - `xngl-web/src/pages/VehicleInsurances.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/site_dashboard_capacity_smoke_2026-03-20.md`
  - `docs/test-reports/site_dashboard_capacity_smoke_2026-03-20.json`
  - `docs/test-reports/vehicle_cards_insurance_smoke_2026-03-20.md`
  - `docs/test-reports/vehicle_cards_insurance_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 场地报表、分析、油电卡、保险后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 看板/报表/油电卡/保险前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 最新后端已在 `8090` 启动 | PASS |
| `cd xngl-web && npm run dev -- --host 127.0.0.1` | 最新前端已在 `5173` 启动 | PASS |
| `python3 /tmp/vehicle_cards_insurance_smoke.py` | 16/16 通过，报告已落盘 `docs/test-reports/vehicle_cards_insurance_smoke_2026-03-20.md` | PASS |

### Phase 7: 车辆维保计划与维修管理联调
- **Status:** completed
- **Completed:** 2026-03-20
- Actions taken:
  - 新增 `024_vehicle_maintenance_and_repairs.sql`，落库车辆维保计划、维保记录、维修申请单三张业务表及演示数据
  - 新增后端 `/api/vehicle-maintenance-plans*` 与 `/api/vehicle-repairs*` 真实接口，支持汇总、分页、创建、更新、维保执行、维修审批/驳回/完工
  - 为 `VehicleMaintenancePlan`、`VehicleMaintenanceRecord`、`VehicleRepairOrder` 增加 `@TableName` 映射，修复维保控制器泛型分页签名冲突
  - 在父 `pom.xml` 补齐 Lombok annotation processor 配置，恢复 `xngl-service-web` / `xngl-service-manager` 编译链
  - 新增前端 `VehicleMaintenancePlans`、`VehicleRepairs` 页面与 API util，并接入车辆与运力菜单/路由
  - 重新执行 `024` patch，启动后端 `8090` 与前端 `5173`，输出烟测报告 `docs/test-reports/vehicle_maintenance_repairs_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/pom.xml`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleMaintenancePlan.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleMaintenanceRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleRepairOrder.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleMaintenancePlanMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleMaintenanceRecordMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleRepairOrderMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleMaintenancePlansController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleRepairsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleMaintenance*.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleRepair*.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/024_vehicle_maintenance_and_repairs.sql`
  - `xngl-web/src/utils/vehicleMaintenanceApi.ts`
  - `xngl-web/src/utils/vehicleRepairApi.ts`
  - `xngl-web/src/pages/VehicleMaintenancePlans.tsx`
  - `xngl-web/src/pages/VehicleRepairs.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/vehicle_maintenance_repairs_smoke_2026-03-20.md`
  - `docs/test-reports/vehicle_maintenance_repairs_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../024_vehicle_maintenance_and_repairs.sql` | 024 维保/维修 patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 后端全量编译安装通过 | PASS |
| `cd xngl-web && npm run build` | 前端维保/维修页面构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 最新后端已在 `8090` 启动 | PASS |
| `cd xngl-web && npm run dev -- --host 127.0.0.1` | 最新前端已在 `5173` 启动 | PASS |
| `python3 <vehicle_maintenance_repairs_smoke>` | 17/17 通过，报告已落盘 `docs/test-reports/vehicle_maintenance_repairs_smoke_2026-03-20.md` | PASS |

### Phase 8: 车队人证管理联调
- **Status:** completed
- **Completed:** 2026-03-20
- Actions taken:
  - 新增 `025_vehicle_personnel_certificates.sql`，落库车队人证与费用台账表，并补充两条演示数据
  - 新增后端 `/api/vehicle-personnel-certificates`、`/api/vehicle-personnel-certificates/summary`，支持汇总、分页、创建、更新、证件到期状态计算与费用未缴统计
  - 新增前端 `VehiclePersonnelCertificates` 页面与 `vehiclePersonnelApi.ts`，接入真实筛选、统计卡片、新增/编辑弹窗
  - 将“人证管理”接入车辆与运力菜单与路由，完成本地联调
  - 输出烟测报告 `docs/test-reports/vehicle_personnel_certificates_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehiclePersonnelCertificate.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehiclePersonnelCertificateMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclePersonnelCertificatesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehiclePersonnelCertificateListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehiclePersonnelCertificateSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehiclePersonnelCertificateUpsertDto.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/025_vehicle_personnel_certificates.sql`
  - `xngl-web/src/utils/vehiclePersonnelApi.ts`
  - `xngl-web/src/pages/VehiclePersonnelCertificates.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/vehicle_personnel_certificates_smoke_2026-03-20.md`
  - `docs/test-reports/vehicle_personnel_certificates_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../025_vehicle_personnel_certificates.sql` | 025 人证管理 patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 后端人证管理编译安装通过 | PASS |
| `cd xngl-web && npm run build` | 前端人证管理页面构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 最新后端已在 `8090` 启动 | PASS |
| `cd xngl-web && npm run dev -- --host 127.0.0.1` | 最新前端已在 `5173` 启动 | PASS |
| `python3 <vehicle_personnel_certificates_smoke>` | 9/9 通过，报告已落盘 `docs/test-reports/vehicle_personnel_certificates_smoke_2026-03-20.md` | PASS |

### Phase 9: 车队维护/运输计划/调度/财务/报表联调
- **Status:** completed
- **Completed:** 2026-03-20
- Actions taken:
  - 新增 `026_fleet_management_foundation.sql`，落库车队档案、运输计划、调度申请/审批、财务记录四张业务表，并补充初始化演示数据
  - 新增后端 `/api/fleet-management/*` 真实接口，支持概览汇总、车队维护分页、新增/编辑、运输计划新增/编辑、调度申请/审批/驳回、财务记录新增/编辑、报表统计
  - 修复 `026` patch 与老库 `biz_vehicle.fleet_name` 的排序规则冲突，确保本地 MySQL 可直接执行
  - 新增前端 `fleetApi.ts`，将 `FleetManagement` 升级为 6 个标签页：概览、车队维护、运输计划、调度审批、财务管理、报表管理
  - 完成前后端重启联调，并输出烟测报告 `docs/test-reports/fleet_management_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/fleet/FleetProfile.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/fleet/FleetTransportPlan.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/fleet/FleetDispatchOrder.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/fleet/FleetFinanceRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/FleetProfileMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/FleetTransportPlanMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/FleetDispatchOrderMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/FleetFinanceRecordMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/FleetManagementController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/fleet/*.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/026_fleet_management_foundation.sql`
  - `xngl-web/src/utils/fleetApi.ts`
  - `xngl-web/src/pages/FleetManagement.tsx`
  - `docs/test-reports/fleet_management_smoke_2026-03-20.md`
  - `docs/test-reports/fleet_management_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../026_fleet_management_foundation.sql` | 026 车队管理 patch 执行通过，已修复排序规则冲突 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 后端车队管理编译安装通过 | PASS |
| `cd xngl-web && npm run build` | 前端车队管理页面构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 最新后端已在 `8090` 启动 | PASS |
| `cd xngl-web && npm run dev` | 最新前端已在 `5173` 启动 | PASS |
| `python3 <fleet_management_smoke>` | 15/15 通过，报告已落盘 `docs/test-reports/fleet_management_smoke_2026-03-20.md` | PASS |

### Phase 7: 处置证关联 + 审批材料/字典/系统参数收口
- **Status:** in_progress
- **Started:** 2026-03-20
- Actions taken:
  - 扩展处置证模型与接口，补齐 `tenantId`、`contractId` 关联能力，`ProjectsPermits` 支持项目/场地/合同联动展示与编辑
  - 新增 `027_disposal_permit_contract_and_approval_materials.sql`，补齐处置证合同关联字段与办事材料配置表
  - 新增 `ApprovalMaterialConfigsController` 与 `ApprovalMaterialConfig`/Mapper，支持办事材料新增、编辑、启停
  - 将 `ApprovalConfig` 升级为“审批人规则 + 办事材料配置”双标签配置台
  - 打通数据字典、系统参数真实页面与接口联调，完成字典/参数新增、编辑、状态切换链路验证
  - 输出联调报告 `docs/test-reports/permit_approval_dict_sysparam_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/disposal/entity/DisposalPermit.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/DisposalPermitsController.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/system/ApprovalMaterialConfig.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/ApprovalMaterialConfigMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ApprovalMaterialConfigsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/user/ApprovalMaterialConfigCreateUpdateDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/user/ApprovalMaterialConfigDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/user/ApprovalMaterialConfigListItemDto.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/027_disposal_permit_contract_and_approval_materials.sql`
  - `xngl-web/src/pages/ProjectsPermits.tsx`
  - `xngl-web/src/pages/ApprovalConfig.tsx`
  - `xngl-web/src/utils/permitApi.ts`
  - `xngl-web/src/utils/approvalApi.ts`
  - `docs/test-reports/permit_approval_dict_sysparam_smoke_2026-03-20.md`
  - `docs/test-reports/permit_approval_dict_sysparam_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../027_disposal_permit_contract_and_approval_materials.sql` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 处置证/审批材料/字典/参数后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 审批配置/处置证前端构建通过 | PASS |
| `python3 -u <permit approval dict sysparam smoke>` | 25/25 通过，报告已落盘 `docs/test-reports/permit_approval_dict_sysparam_smoke_2026-03-20.md` | PASS |

### Phase 8: 事件管理与安全台账扩字段增强
- **Status:** in_progress
- **Started:** 2026-03-20
- Actions taken:
  - 新增 `028_alert_event_security_detail_extensions.sql`，补齐人工事件地址/联系人/时限/责任人/附件字段，以及安全检查隐患类别/等级/整改责任人/费用/附件字段
  - 为人工事件新增 `/api/events/summary` 汇总接口，补齐类型/来源桶统计与超期统计
  - 扩展 `ManualEventsController`、`SecurityInspectionsController` 与对应实体映射，打通新增字段的创建、详情、流转、整改闭环
  - 将 `EventsManagement` 增强为“筛选 + 汇总卡片 + 类型/来源分布 + 扩展字段详情/表单”版本
  - 将 `SecurityLedger` 增强为“对象分布 + 隐患等级/类别分布 + 扩展字段台账明细”版本
  - 执行 028 patch、重启后端、完成扩展字段冒烟测试并落盘 `docs/test-reports/alerts_events_security_detail_extensions_smoke_2026-03-20.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/028_alert_event_security_detail_extensions.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/event/ManualEvent.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/security/SecurityInspection.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ManualEventsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SecurityInspectionsController.java`
  - `xngl-web/src/utils/manualEventApi.ts`
  - `xngl-web/src/utils/securityApi.ts`
  - `xngl-web/src/pages/EventsManagement.tsx`
  - `xngl-web/src/pages/SecurityLedger.tsx`
  - `docs/test-reports/alerts_events_security_detail_extensions_smoke_2026-03-20.md`
  - `docs/test-reports/alerts_events_security_detail_extensions_smoke_2026-03-20.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../028_alert_event_security_detail_extensions.sql"` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 事件/安全台账增强后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 事件/安全台账增强前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听 | PASS |
| `python3 -u <alerts events security detail extensions smoke>` | 9/9 通过，报告已落盘 `docs/test-reports/alerts_events_security_detail_extensions_smoke_2026-03-20.md` | PASS |

### Phase 9: 预警模型 + 合同/人员预警增强
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `029_alert_contract_personnel_seed.sql`，补齐合同临期/应付款超期、人员证照/违章风险规则、推送配置与示例预警事件
  - 扩展 `AlertsController`，新增合同/人员预警计数、目标名称补齐、`/top-risk-targets` 排行接口与更完整的详情字段
  - 将 `AlertsMonitor` 升级为支持合同/人员风险排行、确认/关闭原因填写的处置弹窗版本
  - 将 `AlertConfig` 增加“研判模型”标签页，按场景展示规则数、挂接围栏/推送数，并支持快捷新增场景规则
  - 执行 029 patch、重启后端，并完成合同/人员预警模型冒烟测试 `docs/test-reports/alerts_models_contract_personnel_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/029_alert_contract_personnel_seed.sql`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AlertsController.java`
  - `xngl-web/src/utils/alertApi.ts`
  - `xngl-web/src/pages/AlertsMonitor.tsx`
  - `xngl-web/src/pages/AlertConfig.tsx`
  - `docs/test-reports/alerts_models_contract_personnel_smoke_2026-03-21.md`
  - `docs/test-reports/alerts_models_contract_personnel_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../029_alert_contract_personnel_seed.sql"` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 预警增强后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 预警增强前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听 | PASS |
| `python3 -u <alerts models contract personnel smoke>` | 13/13 通过，报告已落盘 `docs/test-reports/alerts_models_contract_personnel_smoke_2026-03-21.md` | PASS |

### Phase 10: 预警自动生成 + 审批流程配置
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `030_approval_flow_configs_and_alert_generation.sql`，补齐 `sys_approval_config` 表及默认流程节点种子
  - 扩展 `AlertsController`，新增 `/api/alerts/generate`，支持项目进度、合同临期/应收超期、人员证照临期/安全检查风险自动生成与自动关闭
  - 新增 `ApprovalConfigsController`，打通审批流程配置列表、详情、新增、编辑、启停、删除真实接口
  - 将 `AlertsMonitor` 增加“刷新自动预警”入口，支持前端直接触发模型刷新并查看结果
  - 将 `ApprovalConfig` 增加“流程配置”标签页，支持审批节点编排、审批方式、条件表达式、审批人表达式配置
  - 执行 030 patch、重启后端、完成 API + UI 烟测并落盘 `docs/test-reports/phase10_alerts_approval_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/030_approval_flow_configs_and_alert_generation.sql`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/approval/entity/ApprovalConfig.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/approval/controller/ApprovalConfigController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AlertsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ApprovalConfigsController.java`
  - `xngl-web/src/utils/alertApi.ts`
  - `xngl-web/src/utils/approvalApi.ts`
  - `xngl-web/src/pages/AlertsMonitor.tsx`
  - `xngl-web/src/pages/ApprovalConfig.tsx`
  - `docs/test-reports/phase10_alerts_approval_smoke_2026-03-21.md`
  - `docs/test-reports/phase10_alerts_approval_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../030_approval_flow_configs_and_alert_generation.sql` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 预警自动生成/审批流程配置后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 审批流程配置/预警页面前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听（PID 26468） | PASS |
| `python3 -u <phase10 alerts approval smoke>` | 14/14 通过，报告已落盘 `docs/test-reports/phase10_alerts_approval_smoke_2026-03-21.md` | PASS |

### Phase 11: 处置证有效期预警 + 定时任务化
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `031_project_permit_alert_seed.sql`，补齐 `PROJECT_PERMIT_EXPIRING` 规则、推送配置与临期处置证演示数据
  - 扩展 `AlertsController`，将项目预警进一步补齐到处置证有效期场景，并支持 `relatedType=DISPOSAL_PERMIT` 的实例快照
  - 新增 `AlertAutoGenerateScheduler`，通过 `@Scheduled` 定时刷新项目/合同/人员自动预警
  - 为启动类启用调度能力，并在 `application.yml` 中新增预警自动生成开关与 cron 配置
  - 重启后端并完成处置证有效期预警 + 定时任务配置烟测，报告落盘 `docs/test-reports/phase11_permit_alert_scheduler_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/031_project_permit_alert_seed.sql`
  - `xngl-service/xngl-service-starter/src/main/java/com/xngl/XnglServiceApplication.java`
  - `xngl-service/xngl-service-starter/src/main/resources/application.yml`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AlertsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/task/AlertAutoGenerateScheduler.java`
  - `docs/test-reports/phase11_permit_alert_scheduler_smoke_2026-03-21.md`
  - `docs/test-reports/phase11_permit_alert_scheduler_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../031_project_permit_alert_seed.sql` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 处置证预警/定时任务化后端安装通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听（PID 37686） | PASS |
| `python3 -u <phase11 permit alert scheduler smoke>` | 9/9 通过，报告已落盘 `docs/test-reports/phase11_permit_alert_scheduler_smoke_2026-03-21.md` | PASS |

### Phase 12: 打卡数据查询与异常作废
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `CheckinsController` 与 `CheckinListItemDto`，补齐 `/api/checkins` 列表查询和 `/api/checkins/{id}/void` 作废接口
  - 以 `biz_contract_ticket` 为数据源打通项目、场地、合同、运输单位、车辆/司机信息映射，并支持关键字、项目、场地、状态、日期范围和分页筛选
  - 新增 `checkinApi.ts` 与 `CheckinRecords` 页面，支持统计卡片、组合筛选、异常作废弹窗和作废原因展示
  - 将“打卡数据”接入 `App.tsx` 路由与 `MainLayout.tsx` 左侧菜单 `信息查询`
  - 完成后端安装、前端构建、API 烟测与前端接线校验，报告落盘 `docs/test-reports/phase15_checkin_records_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/CheckinsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/query/CheckinListItemDto.java`
  - `xngl-web/src/utils/checkinApi.ts`
  - `xngl-web/src/pages/CheckinRecords.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase15_checkin_records_smoke_2026-03-21.md`
  - `docs/test-reports/phase15_checkin_records_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 打卡数据后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 打卡数据前端构建通过 | PASS |
| `python3 -u <phase15 checkin api smoke>` | 4/4 通过，临时票据创建/作废/校验/清理闭环完成 | PASS |
| `grep -n "queries/checkins" xngl-web/src/App.tsx` | 路由注册校验通过 | PASS |
| `grep -n "信息查询\\|打卡数据" xngl-web/src/layouts/MainLayout.tsx` | 菜单接入校验通过 | PASS |
| `grep -n "/checkins" xngl-web/src/utils/checkinApi.ts` | 前端 API 绑定校验通过 | PASS |

### Phase 13: 消纳信息全平台查询
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `DisposalsController` 与 `DisposalRecordDto`，补齐 `/api/disposals` 全平台消纳信息查询接口
  - 基于 `biz_contract_ticket` + 合同/项目/场地/运输单位/车辆映射形成统一消纳记录视图，支持关键字、项目、场地、状态、日期范围和分页查询
  - 新增 `disposalApi.ts` 与 `DisposalRecords` 页面，支持统计卡片、组合筛选和全平台消纳记录列表展示
  - 将“消纳信息”接入 `App.tsx` 路由与 `MainLayout.tsx` 左侧菜单 `信息查询`
  - 完成后端安装、前端构建、API 烟测与前端接线校验，报告落盘 `docs/test-reports/phase16_disposal_records_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/DisposalsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/query/DisposalRecordDto.java`
  - `xngl-web/src/utils/disposalApi.ts`
  - `xngl-web/src/pages/DisposalRecords.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase16_disposal_records_smoke_2026-03-21.md`
  - `docs/test-reports/phase16_disposal_records_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 消纳信息后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 消纳信息前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听（PID 64878） | PASS |
| `python3 -u <phase16 disposal api smoke>` | 4/4 通过，关键字/场地/日期筛选闭环完成 | PASS |
| `grep -n "queries/disposals" xngl-web/src/App.tsx` | 路由注册校验通过 | PASS |
| `grep -n "消纳信息\\|queries/disposals" xngl-web/src/layouts/MainLayout.tsx` | 菜单接入校验通过 | PASS |
| `grep -n "'/disposals'" xngl-web/src/utils/disposalApi.ts` | 前端 API 绑定校验通过 | PASS |

### Phase 14: 错误日志采集与查询
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `034_error_log_foundation.sql`，创建 `sys_error_log` 表并补充初始化联调样例数据
  - 新增 `ErrorLog` 实体、`ErrorLogMapper`、`ErrorLogService`、`ErrorLogsController`，补齐 `/api/error-logs` 分页查询接口
  - 扩展 `GlobalExceptionHandler`，对未捕获异常增加错误日志落库逻辑，自动回填当前用户的 `tenantId/username`
  - 将 `SystemLogs` 的“错误日志”页签接入真实数据，支持关键字检索、级别筛选和分页展示
  - 完成 patch 落库、后端重启、API 烟测、临时测试数据清理与报告落盘 `docs/test-reports/phase17_error_logs_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/034_error_log_foundation.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/system/ErrorLog.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/ErrorLogMapper.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/log/ErrorLogService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/log/ErrorLogServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ErrorLogsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/user/ErrorLogListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/exception/GlobalExceptionHandler.java`
  - `xngl-web/src/pages/SystemLogs.tsx`
  - `docs/test-reports/phase17_error_logs_smoke_2026-03-21.md`
  - `docs/test-reports/phase17_error_logs_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../034_error_log_foundation.sql"` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 错误日志后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 错误日志前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听（PID 68457） | PASS |
| `python3 -u <phase17 error logs api smoke>` | 4/4 通过，列表/级别/关键字筛选闭环完成 | PASS |
| `grep -n "error-logs" xngl-web/src/pages/SystemLogs.tsx` | 前端错误日志数据绑定校验通过 | PASS |
| `grep -n "@RequestMapping(\"/api/error-logs\")" .../ErrorLogsController.java` | 后端接口注册校验通过 | PASS |

### Phase 15: 消息管理中心
- **Status:** in_progress
- **Started:** 2026-03-21
- Actions taken:
  - 新增 `035_message_center_foundation.sql`，创建 `biz_message_record` 表并补充管理员可见消息种子
  - 将旧的消息骨架改造成真实消息中心：补齐 `MessageRecordService`、`MessagesController`、汇总接口和“标记已读”动作
  - 将旧 `MessageRecordController` 调整到 `_legacy` 路径，避免和新 `/api/messages` 真实接口冲突
  - 新增 `MessageCenter` 页面与 `messageApi.ts`，支持消息汇总、关键字/状态/时间筛选、分页和标记已读
  - 将“消息中心 / 消息管理”接入前端菜单和路由，完成 patch 落库、后端重启、API 烟测与临时数据清理，报告落盘 `docs/test-reports/phase18_message_center_smoke_2026-03-21.md`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/035_message_center_foundation.sql`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/entity/MessageRecord.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/MessageRecordService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/MessageRecordServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/controller/MessageRecordController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MessagesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/message/MessageListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/message/MessageSummaryDto.java`
  - `xngl-web/src/pages/MessageCenter.tsx`
  - `xngl-web/src/utils/messageApi.ts`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase18_message_center_smoke_2026-03-21.md`
  - `docs/test-reports/phase18_message_center_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../035_message_center_foundation.sql"` | patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 消息管理后端安装通过 | PASS |
| `cd xngl-web && npm run build` | 消息管理前端构建通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端重启通过，端口 8090 正常监听（PID 73957） | PASS |
| `python3 -u <phase18 message center smoke>` | 5/5 通过，汇总/列表/已读动作闭环完成 | PASS |
| `grep -n "path=\"messages\"" xngl-web/src/App.tsx` | 路由注册校验通过 | PASS |
| `grep -n "消息中心\\|/messages" xngl-web/src/layouts/MainLayout.tsx` | 菜单接入校验通过 | PASS |

### Phase 19: 角色新增闭环
- **Status:** completed
- **Started:** 2026-03-21 21:49
- **Finished:** 2026-03-21 22:02
- Actions taken:
  - 为 `RoleServiceImpl#create/update` 增加默认状态与默认数据范围规则兜底
  - 修正 `RolesController` 的数据范围 DTO 映射，统一对接真实列 `bizModule/scopeType/scopeValue`
  - 在 `RolesManagement` 增加“新增角色”弹窗，补齐角色编码、范围、默认数据权限和描述录入
  - 冒烟时定位 `DataScopeRule` 实体误映射兼容字段导致的 SQL 500，并修复为非持久化字段
  - 重启后端、回归 API、校验库表落数并清理测试角色
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/role/RoleServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/RolesController.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/system/DataScopeRule.java`
  - `xngl-web/src/pages/RolesManagement.tsx`
  - `docs/test-reports/phase19_role_create_smoke_2026-03-21.md`
  - `docs/test-reports/phase19_role_create_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 角色新增后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 角色新增前端构建通过 | PASS |
| `python3 -u <phase19 role create smoke>` | 登录、创建、详情、默认权限/数据范围、列表查询 6/6 通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "select ... from sys_data_scope_rule ..."` | 默认数据范围规则已真实落库 | PASS |

### Phase 20: 人员新增闭环
- **Status:** completed
- **Started:** 2026-03-21 22:03
- **Finished:** 2026-03-21 22:10
- Actions taken:
  - 为 `UsersController` 补充用户创建必填校验、默认初始密码 `123456`、`needResetPassword` 与本地账号默认属性
  - 新增 `/api/users/{id}/roles`、`/api/users/{id}/orgs`，并补齐用户列表/详情的组织名称、角色名称回填
  - 在 `Organization` 页面增加“新增人员”弹窗，接入主组织、所属组织、角色多选和默认密码提示
  - 通过 API 验证新用户创建、组织/角色绑定、默认密码登录，并清理冒烟账号
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/UsersController.java`
  - `xngl-web/src/pages/Organization.tsx`
  - `docs/test-reports/phase20_user_create_smoke_2026-03-21.md`
  - `docs/test-reports/phase20_user_create_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 人员新增后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 人员新增前端构建通过 | PASS |
| `python3 -u <phase20 user create smoke>` | 创建、详情、列表、关系接口、默认密码登录 7/7 通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "delete from sys_user where id = 15"` | 冒烟账号物理清理完成 | PASS |

### Phase 21: 人员详情、编辑、删除闭环
- **Status:** completed
- **Started:** 2026-03-21 22:11
- **Finished:** 2026-03-21 22:14
- Actions taken:
  - 在 `Organization` 页面增加人员详情抽屉，展示主组织、所属组织、角色、联系方式、首次改密和最近登录信息
  - 复用人员弹窗实现编辑，打通基础信息、组织绑定、角色绑定的更新回显
  - 将人员列表操作改为“查看 / 编辑 / 删除”，接通真实删除接口
  - 通过 API 回归详情查询、编辑更新、删除后不可见，并物理清理逻辑删除的测试账号
- Files created/modified:
  - `xngl-web/src/pages/Organization.tsx`
  - `docs/test-reports/phase21_user_detail_smoke_2026-03-21.md`
  - `docs/test-reports/phase21_user_detail_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 人员详情后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 人员详情前端构建通过 | PASS |
| `python3 -u <phase21 user detail smoke>` | 详情、编辑、删除、更新后列表回显 8/8 通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "delete from sys_user where id = 16"` | 详情冒烟账号物理清理完成 | PASS |

### Phase 22: 角色详情、编辑、删除闭环
- **Status:** completed
- **Started:** 2026-03-21 22:15
- **Finished:** 2026-03-21 22:21
- Actions taken:
  - 在 `RolesManagement` 页面增加角色详情描述区，展示角色编码、范围、分类、状态、描述与默认数据范围
  - 复用角色弹窗实现编辑，并补齐删除角色动作
  - 修复 `RoleServiceImpl#updateDataScopeRules` 逻辑删除残留导致的唯一索引冲突，改为按角色物理删除数据范围规则后重建
  - 完成角色详情、编辑、权限树保存、数据范围保存、删除和历史测试数据清理
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/DataScopeRuleMapper.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/role/RoleServiceImpl.java`
  - `xngl-web/src/pages/RolesManagement.tsx`
  - `docs/test-reports/phase22_role_detail_smoke_2026-03-21.md`
  - `docs/test-reports/phase22_role_detail_smoke_2026-03-21.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 角色详情后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 角色详情前端构建通过 | PASS |
| `python3 -u <phase22 role detail smoke>` | 详情、编辑、权限保存、删除 10/10 通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "delete from sys_role where id in (9,10,11)"` | 角色冒烟数据物理清理完成 | PASS |

### Phase 23: 合同导出闭环
- **Status:** completed
- **Started:** 2026-03-22 10:55
- **Finished:** 2026-03-22 11:18
- Actions taken:
  - 新增 `ContractExportFileService`，补齐导出任务 `PENDING -> PROCESSING -> COMPLETED/FAILED` 状态流转和 CSV 文件生成
  - 打通 `/api/contracts/export` 与 `/api/export-tasks/{id}/download`，支持按筛选条件导出合同清单
  - `ContractsManagement` 接入真实导出按钮、任务轮询和下载行为，替换原占位“导出月度报表”
  - 完成接口级导出冒烟、前后端构建校验与前端 dev 路由可达校验
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractExportFileService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ExportTaskService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ExportTaskServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ContractExportController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ExportTaskController.java`
  - `xngl-web/src/pages/ContractsManagement.tsx`
  - `xngl-web/src/utils/contractApi.ts`
  - `docs/test-reports/phase23_contract_export_smoke_2026-03-22.md`
  - `docs/test-reports/phase23_contract_export_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 合同导出后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 合同导出前端构建通过 | PASS |
| `python3 -u <phase23 contract export api smoke>` | 登录、筛选导出、任务完成、CSV 下载与内容校验 6/6 通过 | PASS |
| `curl -sS -I http://127.0.0.1:5173/contracts` | 前端导出入口页面可访问，返回 `200 OK` | PASS |

### Phase 23: 合同导出真实文件闭环
- **Status:** completed
- **Started:** 2026-03-22 10:55
- **Finished:** 2026-03-22 11:15
- Actions taken:
  - 为导出任务补齐 `PROCESSING / COMPLETED / FAILED` 状态流转，支持文件名、文件路径和失败原因回写
  - 新增 `ContractExportFileService`，复用合同高级筛选分页查询，真实生成 UTF-8 CSV 文件并落盘到本地导出目录
  - 将 `/api/export-tasks/{id}/download` 改为真实文件流下载接口，返回 `attachment` 响应
  - 在 `ContractsManagement` 页面接通“导出筛选结果”按钮，透传当前筛选条件、轮询任务状态并触发浏览器下载
  - 完成 API 与前端无头联调，验证筛选合同 `HT-005` 的导出内容正确
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ExportTaskService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ExportTaskServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractExportFileService.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ContractExportController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ExportTaskController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/ContractExportRequestDto.java`
  - `xngl-web/src/utils/contractApi.ts`
  - `xngl-web/src/pages/ContractsManagement.tsx`
  - `docs/test-reports/phase23_contract_export_smoke_2026-03-22.md`
  - `docs/test-reports/phase23_contract_export_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 合同导出后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 合同导出前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 登录、列表查询、导出任务创建、CSV 下载校验通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端合同清单点击“导出筛选结果”下载成功，文件包含 `HT-005` | PASS |

### Phase 24: 合同导入真实入库闭环
- **Status:** completed
- **Started:** 2026-03-22 11:16
- **Finished:** 2026-03-22 11:25
- Actions taken:
  - 将合同导入预览请求升级为 `fileName + rows` 结构，并把原始导入行落盘到本地 JSON 文件，供提交阶段复用
  - 扩展导入校验逻辑，支持必填项、ID、日期、金额、布尔值和重复合同号校验，并保留逐行错误明细
  - 打通 `import-commit` 真实入库，按有效行写入 `biz_contract`，默认落 `sourceType=IMPORT`、`approvalStatus=APPROVED`、`contractStatus=EFFECTIVE`
  - 在 `ContractsManagement` 页面新增“批量导入合同”弹窗，支持 CSV 模板下载、文件解析、预览校验、错误表格展示和提交导入
  - 完成 API 与前端无头联调，验证导入后列表可按新增合同编号检索
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractImportService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractImportServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractImportCommitResult.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ContractImportController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/ContractImportPreviewRequestDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/ImportCommitResultDto.java`
  - `xngl-web/src/utils/contractApi.ts`
  - `xngl-web/src/pages/ContractsManagement.tsx`
  - `docs/test-reports/phase24_contract_import_smoke_2026-03-22.md`
  - `docs/test-reports/phase24_contract_import_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 合同导入后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 合同导入前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 预览、提交、导入后列表回查通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端合同导入弹窗选择 CSV、预览校验、提交导入通过 | PASS |

### Phase 25: 线下合同录入闭环
- **Status:** completed
- **Started:** 2026-03-22 11:26
- **Finished:** 2026-03-22 11:35
- Actions taken:
  - 为合同创建 DTO 补齐 `partyId`，打通三方单位关联字段从前端到实体落库
  - 调整 `ContractServiceImpl#createContract`，`sourceType=OFFLINE` 时直接落 `approvalStatus=APPROVED`、`contractStatus=EFFECTIVE`
  - 在 `ContractsManagement` 页面新增“线下合同录入”弹窗，支持普通/三方合同、项目/场地/单位选择、价格区域切换、区内/区外单价和自动金额计算
  - 完成 API 线下三方补录链路验证，并确认 `OFFLINE` 来源、三方单位和区内/区外单价字段可回查
  - 完成前端页面入口验证，确认补录弹窗与关键字段已挂载
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/ContractCreateDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ContractsController.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractServiceImpl.java`
  - `xngl-web/src/utils/contractApi.ts`
  - `xngl-web/src/pages/ContractsManagement.tsx`
  - `docs/test-reports/phase25_offline_contract_entry_smoke_2026-03-22.md`
  - `docs/test-reports/phase25_offline_contract_entry_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 线下合同录入后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 线下合同录入前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 创建线下三方补录合同并回查详情通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端合同清单页线下合同录入弹窗可达，关键字段可见 | PASS |

### Phase 26: 单位统计项目/场地汇总闭环
- **Status:** completed
- **Started:** 2026-03-22 11:36
- **Finished:** 2026-03-22 11:45
- Actions taken:
  - 为单位统计新增 `/api/units/{id}/projects` 和 `/api/units/{id}/contract-groups`，支持项目维度统计和按消纳场地分组的合同明细查询
  - 新增 `UnitProjectStatDto`、`UnitSiteContractGroupDto`、`UnitContractItemDto`，统一封装单位详情钻取视图所需数据结构
  - 在 `UnitsManagement` 详情抽屉中补充项目列表表格、项目点击切换，以及“合同明细（按消纳场地汇总）”卡片视图
  - 完成 API 与页面联调，验证单位维度项目/合同/场地钻取链路可用
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/UnitsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitProjectStatDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitSiteContractGroupDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/unit/UnitContractItemDto.java`
  - `xngl-web/src/utils/unitApi.ts`
  - `xngl-web/src/pages/UnitsManagement.tsx`
  - `docs/test-reports/phase26_unit_statistics_smoke_2026-03-22.md`
  - `docs/test-reports/phase26_unit_statistics_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 单位统计后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 单位统计前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 校验单位项目统计与按场地汇总合同明细通过 | PASS |
| `python3 -u - <<'PY' ...` | 页面单位详情抽屉可见项目列表与按场地汇总区块 | PASS |

### Phase 27: 合同审批闭环
- **Status:** completed
- **Started:** 2026-03-22 11:46
- **Finished:** 2026-03-22 12:08
- Actions taken:
  - 为合同审批补齐真实状态流转，`submit/approve/reject` 统一写入 `biz_contract_approval_record`
  - 在服务层新增审批文书自动生成功能，按动作写入 `/tmp/xngl-contract-docs` 并登记到 `biz_contract_material`
  - 在 `ContractsController` 增加合同材料下载接口，支持详情页直接预览/下载审批文书
  - 在 `ContractDetail` 页面补齐提交审批、审批通过、驳回动作和材料下载入口，提交后自动刷新审批流程与材料列表
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ContractsController.java`
  - `xngl-web/src/utils/contractApi.ts`
  - `xngl-web/src/pages/ContractDetail.tsx`
  - `docs/test-reports/phase27_contract_approval_smoke_2026-03-22.md`
  - `docs/test-reports/phase27_contract_approval_smoke_2026-03-22.json`
  - `docs/test-reports/phase27_contract_approval_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 合同审批后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 合同审批前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 校验提交/通过/驳回、审批记录、审批文书下载通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端合同详情页提交审批后时间线和材料列表联调通过 | PASS |

### Phase 28: 内拨申请闭环
- **Status:** completed
- **Started:** 2026-03-22 12:09
- **Finished:** 2026-03-22 12:13
- Actions taken:
  - 复用现有 `/api/contracts/transfers` 后端能力，新增前端独立“内拨申请”页面，接入列表、状态筛选、详情抽屉与创建弹窗
  - 为 `contractApi` 补齐内拨申请类型定义以及创建、详情、提交、通过、驳回动作 API
  - 在合同与结算菜单中挂载 `/contracts/transfers` 路由，形成可访问入口
  - 完成 API 与页面烟测，验证内拨通过后源/目标合同金额和方量联动调整
- Files created/modified:
  - `xngl-web/src/pages/ContractTransfers.tsx`
  - `xngl-web/src/utils/contractApi.ts`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase28_contract_transfer_smoke_2026-03-22.md`
  - `docs/test-reports/phase28_contract_transfer_smoke_2026-03-22.json`
  - `docs/test-reports/phase28_contract_transfer_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-web && npm run build` | 内拨申请前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 校验内拨创建/详情/提交/通过/驳回与合同联动通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端内拨申请页面、创建弹窗与详情抽屉联调通过 | PASS |

### Phase 29: 项目结算合同汇总补全
- **Status:** completed
- **Started:** 2026-03-22 12:14
- **Finished:** 2026-03-22 12:18
- Actions taken:
  - 为结算详情补齐关联合同汇总模型，新增 `SettlementContractSummaryDto`
  - 在 `SettlementController` 中根据结算明细来源票据反查合同，补充 `contractSummaries` 和明细行 `contractNo`
  - 在 `Settlements` 详情抽屉中增加关联合同汇总表，并在结算明细中展示合同编号
  - 通过本地测试票据生成新的项目结算单，验证列表、详情、审批流转和关联合同展示
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SettlementController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/SettlementContractSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/SettlementDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/contract/SettlementLineDto.java`
  - `xngl-web/src/utils/settlementApi.ts`
  - `xngl-web/src/pages/Settlements.tsx`
  - `docs/test-reports/phase29_project_settlement_smoke_2026-03-22.md`
  - `docs/test-reports/phase29_project_settlement_smoke_2026-03-22.json`
  - `docs/test-reports/phase29_project_settlement_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 项目结算后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 项目结算前端构建通过 | PASS |
| `python3 - <<'PY' ...` | API 校验项目结算生成、详情合同汇总、列表与审批通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端结算详情抽屉可见关联合同编号 | PASS |

### Phase 30: 消息推送审批通知闭环
- **Status:** completed
- **Started:** 2026-03-22 12:25
- **Finished:** 2026-03-22 12:34
- Actions taken:
  - 修复 `MiniProgramUserController` 包路径与扫描配置不一致导致的后端启动冲突
  - 打通合同审批、内拨申请、项目结算 3 类审批结果到 `biz_message_record` 的 `SYSTEM + SMS` 推送落库
  - 验证消息中心 `/api/messages`、`/api/messages/summary`、已读处理与前端消息页通知回查
- Files created/modified:
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/miniprogram/controller/MiniProgramUserController.java`
  - `xngl-service/xngl-service-starter/src/main/java/com/xngl/XnglServiceApplication.java`
  - `docs/test-reports/phase30_message_push_smoke_2026-03-22.md`
  - `docs/test-reports/phase30_message_push_smoke_2026-03-22.json`
  - `docs/test-reports/phase30_message_push_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 消息推送后端编译通过 | PASS |
| `python3 - <<'PY' ...` | 真实审批动作触发消息推送、DB 落库、消息汇总与已读校验通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端消息管理页可见合同/内拨/结算三类审批通知 | PASS |

### Phase 31: 车辆地图真实轨迹
- **Status:** completed
- **Started:** 2026-03-22 12:35
- **Finished:** 2026-03-22 12:45
- Actions taken:
  - 新增 `biz_vehicle_track_point` 轨迹表与轨迹种子数据，补充车辆历史轨迹数据源
  - 新增 `/api/vehicles/{id}/track-history` 历史轨迹接口，并在无轨迹时回退为空轨迹/实时定位
  - `VehicleTracking` 页面改为真实轨迹查询与回放，支持时间范围查询，不再使用伪造 `buildPath`
- Files created/modified:
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleTrackPoint.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleTrackPointMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleTrackHistoryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleTrackPointDto.java`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/036_vehicle_track_history_foundation.sql`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/pages/VehicleTracking.tsx`
  - `docs/test-reports/phase31_vehicle_tracking_smoke_2026-03-22.md`
  - `docs/test-reports/phase31_vehicle_tracking_smoke_2026-03-22.json`
  - `docs/test-reports/phase31_vehicle_tracking_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 车辆轨迹后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 车辆追踪前端构建通过 | PASS |
| `mysql ... -e "source .../036_vehicle_track_history_foundation.sql"` | 车辆轨迹表和种子数据落库成功 | PASS |
| `python3 - <<'PY' ...` | API 校验真实历史轨迹与空轨迹退化通过 | PASS |
| `python3 -u - <<'PY' ...` | 前端车辆追踪页、轨迹查询控件和车辆列表联调通过 | PASS |

### Phase 32: 项目报表独立页
- **Status:** completed
- **Started:** 2026-03-22 14:11
- **Finished:** 2026-03-22 14:26
- Actions taken:
  - 后端新增项目报表汇总、列表、趋势、导出四个接口，支持按日/月/年统计与关键字筛选
  - 新增 `ProjectReportSummaryDto`、`ProjectReportItemDto`，按项目聚合合同票据、工程总量、累计消纳量和剩余工程量
  - 前端新增 `ProjectsReports` 页面，补齐项目筛选、关键字检索、趋势图、统计卡片和导出入口
  - 项目管理菜单新增“项目报表”路由，完成 API 与页面联调
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/OperationsReportController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/ProjectReportSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/ProjectReportItemDto.java`
  - `xngl-web/src/utils/reportApi.ts`
  - `xngl-web/src/pages/ProjectsReports.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase32_project_report_smoke_2026-03-22.md`
  - `docs/test-reports/phase32_project_report_smoke_2026-03-22.json`
  - `docs/test-reports/phase32_project_report_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 项目报表后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 项目报表前端构建通过 | PASS |
| `python3 - <<'PY' ...` | `/api/reports/projects/summary/list/trend/export` 真实接口校验通过 | PASS |
| `python3 -u - <<'PY' ...` | `/projects/reports` 页面联调与真实数据渲染通过 | PASS |

### Phase 33: 运力分析真实轨迹口径
- **Status:** completed
- **Started:** 2026-03-22 14:27
- **Finished:** 2026-03-22 14:34
- Actions taken:
  - `AnalyticsController` 接入 `VehicleTrackPointMapper`，优先按真实轨迹点序列计算车辆总里程
  - 运力分析对有轨迹车辆生成真实里程驱动的荷载/空载里程与能耗口径，无轨迹数据保留原有回退算法
  - 修复按关键字筛选时趋势图仍统计全量车辆的问题，保证列表、汇总、趋势口径一致
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AnalyticsController.java`
  - `docs/test-reports/phase33_vehicle_capacity_track_based_2026-03-22.md`
  - `docs/test-reports/phase33_vehicle_capacity_track_based_2026-03-22.json`
  - `docs/test-reports/phase33_vehicle_capacity_track_based_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 运力分析后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 运力分析前端构建通过 | PASS |
| `python3 - <<'PY' ...` | 真实轨迹里程、筛选后一致性与全量汇总校验通过 | PASS |
| `python3 -u - <<'PY' ...` | `/dashboard/capacity-analysis` 页面联调与轨迹口径展示通过 | PASS |

### Phase 34: 地图展示真实数据
- **Status:** completed
- **Started:** 2026-03-22 14:35
- **Finished:** 2026-03-22 14:42
- Actions taken:
  - `DashboardMap` 页面去除静态场地、项目、车辆样例，改为加载真实 `sites / projects / vehicles / dashboard overview` 数据
  - 接入真实车辆轨迹查询与回放控制，地图支持按车辆与时间范围查询轨迹并绘制 polyline
  - 项目和场地主数据在缺少经纬度时使用稳定回退坐标算法，保证地图分布可用且不再写死样例坐标
- Files created/modified:
  - `xngl-web/src/pages/DashboardMap.tsx`
  - `docs/test-reports/phase34_dashboard_map_real_data_2026-03-22.md`
  - `docs/test-reports/phase34_dashboard_map_real_data_2026-03-22.json`
  - `docs/test-reports/phase34_dashboard_map_real_data_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 地图展示后端构建校验通过 | PASS |
| `cd xngl-web && npm run build` | 地图展示前端构建通过 | PASS |
| `python3 - <<'PY' ...` | 地图页依赖的真实场地/项目/车辆/轨迹/概览接口校验通过 | PASS |
| `python3 -u - <<'PY' ...` | `/dashboard/map` 页面联调与真实数据渲染通过 | PASS |

### Phase 35: 总体分析与看板统计增强
- **Status:** completed
- **Started:** 2026-03-22 15:48
- **Finished:** 2026-03-22 16:01
- Actions taken:
  - 后端 `DashboardOverviewDto` 补充单位总数与活跃单位，并新增 `/api/reports/dashboard/org-analysis` 单位运营分析接口
  - 首页 `Dashboard` 新增单位维度卡片和重点单位运营排行表，补齐“场地 + 项目 + 单位 + 车辆”的总体分析视角
  - `DashboardSites`、`DashboardProjects` 改为复用场地/项目报表接口，支持日/月/年与自定义时间统计，不再只展示单日排行
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AnalyticsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/DashboardOverviewDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/OrgAnalysisItemDto.java`
  - `xngl-web/src/utils/reportApi.ts`
  - `xngl-web/src/pages/Dashboard.tsx`
  - `xngl-web/src/pages/DashboardSites.tsx`
  - `xngl-web/src/pages/DashboardProjects.tsx`
  - `docs/test-reports/phase35_dashboard_panels_2026-03-22.md`
  - `docs/test-reports/phase35_dashboard_panels_2026-03-22.json`
  - `docs/test-reports/phase35_dashboard_panels_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 总体分析/看板后端构建通过 | PASS |
| `cd xngl-web && npm run build` | 总体分析/看板前端构建通过 | PASS |
| `python3 - <<'PY' ...` | 单位分析与场地/项目年报接口校验通过 | PASS |
| `python3 -u - <<'PY' ...` | `/`、`/dashboard/sites`、`/dashboard/projects` 页面联调通过 | PASS |

### Phase 36: 消纳场地红线与设备地图
- **Status:** completed
- **Started:** 2026-03-22 16:03
- **Finished:** 2026-03-22 16:19
- Actions taken:
  - 新增 `037_site_map_layers_and_devices.sql`，为 `biz_site` 补齐经纬度与红线字段，并新增 `biz_site_device` 设备点位表和演示数据
  - 后端新增场地图层 DTO 与真实 `/api/sites/map-layers` 输出，`/api/sites`、`/api/sites/{id}` 同步返回经纬度、红线和设备清单
  - 天地图组件新增 polygon 覆盖物支持，`DashboardMap` 叠加场地红线/设备点位，`SiteDetail` 改为真实红线预览与设备配置列表
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/037_site_map_layers_and_devices.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/Site.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/SiteDevice.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/SiteDeviceMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SitesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteDeviceDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteMapLayerDto.java`
  - `xngl-web/src/components/TiandituMap.tsx`
  - `xngl-web/src/pages/DashboardMap.tsx`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/utils/mapGeometry.ts`
  - `docs/test-reports/phase36_site_map_layers_smoke_2026-03-22.md`
  - `docs/test-reports/phase36_site_map_layers_smoke_2026-03-22.json`
  - `docs/test-reports/phase36_site_map_layers_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 场地红线/设备地图后端构建通过 | PASS |
| `cd xngl-web && npm run build` | 场地红线/设备地图前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 037_site_map_layers_and_devices.sql` | 场地红线与设备点位 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | `/api/sites/map-layers`、`/api/sites`、`/api/sites/{id}` 与 `/dashboard/map`、`/sites/1` 联调通过 | PASS |

### Phase 37: 项目详情合同/场地/配置真实化
- **Status:** completed
- **Started:** 2026-03-22 16:20
- **Finished:** 2026-03-22 16:30
- Actions taken:
  - 新增 `038_project_detail_configs.sql` 和 `biz_project_config`，补齐项目打卡、位置判断、预扣值、线路和违规围栏配置主数据
  - `ProjectsController` 聚合返回项目合同清单、场地清单和项目配置，合同维度增加已消纳/剩余方量，场地维度增加合同方量和已消纳汇总
  - `ProjectDetail` 替换原静态合同/场地/配置卡片，接入真实合同清单、项目场地清单、线路与违规围栏地图预览
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/038_project_detail_configs.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/project/ProjectConfig.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/ProjectConfigMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ProjectsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/project/ProjectDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/project/ProjectContractSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/project/ProjectSiteSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/project/ProjectConfigDto.java`
  - `xngl-web/src/pages/ProjectDetail.tsx`
  - `xngl-web/src/utils/projectApi.ts`
  - `xngl-web/src/utils/mapGeometry.ts`
  - `docs/test-reports/phase37_project_detail_config_smoke_2026-03-22.md`
  - `docs/test-reports/phase37_project_detail_config_smoke_2026-03-22.json`
  - `docs/test-reports/phase37_project_detail_config_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 项目详情合同/场地/配置后端构建通过 | PASS |
| `cd xngl-web && npm run build` | 项目详情合同/场地/配置前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 038_project_detail_configs.sql` | 项目配置 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | `/api/projects/1` 与 `/projects/1?tab=contracts|config` 联调通过 | PASS |

### Phase 38: 车辆禁用清单与解禁
- **Status:** completed
- **Started:** 2026-03-22 16:30
- **Finished:** 2026-03-22 16:41
- Actions taken:
  - 新增 `039_vehicle_violation_disable_records.sql` 和 `biz_vehicle_violation_record`，沉淀违规车辆禁用/解禁记录
  - 后端新增 `/api/vehicles/violations` 查询、`/disable` 单车禁用、`/{id}/release` 提前解禁接口，并同步更新车辆主状态
  - `ViolationsList` 页面改为真实列表，支持关键字/类型/状态/时间筛选、禁用弹窗和提前解禁动作
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/039_vehicle_violation_disable_records.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleViolationRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/VehicleViolationRecordMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleViolationsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleViolationRecordDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleDisableRequestDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleReleaseRequestDto.java`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/pages/ViolationsList.tsx`
  - `docs/test-reports/phase38_vehicle_disable_smoke_2026-03-22.md`
  - `docs/test-reports/phase38_vehicle_disable_smoke_2026-03-22.json`
  - `docs/test-reports/phase38_vehicle_disable_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 车辆禁用/解禁后端构建通过 | PASS |
| `cd xngl-web && npm run build` | 车辆禁用/解禁前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 039_vehicle_violation_disable_records.sql` | 车辆禁用记录 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | `/api/vehicles/violations` 禁用/解禁流转与 `/vehicles/violations` 页面联调通过 | PASS |

### Phase 39: 场地设备配置与运营配置
- **Status:** completed
- **Started:** 2026-03-22 16:42
- **Finished:** 2026-03-22 17:02
- Actions taken:
  - 新增 `040_site_operation_config.sql` 与 `biz_site_operation_config`，为场地排号、人工消纳、范围检测和消纳时长规则提供持久化数据源
  - 新增场地设备新增/编辑和运营配置保存接口，`/api/sites/{id}` 详情同步返回真实运营配置
  - `SiteDetail` 配置页改为真实设备新增/编辑弹窗和运营配置表单保存，不再只读展示静态值
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/040_site_operation_config.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/SiteOperationConfig.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/SiteOperationConfigMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteConfigsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SitesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteDetailDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteOperationConfigDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteDeviceUpsertDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteOperationConfigUpsertDto.java`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `docs/test-reports/phase39_site_device_operation_smoke_2026-03-22.md`
  - `docs/test-reports/phase39_site_device_operation_smoke_2026-03-22.json`
  - `docs/test-reports/phase39_site_device_operation_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests install` | 场地设备/运营配置后端构建通过 | PASS |
| `cd xngl-web && npm run build` | 场地设备/运营配置前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 040_site_operation_config.sql` | 场地运营配置 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | 场地设备新增/编辑、运营配置保存及 `/sites/1?tab=config` 联调通过 | PASS |

### Phase 40: 场地人员配置
- **Status:** completed
- **Started:** 2026-03-22 17:03
- **Finished:** 2026-03-22 18:05
- Actions taken:
  - 新增 `041_site_personnel_config.sql` 与 `biz_site_personnel_config`，沉淀场地与系统用户的账号绑定关系、岗位、班次、职责和启停状态
  - 后端新增 `/api/sites/{id}/personnel`、`/personnel/candidates`、新增、编辑、删除接口，并补齐候选人员组织名称回显
  - `SiteDetail` 配置页新增“人员配置”卡片和真实弹窗，支持基于现有系统用户进行场地人员绑定、编辑和删除
  - 修复逻辑删除后无法重新创建同一场地人员的唯一索引问题，调整唯一键为 `(tenant_id, site_id, user_id, deleted)`
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/041_site_personnel_config.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/SitePersonnelConfig.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/SitePersonnelConfigMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteConfigsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SitePersonnelCandidateDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SitePersonnelConfigDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SitePersonnelUpsertDto.java`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `docs/test-reports/phase40_site_personnel_config_smoke_2026-03-22.md`
  - `docs/test-reports/phase40_site_personnel_config_smoke_2026-03-22.json`
  - `docs/test-reports/phase40_site_personnel_config_ui_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 场地人员配置后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 场地人员配置前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 041_site_personnel_config.sql` | 场地人员配置 patch 落库成功 | PASS |
| `python3 -u - <<'PY' ...` | 场地人员新增/编辑/删除与 `/sites/1?tab=config` UI 回显通过 | PASS |

### Phase 42/43: 场地创建与场地测绘
- **Status:** completed
- **Started:** 2026-03-22 20:05
- **Finished:** 2026-03-22 20:27
- Actions taken:
  - 新增 `043_site_creation_extensions.sql`，为场地主表补齐 `site_level / parent_site_id / management_area / weighbridge_site_id`，支持二级场地与地磅借用配置
  - 新增 `044_site_survey_records.sql` 与 `biz_site_survey_record`，补齐无地磅场地的测绘结算数据源
  - 后端新增真实 `/api/sites` 创建接口，以及 `/api/sites/{id}/surveys` 测绘记录新增、编辑、删除、列表接口
  - 前端 `SitesManagement` 接入新增场地弹窗，`SitesBasicInfo` 增加层级展示，`SiteDetail` 新增借用地磅说明和“场地测绘”页签
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/043_site_creation_extensions.sql`
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/044_site_survey_records.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/Site.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/SiteSurveyRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/SiteSurveyRecordMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SitesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SiteConfigsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteCreateDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteSurveyRecordDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteSurveyUpsertDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/site/SiteDetailDto.java`
  - `xngl-web/src/utils/siteApi.ts`
  - `xngl-web/src/pages/SitesManagement.tsx`
  - `xngl-web/src/pages/SitesBasicInfo.tsx`
  - `xngl-web/src/pages/SiteDetail.tsx`
  - `docs/test-reports/phase42_43_site_creation_survey_smoke_2026-03-22.md`
  - `docs/test-reports/phase42_43_site_creation_survey_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 场地创建/测绘后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 场地创建/测绘前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 043_site_creation_extensions.sql` | 场地创建扩展 patch 落库成功 | PASS |
| `mysql -h127.0.0.1 -P3306 ... < 044_site_survey_records.sql` | 场地测绘 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | 二级场地创建、借用地磅、测绘记录增改删与页面路由检查通过 | PASS |

### Phase 44: 项目违法统计
- **Status:** completed
- **Started:** 2026-03-22 20:28
- **Finished:** 2026-03-22 20:34
- Actions taken:
  - 新增 `/api/reports/projects/violations`，基于 `biz_vehicle_violation_record + biz_vehicle + sys_org` 聚合输出按车队、车牌、处理中队统计
  - 新增 `ProjectViolationAnalysisDto / SummaryDto / StatItemDto`，沉淀项目违法统计返回结构
  - `ProjectsReports` 页面新增违法统计区块，补入总违规、已处理、待处理和三张排行表
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/OperationsReportController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/ProjectViolationAnalysisDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/ProjectViolationSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/report/ProjectViolationStatItemDto.java`
  - `xngl-web/src/utils/reportApi.ts`
  - `xngl-web/src/pages/ProjectsReports.tsx`
  - `docs/test-reports/phase44_project_violation_stats_smoke_2026-03-22.md`
  - `docs/test-reports/phase44_project_violation_stats_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 项目违法统计后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 项目违法统计前端构建通过 | PASS |
| `python3 - <<'PY' ...` | 违规记录创建/解禁、违法统计接口与项目报表路由检查通过 | PASS |

### Phase 45/93/95: 平台对接中心
- **Status:** completed
- **Started:** 2026-03-22 20:45
- **Finished:** 2026-03-22 21:00
- Actions taken:
  - 新增 `045_platform_integration_foundation.sql`，落地 `sys_platform_integration_config`、`sys_sso_login_ticket`、`biz_dam_monitor_record` 三张基础表与默认配置
  - 新增 `/api/platform-integrations/*` 对接中心接口，覆盖概览、配置读写、SSO 票据生成、视频通道聚合、坝体监测记录查询与 mock 同步
  - `AuthController` 新增 `/api/auth/sso/exchange`，`JwtAuthFilter` 放行单点登录换票接口
  - 前端新增 `PlatformIntegrations` 页面与 `platformApi.ts`，接入配置卡片、SSO 票据工具、视频通道表、坝体监测列表，并挂载到系统设置菜单
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/045_platform_integration_foundation.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/system/PlatformIntegrationConfig.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/system/SsoLoginTicket.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/DamMonitorRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/PlatformIntegrationConfigMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/SsoLoginTicketMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/DamMonitorRecordMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/PlatformIntegrationsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AuthController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/auth/JwtAuthFilter.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/platform/*.java`
  - `xngl-web/src/utils/platformApi.ts`
  - `xngl-web/src/pages/PlatformIntegrations.tsx`
  - `xngl-web/src/App.tsx`
  - `xngl-web/src/layouts/MainLayout.tsx`
  - `docs/test-reports/phase45_platform_integrations_smoke_2026-03-22.md`
  - `docs/test-reports/phase45_platform_integrations_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 平台对接后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 平台对接后端安装打包通过 | PASS |
| `cd xngl-web && npm run build` | 平台对接前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../045_platform_integration_foundation.sql"` | 平台对接 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | 登录、对接概览、配置保存、SSO 票据换登、视频通道、坝体 mock 同步均通过 | PASS |
| `python3 - <<'PY' ... playwright ...` | 本机缺少 Playwright Chromium 内核，浏览器自动化未执行 | BLOCKED |

### Phase 46: 小程序登录与场地能力
- **Status:** completed
- **Started:** 2026-03-22 21:01
- **Finished:** 2026-03-22 21:12
- Actions taken:
  - 新增 `046_mini_program_auth_and_sites.sql`，落地 `mini_user_binding` 与 `mini_sms_code_record`
  - 新增 `/api/mini/*` 接口，覆盖短信验证码发送、账号密码+短信码登录、当前用户信息、可访问场地列表、场地详情、场地消纳清单
  - 登录态复用现有 JWT，场地权限优先按 `biz_site_personnel_config` 控制，管理员自动具备全场地访问能力
  - 通过现有场地、设备、合同与领票数据聚合输出小程序场地概览与消纳清单
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/046_mini_program_auth_and_sites.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/miniprogram/MiniUserBinding.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/miniprogram/MiniSmsCodeRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/MiniUserBindingMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/MiniSmsCodeRecordMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniProgramController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/*.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/auth/JwtAuthFilter.java`
  - `docs/test-reports/phase46_mini_login_sites_smoke_2026-03-22.md`
  - `docs/test-reports/phase46_mini_login_sites_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序登录/场地接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序登录/场地接口后端安装打包通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source .../046_mini_program_auth_and_sites.sql"` | 小程序基础 patch 落库成功 | PASS |
| `python3 - <<'PY' ...` | 短信码发送、二次登录、我的信息、场地列表、场地详情、场地消纳清单均通过 | PASS |

### Phase 47: 小程序工单能力
- **Status:** completed
- **Started:** 2026-03-22 21:25
- **Finished:** 2026-03-22 21:38
- Actions taken:
  - 新增 `047_mini_program_work_orders.sql`，落地 `mini_excavation_photo`、`mini_checkin_exception_apply`、`mini_delay_apply`、`mini_feedback`
  - 新增 `MiniWorkOrdersController` 与对应 DTO/Mapper，补齐 `/api/mini/photos`、`/api/mini/checkin-exceptions`、`/api/mini/delay-applies`、`/api/mini/feedbacks`
  - 小程序异常申报、延期申报、问题反馈统一镜像到 `biz_manual_event`，复用现有事件中心与审核日志
  - 修复 `biz_manual_event.uk_manual_event_no` 同秒冲突，事件号改为时间戳 + UUID 短后缀
  - 补强 URL 编码车牌 mock 识别与带时区 ISO 时间解析，提升移动端提交兼容性
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/047_mini_program_work_orders.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/miniprogram/*.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/Mini*.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniWorkOrdersController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/*.java`
  - `docs/test-reports/phase47_mini_work_orders_smoke_2026-03-22.md`
  - `docs/test-reports/phase47_mini_work_orders_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序工单接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序工单接口后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 工单接口重启成功 | PASS |
| `python3 - <<'PY' ...` | 出土拍照、异常申报、延期申报、问题反馈、事件中心回查 8/8 通过 | PASS |

### Phase 48: 小程序单位、事件与报表能力
- **Status:** completed
- **Started:** 2026-03-22 21:40
- **Finished:** 2026-03-22 21:58
- Actions taken:
  - 扩展 `MiniProgramController`，新增小程序当前出土单位、项目列表与项目详情/消纳清单接口
  - 新增 `MiniEventsController`，打通简易事件上报、事件列表、事件详情与审核日志查询
  - 新增 `MiniReportsController`，复用现有项目/场地报表与导出任务链路，补齐移动端导出分享包装
  - 优化当前单位识别逻辑，管理员默认优先回显真实业务单位而非平台根组织
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniProgramController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniEventsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniReportsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniExcavationOrgDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniExcavationProjectDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniEventCreateDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniEventDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniExportShareDto.java`
  - `docs/test-reports/phase48_mini_org_events_reports_smoke_2026-03-22.md`
  - `docs/test-reports/phase48_mini_org_events_reports_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序单位/事件/报表接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序单位/事件/报表接口后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 单位/事件/报表接口重启成功 | PASS |
| `python3 - <<'PY' ...` | 当前单位、项目详情、事件上报、项目/场地报表、导出分享 14/14 通过 | PASS |

### Phase 49: 小程序账号安全能力
- **Status:** completed
- **Started:** 2026-03-22 22:00
- **Finished:** 2026-03-22 22:07
- Actions taken:
  - 扩展 `MiniProgramController`，补齐 `/api/mini/account/send-password-code`、`/api/mini/account/password`、`/api/mini/account/bind` 与 `/api/mini/auth/openid-login`
  - 新增 `MiniPasswordChangeDto`、`MiniAccountBindDto`、`MiniOpenIdLoginRequestDto`，打通短信验证码改密、旧密码校验改密与微信账号绑定参数模型
  - 调整 `JwtAuthFilter` 放行 OpenID 登录入口，保证已绑定微信账号可直接换取小程序登录态
  - 完成管理员账号往返改密烟测，并在测试结束后恢复原密码，验证短信改密、旧密码改密与 OpenID 免密登录链路
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniProgramController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/auth/JwtAuthFilter.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniPasswordChangeDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniAccountBindDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/MiniOpenIdLoginRequestDto.java`
  - `docs/test-reports/phase49_mini_account_security_smoke_2026-03-22.md`
  - `docs/test-reports/phase49_mini_account_security_smoke_2026-03-22.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序账号安全接口后端编译通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序账号安全接口后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 密码修改/账号绑定接口重启成功 | PASS |
| `python3 - <<'PY' ...` | 短信改密、旧密码改密、微信绑定、OpenID 登录 11/11 通过 | PASS |

### Phase 50: 小程序车辆与安全能力
- **Status:** completed
- **Started:** 2026-03-23 08:55
- **Finished:** 2026-03-23 09:03
- Actions taken:
  - 新增 `048_mini_program_vehicle_and_safety.sql`，落地 `mini_manual_disposal_record`、`mini_vehicle_inspection`、`mini_safety_course`、`mini_safety_learning_record`
  - 扩展 `MiniWorkOrdersController`，补齐 `/api/mini/manual-disposals*`，并在提交时真实写入 `biz_contract_ticket`
  - 新增 `MiniVehiclesController`，提供 `/api/mini/vehicles/realtime`、轨迹/停留点查询、车辆检查与车辆电子围栏配置
  - 新增 `MiniSafetyEducationController`，提供课程列表、学习开始、人脸校验、学习完成与学习记录查询
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/048_mini_program_vehicle_and_safety.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/miniprogram/*.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/Mini*.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniWorkOrdersController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniVehiclesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MiniSafetyEducationController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/mini/*.java`
  - `docs/test-reports/phase50_mini_vehicle_and_safety_smoke_2026-03-23.md`
  - `docs/test-reports/phase50_mini_vehicle_and_safety_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 小程序车辆/安全模块后端编译通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../048_mini_program_vehicle_and_safety.sql` | `048` patch 落库成功 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 小程序车辆/安全模块后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带 mini 车辆/安全接口重启成功 | PASS |
| `python3 - <<'PY' ...` | 手动消纳、车辆检查、实时车辆、电子围栏、安全教育 21/21 通过 | PASS |

### Phase 51: 政务网与地磅对接
- **Status:** completed
- **Started:** 2026-03-23 09:08
- **Finished:** 2026-03-23 09:58
- Actions taken:
  - 新增 `049_gov_portal_and_weighbridge_sync.sql`，补齐处置证同步字段、平台同步日志表和地磅记录表，并兼容本地 MySQL 语法差异
  - 扩展 `PlatformIntegrationsController`，新增政务网 mock 同步、政务网同步日志、地磅记录列表、地磅 mock 同步和地磅控制指令接口
  - 扩展 `DisposalPermit` 与前端 `PlatformIntegrations` / `ProjectsPermits` 页面，接入同步来源、外部流水号、同步批次和地磅联调视图
  - 完成平台对接中心与处置证列表页联调，验证政务网同步、同步日志、地磅记录、控制指令全链路可回查
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/049_gov_portal_and_weighbridge_sync.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/site/WeighbridgeRecord.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/system/PlatformSyncLog.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/WeighbridgeRecordMapper.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/PlatformSyncLogMapper.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/disposal/entity/DisposalPermit.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/PlatformIntegrationsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/platform/*.java`
  - `xngl-web/src/pages/PlatformIntegrations.tsx`
  - `xngl-web/src/pages/ProjectsPermits.tsx`
  - `xngl-web/src/utils/platformApi.ts`
  - `xngl-web/src/utils/permitApi.ts`
  - `docs/test-reports/phase51_gov_portal_weighbridge_smoke_2026-03-23.md`
  - `docs/test-reports/phase51_gov_portal_weighbridge_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-web && npm run build` | 平台对接中心与处置证页面前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../049_gov_portal_and_weighbridge_sync.sql` | `049` patch 落库成功 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 政务网/地磅模块后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带政务网/地磅接口重启成功 | PASS |
| `python3 - <<'PY' ...` | 平台对接概览、政务网同步/日志、处置证回查、地磅同步/控制 13/13 通过 | PASS |

### Phase 52: 处置证新增与关联补强
- **Status:** completed
- **Started:** 2026-03-23 10:08
- **Finished:** 2026-03-23 10:32
- Actions taken:
  - 扩展 `DisposalPermitsController`，新增 `projectId/siteId/vehicleNo/bindStatus/sourcePlatform` 服务端筛选参数，并补齐合同、项目、场地、车辆的主数据一致性校验
  - 处置证手工新增时支持根据关联合同自动回填项目和场地，手工新增默认来源标记为 `MANUAL`
  - `ProjectsPermits` 页面切换为服务端真实筛选，新增项目、合同、场地、来源平台、绑定状态筛选，并在表单中补齐合同联动回填
  - 新增重新校准剩余清单文档，后续推进口径切换到 `remaining_status_recalibrated_2026-03-23.md`
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/DisposalPermitsController.java`
  - `xngl-web/src/pages/ProjectsPermits.tsx`
  - `xngl-web/src/utils/permitApi.ts`
  - `docs/test-reports/phase52_disposal_permit_manual_and_binding_smoke_2026-03-23.md`
  - `docs/test-reports/phase52_disposal_permit_manual_and_binding_smoke_2026-03-23.json`
  - `docs/tech-plans/remaining_status_recalibrated_2026-03-23.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 处置证新增/关联模块后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 处置证清单页面前端构建通过 | PASS |
| `cd xngl-service && mvn -pl xngl-service-starter -am install -DskipTests` | 处置证新增/关联模块后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端带处置证新增/关联增强接口重启成功 | PASS |
| `python3 - <<'PY' ...` | 手工新增、详情回查、多维筛选、合同/车辆一致性校验 9/9 通过 | PASS |

### Phase 53: 合同发起 / 变更 / 延期
- **Status:** completed
- **Started:** 2026-03-23 10:33
- **Finished:** 2026-03-23 10:39
- Actions taken:
  - 扩展 `ContractsManagement`，新增在线合同发起入口，支持普通合同和三方合同在线创建并直接提交审批
  - 合同列表新增变更申请、延期申请入口，并在同页新增变更/延期申请列表和提交、通过、驳回动作
  - 扩展 `contractApi`，补齐合同变更申请、延期申请的创建、详情、提交、通过、驳回接口封装
  - 通过 API 验证在线合同发起、退回原因、场地+方量变更审批和延期审批生效
- Files created/modified:
  - `xngl-web/src/pages/ContractsManagement.tsx`
  - `xngl-web/src/utils/contractApi.ts`
  - `docs/test-reports/phase53_contract_apply_change_extension_smoke_2026-03-23.md`
  - `docs/test-reports/phase53_contract_apply_change_extension_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -pl xngl-service-starter -am compile -DskipTests` | 合同申请/变更/延期后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 合同管理页在线申请/变更/延期入口前端构建通过 | PASS |
| `python3 - <<'PY' ...` | 在线普通/三方合同、退回原因、变更申请、延期申请 8/8 通过 | PASS |

### Phase 57: 送货跟踪
- **Status:** completed
- **Started:** 2026-03-23 11:17
- **Finished:** 2026-03-23 11:39
- Actions taken:
  - 扩展 `FleetManagementController`，新增 `/api/fleet-management/tracking/summary`、`/tracking`、`/tracking/{vehicleId}/history` 三个车队维度送货跟踪接口
  - 新增 `FleetTracking*Dto`，基于车辆实时定位、车队调度单、运输计划和轨迹点数据输出实时状态、配送状态、轨迹回放和停留明细
  - 扩展 `fleetApi` 与 `FleetManagement` 页面，新增 `送货跟踪` 页签、跟踪汇总卡片、地图轨迹回放、车辆选择、停留明细和列表联动
  - 完成后端编译、前端构建、服务重启和专项 API/UI 路由烟测，输出 `phase57` 专项测试报告
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/FleetManagementController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/fleet/FleetTrackingSummaryDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/fleet/FleetTrackingItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/fleet/FleetTrackingStopDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/fleet/FleetTrackingHistoryDto.java`
  - `xngl-web/src/utils/fleetApi.ts`
  - `xngl-web/src/pages/FleetManagement.tsx`
  - `docs/test-reports/phase57_fleet_delivery_tracking_smoke_2026-03-23.md`
  - `docs/test-reports/phase57_fleet_delivery_tracking_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 送货跟踪后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 车队管理送货跟踪页签前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 后端安装打包通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到送货跟踪最新代码 | PASS |
| `python3 - <<'PY' ...` | 送货跟踪汇总、列表、配送中过滤、轨迹回放、前端路由 7/7 通过 | PASS |

### Final Regression: 场地资料/报表、违法清单、预警中心、送货跟踪
- **Status:** completed
- **Started:** 2026-03-23 11:39
- **Finished:** 2026-03-23 11:39
- Actions taken:
  - 对最近四个批次的核心页面和接口执行统一回归，覆盖 `sites/documents`、`sites/reports`、`vehicles/violations`、`alerts`、`vehicles/fleet`
  - 校验场地资料汇总、自定义场地报表、自定义趋势、违法汇总/详情、预警汇总/分析/Top Risk、送货跟踪汇总/列表/历史轨迹
  - 输出统一功能联调测试报告 `functional_smoke_2026-03-23.*`
- Files created/modified:
  - `docs/test-reports/functional_smoke_2026-03-23.md`
  - `docs/test-reports/functional_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 - <<'PY' ...` | 最近四批次统一回归 19/19 通过 | PASS |

### Phase 58: 油电卡、人证、车队财务报表增强
- **Status:** completed
- **Started:** 2026-03-23 12:40
- **Finished:** 2026-03-23 13:33
- Actions taken:
  - 扩展 `VehicleCardsController`，补齐油电卡台账导出、流水列表/汇总/导出、充值备注、消费确认与流水落库能力
  - 执行 `051_vehicle_card_transactions.sql`，落地油电卡流水表和演示数据
  - 重写 `VehiclesCards` 页面，升级为“卡片台账 + 流水记录”双视图，支持筛选、导出、充值、消费、绑定解绑
  - 完善 `VehiclePersonnelCertificates` 的到期/欠费筛选导出链路
  - 将 `FleetManagement` 财务管理页接入真实财务汇总、账期/合同号/未结筛选与导出，并补齐车队报表筛选导出
  - 完成后端编译、整仓安装、服务重启和专项烟测，输出 `phase58` 测试报告
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/051_vehicle_card_transactions.sql`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleCardsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclePersonnelCertificatesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/FleetManagementController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleCardRechargeDto.java`
  - `xngl-web/src/utils/vehicleCardApi.ts`
  - `xngl-web/src/utils/vehiclePersonnelApi.ts`
  - `xngl-web/src/utils/fleetApi.ts`
  - `xngl-web/src/pages/VehiclesCards.tsx`
  - `xngl-web/src/pages/VehiclePersonnelCertificates.tsx`
  - `xngl-web/src/pages/FleetManagement.tsx`
  - `docs/test-reports/phase58_vehicle_cards_finance_reports_smoke_2026-03-23.md`
  - `docs/test-reports/phase58_vehicle_cards_finance_reports_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 车队增强相关后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 油电卡/人证/车队财务报表前端构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../051_vehicle_card_transactions.sql` | 油电卡流水 patch 执行通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 整仓安装通过，starter 运行依赖已刷新 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到 Phase58 最新代码 | PASS |
| `python3 - <<'PY' ...` | 油电卡/人证/车队财务报表专项烟测 18/18 通过 | PASS |

### Phase 59: 预警中心、事件管理、安全台账增强
- **Status:** completed
- **Started:** 2026-03-23 13:33
- **Finished:** 2026-03-23 13:56
- Actions taken:
  - 扩展 `AlertsController`，补齐规则编码、超期、发生/关闭时间区间筛选，并新增 `/api/alerts/export` CSV 导出接口
  - 扩展 `ManualEventsController`，补齐项目/场地/车辆、超期、发生/截止/上报/关闭时间筛选，汇总联动当前筛选条件，并新增 `/api/events/export`
  - 扩展 `SecurityInspectionsController`，补齐危险等级、隐患类别、超期、检查/整改截止/复查时间筛选，汇总联动当前筛选条件，并新增 `/api/security/inspections/export`
  - 修正三处详情路由为数字约束，避免 `/export` 被 `/{id}` 路由误匹配
  - 重写 `AlertsMonitor`、`EventsManagement`、`SecurityLedger` 页面筛选区，接入时间筛选、超期筛选、导出按钮和详情增强展示
  - 完成后端编译、前端构建、整仓安装、服务重启和专项 API 烟测，输出 `phase59` 测试报告
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/AlertsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ManualEventsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SecurityInspectionsController.java`
  - `xngl-web/src/utils/alertApi.ts`
  - `xngl-web/src/utils/manualEventApi.ts`
  - `xngl-web/src/utils/securityApi.ts`
  - `xngl-web/src/pages/AlertsMonitor.tsx`
  - `xngl-web/src/pages/EventsManagement.tsx`
  - `xngl-web/src/pages/SecurityLedger.tsx`
  - `docs/test-reports/phase59_plan3_alerts_events_security_smoke_2026-03-23.md`
  - `docs/test-reports/phase59_plan3_alerts_events_security_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile -pl xngl-service-web -am` | Plan 3 相关后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 预警中心/事件管理/安全台账前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 整仓安装通过，starter 依赖已刷新到最新 web/manager/infrastructure | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到 Phase59 最新代码 | PASS |
| `python3 - <<'PY' ...` | Plan 3 专项烟测 17/17 通过 | PASS |

### Phase 60: 配置中心、系统日志、消息中心增强
- **Status:** completed
- **Started:** 2026-03-23 14:00
- **Finished:** 2026-03-23 14:27
- Actions taken:
  - 新增 `CsvExportSupport`，为消息、日志、字典、系统参数、审批规则/材料/流程配置补齐统一 CSV 导出响应
  - 扩展 `MessagesController` 与 `MessageRecordService`，补齐 `/api/messages/read-all`、`/api/messages/export`，支持批量已读与按筛选条件导出
  - 扩展登录/操作/错误日志接口，补齐真实筛选参数与导出能力，并修复操作日志导出在本地表结构差异下的查询链路
  - 扩展 `ApprovalActorRulesController`、`ApprovalMaterialConfigsController`、`ApprovalConfigsController`、`DataDictsController`、`SysParamsController` 的导出与筛选能力
  - 重写/增强 `SystemLogs`、`MessageCenter`、`Dictionary`、`SystemParams`、`ApprovalConfig` 页面，接入真实筛选、导出、批量已读和消息详情展示
  - 新增 `scripts/phase60_config_logs_messages_smoke.py`，完成 API + 前端页面专项烟测，输出 `phase60` 测试报告
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/support/CsvExportSupport.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/MessageRecordService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/MessageRecordServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/log/LoginLogService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/log/LoginLogServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/log/OperationLogService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/log/OperationLogServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/approval/ApprovalActorRuleService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/approval/ApprovalActorRuleServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/MessagesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/LoginLogsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/OperationLogsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ErrorLogsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/DataDictsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SysParamsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ApprovalActorRulesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ApprovalMaterialConfigsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ApprovalConfigsController.java`
  - `xngl-web/src/utils/messageApi.ts`
  - `xngl-web/src/utils/dataDictApi.ts`
  - `xngl-web/src/utils/sysParamApi.ts`
  - `xngl-web/src/utils/approvalApi.ts`
  - `xngl-web/src/pages/SystemLogs.tsx`
  - `xngl-web/src/pages/MessageCenter.tsx`
  - `xngl-web/src/pages/Dictionary.tsx`
  - `xngl-web/src/pages/SystemParams.tsx`
  - `xngl-web/src/pages/ApprovalConfig.tsx`
  - `scripts/phase60_config_logs_messages_smoke.py`
  - `docs/test-reports/phase60_config_logs_messages_smoke_2026-03-23.md`
  - `docs/test-reports/phase60_config_logs_messages_smoke_2026-03-23.json`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile -pl xngl-service-web -am` | Plan 4 相关后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 配置中心/系统日志/消息中心前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 整仓安装通过，starter 依赖已刷新到最新 web/manager/infrastructure | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到 Phase60 最新代码 | PASS |
| `python3 scripts/phase60_config_logs_messages_smoke.py` | Plan 4 专项烟测 28/28 通过 | PASS |

### Phase 61: Plan5 待验收模块统一回归
- **Status:** completed
- **Started:** 2026-03-23 14:30
- **Finished:** 2026-03-23 14:34
- Actions taken:
  - 新增 `scripts/phase61_plan5_pending_acceptance_regression.py`，统一覆盖平台对接、合同管理、处置证三组“已完成待验收”模块
  - 对平台对接执行概览、配置、视频通道、同步日志、SSO 票据、地磅记录 API 回归，并完成页面级可达性校验
  - 对合同管理执行合同清单/详情、导出、导入预检、变更/延期/内拨、入账、结算、月报 API 回归，并完成合同相关页面统一回归
  - 对处置证执行清单、详情及页面回归，确认前序 Phase52 成果纳入统一验收批次
  - 基于本轮结果，将 `45/93/95`、`4/5/6/8/9/10/11/12/13/14`、`34/35` 从“已完成待验收”提升为“已完成已验收”
- Files created/modified:
  - `scripts/phase61_plan5_pending_acceptance_regression.py`
  - `docs/test-reports/phase61_plan5_pending_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase61_plan5_pending_acceptance_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`
  - `docs/tech-plans/remaining_status_recalibrated_2026-03-23.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase61_plan5_pending_acceptance_regression.py` | Plan 5 第一批统一回归 37/37 通过 | PASS |

### Phase 62: 车辆档案与车型库增强
- **Status:** completed
- **Started:** 2026-03-23 15:10
- **Finished:** 2026-03-23 15:58
- Actions taken:
  - 为 `VehiclesController` 补齐 `/api/vehicles/export`、`/api/vehicles/batch-status`、`/api/vehicles/batch-delete`
  - 为 `VehicleModelsController` 补齐 `/api/vehicle-models/export`
  - `VehiclesManagement` 新增车辆台账导出、批量设为在用/维修/禁用、批量删除和表格多选
  - `VehicleModelsManagement` 新增车型库导出入口
  - 新增专项回归脚本 `scripts/phase62_vehicle_master_enhancement_regression.py`，完成源码端点、运行态 API 与前端页面可见性回归
  - 修复 `xngl-service-web`、`xngl-service-manager` 的 Lombok annotation processor 编译链，清理全仓 Java 编译坏账
  - 完成全量 `mvn compile`、`mvn install` 和后端重启，解除此前 `Alert* / Analytics / Approval*` 编译阻塞
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleModelsController.java`
  - `xngl-service/xngl-service-web/pom.xml`
  - `xngl-service/xngl-service-manager/pom.xml`
  - `xngl-web/src/utils/vehicleApi.ts`
  - `xngl-web/src/utils/vehicleModelApi.ts`
  - `xngl-web/src/pages/VehiclesManagement.tsx`
  - `xngl-web/src/pages/VehicleModelsManagement.tsx`
  - `scripts/phase62_vehicle_master_enhancement_regression.py`
  - `docs/test-reports/phase62_vehicle_master_enhancement_regression_2026-03-23.md`
  - `docs/test-reports/phase62_vehicle_master_enhancement_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`
  - `docs/tech-plans/remaining_status_recalibrated_2026-03-23.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-web && npm run build` | 车辆档案/车型库增强后的前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests compile` | Lombok 编译链修复后全量编译通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 全量安装通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已按最新代码重启到 `8090` | PASS |
| `python3 scripts/phase62_vehicle_master_enhancement_regression.py` | 源码端点 + 运行态 API + 前端页面可见性回归 15/15 通过 | PASS |

### Phase 63: 保险维保维修清理能力增强
- **Status:** completed
- **Started:** 2026-03-23 16:20
- **Finished:** 2026-03-23 16:31
- Actions taken:
  - 为 `VehicleInsurancesController` 补齐保险记录删除接口
  - 为 `VehicleMaintenancePlansController` 补齐维保计划删除接口，并增加“存在执行记录时禁止删除”的保护
  - 为 `VehicleRepairsController` 补齐维修单删除接口，并增加“已完成维修单禁止删除”的保护
  - `VehicleInsurances`、`VehicleMaintenancePlans`、`VehicleRepairs` 页面新增删除按钮与确认弹窗，接入真实删除后刷新
  - 新增专项回归脚本 `scripts/phase63_vehicle_ops_cleanup_regression.py`，覆盖保险/维保/维修的创建删除链路与页面可见性
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleInsurancesController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleMaintenancePlansController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleRepairsController.java`
  - `xngl-web/src/utils/vehicleInsuranceApi.ts`
  - `xngl-web/src/utils/vehicleMaintenanceApi.ts`
  - `xngl-web/src/utils/vehicleRepairApi.ts`
  - `xngl-web/src/pages/VehicleInsurances.tsx`
  - `xngl-web/src/pages/VehicleMaintenancePlans.tsx`
  - `xngl-web/src/pages/VehicleRepairs.tsx`
  - `scripts/phase63_vehicle_ops_cleanup_regression.py`
  - `docs/test-reports/phase63_vehicle_ops_cleanup_regression_2026-03-23.md`
  - `docs/test-reports/phase63_vehicle_ops_cleanup_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 删除接口与保护逻辑接入后后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 保险/维保/维修页面增强后前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 最新包安装通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到最新 `phase63` 代码 | PASS |
| `python3 scripts/phase63_vehicle_ops_cleanup_regression.py` | 保险/维保/维修创建删除链路与页面可见性回归 8/8 通过 | PASS |

### Phase 64: 安全台账时间线与人员关联增强
- **Status:** completed
- **Started:** 2026-03-23 17:05
- **Finished:** 2026-03-23 17:28
- Actions taken:
  - 新增 `biz_security_inspection_action` 台账动作日志表和 `052_security_inspection_actions.sql`，为历史检查记录补种创建动作
  - `SecurityInspectionsController` 补齐人员对象归一化、`userId`/隐患类别/复查时间等筛选、删除接口、检查动作日志写入与详情时间线返回
  - 安全台账导出增加“关联对象名称/关联人员”口径字段，人员对象详情回显真实人员姓名与手机号
  - `SecurityLedger` 页面补齐项目/场地/车辆/人员/隐患类别/复查时间筛选，新增人员对象真实选择、详情时间线展示和删除动作
  - 新增专项回归脚本 `scripts/phase64_security_ledger_detail_regression.py`，覆盖创建、筛选、详情、整改、导出、删除及页面时间线可见性
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/052_security_inspection_actions.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/security/SecurityInspectionAction.java`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/mapper/SecurityInspectionActionMapper.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SecurityInspectionsController.java`
  - `xngl-web/src/utils/securityApi.ts`
  - `xngl-web/src/pages/SecurityLedger.tsx`
  - `scripts/phase64_security_ledger_detail_regression.py`
  - `docs/test-reports/phase64_security_ledger_detail_regression_2026-03-23.md`
  - `docs/test-reports/phase64_security_ledger_detail_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 安全台账后端增强后编译通过 | PASS |
| `cd xngl-web && npm run build` | 安全台账前端增强后构建通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../052_security_inspection_actions.sql` | 动作日志表与历史补录 patch 已成功应用 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 最新后端安装通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到最新 `phase64` 代码 | PASS |
| `python3 scripts/phase64_security_ledger_detail_regression.py` | 安全台账创建/筛选/详情/整改/导出/删除/页面时间线回归 8/8 通过 | PASS |

### Phase 65: 维保信息录入与执行明细增强
- **Status:** completed
- **Started:** 2026-03-23 17:33
- **Finished:** 2026-03-23 18:00
- Actions taken:
  - 新增 `053_vehicle_maintenance_detail_extensions.sql`，为维保执行记录补齐人工费、材料费、外协费、异常描述、处理结果、维修技师、验收人、签字状态和附件字段，并为历史数据补种明细值
  - `VehicleMaintenancePlansController` 补齐计划详情、计划执行历史、执行记录详情接口，并增强执行录入、计划导出、记录导出口径
  - `VehicleMaintenancePlans` 页面新增计划详情抽屉、执行历史列表、记录详情抽屉，并在执行维保表单中接入费用拆分、异常描述、处理结果、技师/验收/签字和附件录入
  - `vehicleMaintenanceApi` 新增详情接口与扩展字段映射
  - 新增专项回归脚本 `scripts/phase65_vehicle_maintenance_detail_regression.py`，覆盖计划创建、计划详情、执行明细、记录详情、历史记录、导出和页面详情抽屉
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/053_vehicle_maintenance_detail_extensions.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleMaintenanceRecord.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleMaintenanceExecuteDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleMaintenanceRecordListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleMaintenancePlanListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleMaintenancePlansController.java`
  - `xngl-web/src/utils/vehicleMaintenanceApi.ts`
  - `xngl-web/src/pages/VehicleMaintenancePlans.tsx`
  - `scripts/phase65_vehicle_maintenance_detail_regression.py`
  - `docs/test-reports/phase65_vehicle_maintenance_detail_regression_2026-03-23.md`
  - `docs/test-reports/phase65_vehicle_maintenance_detail_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 维保明细增强后后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 维保计划/记录页面增强后前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 最新后端安装通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../053_vehicle_maintenance_detail_extensions.sql` | 维保执行记录明细字段 patch 已成功应用 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到最新 `phase65` 代码 | PASS |
| `python3 scripts/phase65_vehicle_maintenance_detail_regression.py` | 维保计划创建/详情/执行明细/记录详情/导出/页面抽屉回归 8/8 通过 | PASS |

### Phase 66: 车辆维修单详情与完工台账增强
- **Status:** completed
- **Started:** 2026-03-23 18:02
- **Finished:** 2026-03-23 18:10
- Actions taken:
  - 新增 `054_vehicle_repair_detail_extensions.sql`，为维修申请单补齐诊断结论、安全影响、维修负责人、维修技师、验收结果、签字状态、附件以及人工/配件/其他费用字段，并补种历史明细数据
  - `VehicleRepairsController` 补齐维修单详情接口、完工明细入参落库、详情口径映射和导出口径增强，支持差异金额与签字状态文案返回
  - `VehicleRepairs` 页面新增维修单详情抽屉，编辑表单补齐诊断结论/安全影响，完工表单补齐负责人、技师、验收、签字、附件和费用拆分录入
  - `vehicleRepairApi` 扩展详情接口和维修单明细字段映射
  - 新增专项回归脚本 `scripts/phase66_vehicle_repairs_detail_regression.py`，覆盖维修申请创建、审批、完工、详情、导出和页面详情抽屉可见性
- Files created/modified:
  - `xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/054_vehicle_repair_detail_extensions.sql`
  - `xngl-service/xngl-service-infrastructure/src/main/java/com/xngl/infrastructure/persistence/entity/vehicle/VehicleRepairOrder.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleRepairOrderUpsertDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleRepairCompleteDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/dto/vehicle/VehicleRepairOrderListItemDto.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleRepairsController.java`
  - `xngl-web/src/utils/vehicleRepairApi.ts`
  - `xngl-web/src/pages/VehicleRepairs.tsx`
  - `scripts/phase66_vehicle_repairs_detail_regression.py`
  - `docs/test-reports/phase66_vehicle_repairs_detail_regression_2026-03-23.md`
  - `docs/test-reports/phase66_vehicle_repairs_detail_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 维修单详情增强后后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 维修管理页面增强后前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 最新后端安装通过 | PASS |
| `mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < .../054_vehicle_repair_detail_extensions.sql` | 维修单明细字段 patch 已成功应用 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到最新 `phase66` 代码 | PASS |
| `python3 scripts/phase66_vehicle_repairs_detail_regression.py` | 维修申请创建/审批/完工/详情/导出/页面抽屉回归 7/7 通过 | PASS |

### Phase 67: 安全台账对象安全档案摘要增强
- **Status:** completed
- **Started:** 2026-03-23 18:10
- **Finished:** 2026-03-23 18:27
- Actions taken:
  - `SecurityInspectionsController` 复用现有证照、学习、保险、维保、预警、场地资料、场地设备数据源，为 `PERSON/VEHICLE/SITE` 三类检查对象补齐安全档案摘要聚合
  - 详情接口新增 `relatedProfile` 与 `relatedProfileSummary`，导出 CSV 新增安全档案摘要、证照/学习/保险/维保/资料/设备/未闭环预警等列
  - `SecurityLedger` 页面列表补充档案摘要提示，详情抽屉新增“人员安全档案 / 车辆安全档案 / 场地安全档案”区块
  - 修复安全检查编号仅到秒导致的唯一键冲突问题，编号生成升级为毫秒 + 纳秒尾缀
  - 新增专项回归脚本 `scripts/phase67_security_related_profile_regression.py`，覆盖三类对象的档案摘要运行态、导出和页面抽屉可见性
- Files created/modified:
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SecurityInspectionsController.java`
  - `xngl-web/src/utils/securityApi.ts`
  - `xngl-web/src/pages/SecurityLedger.tsx`
  - `scripts/phase67_security_related_profile_regression.py`
  - `docs/test-reports/phase67_security_related_profile_regression_2026-03-23.md`
  - `docs/test-reports/phase67_security_related_profile_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `cd xngl-service && mvn -q -DskipTests compile` | 安全台账对象档案摘要增强后后端编译通过 | PASS |
| `cd xngl-web && npm run build` | 安全台账详情抽屉与摘要增强后前端构建通过 | PASS |
| `cd xngl-service && mvn -q -DskipTests install` | 最新后端安装通过 | PASS |
| `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter` | 后端已重启到最新 `phase67` 代码 | PASS |
| `python3 scripts/phase67_security_related_profile_regression.py` | 人员/车辆/场地安全档案摘要运行态、导出、页面抽屉回归 8/8 通过 | PASS |

### Phase 68: 车辆与车队模块统一回归
- **Status:** completed
- **Started:** 2026-03-23 18:28
- **Finished:** 2026-03-23 18:35
- Actions taken:
  - 新增统一回归脚本 `scripts/phase68_vehicle_fleet_unified_regression.py`，覆盖 `116-124` 与 `130-133` 相关车辆/车队模块
  - 回归范围包含车辆主数据、车型、保险、维保、维修、油电卡、人证、车队财务与报表的核心列表、汇总、导出接口
  - 同步补充前端页面可见性检查，验证 `车辆与运力资源 / 车型管理 / 维保计划 / 维修管理 / 油电卡管理 / 人证管理 / 车队管理` 路由可达
  - 修正统一回归脚本对真实接口返回字段和页面标题的口径，形成可复用的批量验收脚本
- Files created/modified:
  - `scripts/phase68_vehicle_fleet_unified_regression.py`
  - `docs/test-reports/phase68_vehicle_fleet_unified_regression_2026-03-23.md`
  - `docs/test-reports/phase68_vehicle_fleet_unified_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase68_vehicle_fleet_unified_regression.py` | 车辆主数据/车型/保险/维保/维修/油电卡/人证/车队财务报表统一回归 29/29 通过 | PASS |

### Phase 69: 事件与预警规则统一回归
- **Status:** completed
- **Started:** 2026-03-23 18:55
- **Finished:** 2026-03-23 18:57
- Actions taken:
  - 新增组合脚本 `scripts/phase69_alerts_events_unified_regression.py`，统一覆盖 `51-62`
  - 打通人工事件创建、更新、提交、退回、二次提交、审批、关闭及导出、汇总链路
  - 打通预警生成、查询、处理、关闭、汇总、分析、Top 风险、围栏状态与导出链路
  - 打通预警规则、预警围栏、推送配置的新增、更新、状态切换运行态验证
  - 补充 `预警与监控中心 / 事件管理 / 预警配置 / 安全台账管理` 四个页面路由可见性回归
- Files created/modified:
  - `scripts/phase69_alerts_events_unified_regression.py`
  - `docs/test-reports/phase69_alerts_events_unified_regression_2026-03-23.md`
  - `docs/test-reports/phase69_alerts_events_unified_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase69_alerts_events_unified_regression.py` | 事件管理、预警中心、预警配置与安全台账统一回归 13/13 通过 | PASS |

### Phase 70: 治理与配置中心统一回归
- **Status:** completed
- **Started:** 2026-03-23 19:00
- **Finished:** 2026-03-23 19:04
- Actions taken:
  - 新增组合脚本 `scripts/phase70_governance_unified_regression.py`，统一覆盖 `66-83 / 96`
  - 打通组织、角色、人员三条治理主链路的真实创建、更新、权限/数据范围配置、绑定关系与清理回归
  - 打通数据字典、审批人规则、办事材料、审批流程、系统参数的新增、更新、状态切换、导出与删除回归
  - 打通登录日志、操作日志、错误日志导出校验，以及消息汇总、未读标记、批量已读、导出与清理校验
  - 补充 `组织与人员管理 / 角色与权限管理 / 数据字典 / 审核审批配置 / 系统参数 / 系统日志 / 消息管理` 七个页面路由可见性回归
- Files created/modified:
  - `scripts/phase70_governance_unified_regression.py`
  - `docs/test-reports/phase70_governance_unified_regression_2026-03-23.md`
  - `docs/test-reports/phase70_governance_unified_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase70_governance_unified_regression.py` | 组织/角色/人员、字典、审批配置、系统参数、日志、消息与治理页统一回归 16/16 通过 | PASS |

### Phase 71: 经营基础主数据与查询链路统一回归
- **Status:** completed
- **Started:** 2026-03-23 19:10
- **Finished:** 2026-03-23 19:18
- Actions taken:
  - 新增组合脚本 `scripts/phase71_business_master_regression.py`，统一覆盖 `16-21 / 24 / 36 / 37 / 39 / 63 / 64 / 66`
  - 打通单位概览、单位详情、单位项目统计、合同分组、单位新增/编辑与 SQL 清理回归
  - 打通项目列表/详情、项目交款汇总/登记/撤销与 SQL 清理回归
  - 打通处置证清单/详情、打卡数据查询/作废/回滚、消纳信息查询、场地列表/详情/消纳清单/地图图层、车辆概览/列表/导出/单位运力/车队汇总
  - 补充 `单位管理 / 消纳项目清单 / 项目交款管理 / 处置证清单 / 打卡数据 / 消纳信息 / 消纳场地管理 / 全局消纳清单 / 场地基础信息 / 车辆与运力资源` 十个页面路由可见性回归
  - 校正项目交款列表的测试口径，改为按项目全量结果匹配 `paymentNo`，避免误把 `keyword` 过滤口径当成支付单号搜索
- Files created/modified:
  - `scripts/phase71_business_master_regression.py`
  - `docs/test-reports/phase71_business_master_regression_2026-03-23.md`
  - `docs/test-reports/phase71_business_master_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase71_business_master_regression.py` | 单位、项目、项目交款、处置证、打卡、消纳、场地、车辆与经营基础页面统一回归 11/11 通过 | PASS |

### Phase 72: 合同结算与统计链路统一回归
- **Status:** completed
- **Started:** 2026-03-23 19:19
- **Finished:** 2026-03-23 19:25
- Actions taken:
  - 新增组合脚本 `scripts/phase72_contract_settlement_regression.py`，统一覆盖 `1 / 2 / 3 / 7 / 15`
  - 打通合同清单、合同统计、合同详情、审批记录、材料、发票、领票记录的真实只读回归
  - 打通合同入账的创建、详情、冲销与 SQL 清理回归，确认分次入账链路运行态正常
  - 打通月报统计的月度汇总、趋势、类型统计、日报、年报、自定义期间统计及月报导出任务创建
  - 打通场地结算列表/详情/统计，并验证场地结算生成后可清理回滚
  - 补充 `合同与财务结算 / 合同详情 / 合同入账管理 / 月报统计 / 结算管理` 五个页面路由可见性回归
- Files created/modified:
  - `scripts/phase72_contract_settlement_regression.py`
  - `docs/test-reports/phase72_contract_settlement_regression_2026-03-23.md`
  - `docs/test-reports/phase72_contract_settlement_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase72_contract_settlement_regression.py` | 合同详情、合同入账、月报统计、场地结算与合同结算页面统一回归 5/5 通过 | PASS |

### Phase 73: 项目配置与统计链路统一回归
- **Status:** completed
- **Started:** 2026-03-23 19:26
- **Finished:** 2026-03-23 19:32
- Actions taken:
  - 新增组合脚本 `scripts/phase73_project_runtime_regression.py`，统一覆盖 `25 / 26 / 27 / 28 / 29 / 32`
  - 打通项目日报列表、关键字过滤与日报导出任务创建，确认 `项目日报` 运行态可用
  - 打通项目详情配置数据读取，校验打卡配置、位置判断、出土预扣值、线路 GeoJSON、违规围栏 GeoJSON 五类配置均已真实落库并可回显
  - 打通项目报表月/日/年统计汇总、列表、趋势与导出任务创建，确认项目统计链路运行态正常
  - 打通项目违规分析汇总及按关键字过滤回归，确认按车牌等底层字段过滤的口径正常
  - 补充 `项目日报 / 项目报表 / 项目配置` 三个页面路由可见性回归
  - 修正违规分析脚本的过滤口径误判，避免把聚合名称误当成接口 `keyword` 过滤字段
- Files created/modified:
  - `scripts/phase73_project_runtime_regression.py`
  - `docs/test-reports/phase73_project_runtime_regression_2026-03-23.md`
  - `docs/test-reports/phase73_project_runtime_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase73_project_runtime_regression.py` | 项目日报、项目报表、项目配置、线路与违规围栏、违规统计页面统一回归 7/7 通过 | PASS |

### Phase 74: 车队基础链路统一回归
- **Status:** completed
- **Started:** 2026-03-23 19:33
- **Finished:** 2026-03-23 19:36
- Actions taken:
  - 新增组合脚本 `scripts/phase74_fleet_core_regression.py`，统一覆盖 `125 / 126 / 127 / 128 / 129`
  - 打通车队概览、车队清单、运输计划、调度申请的真实查询回归，确认车队基础查询链路可用
  - 打通车队信息新增、编辑与关键字过滤回归，并通过 SQL 自动清理临时车队数据
  - 打通运输计划新增、编辑、按车队过滤回归，并通过 SQL 自动清理临时计划和车队数据
  - 打通调度申请新增、编辑、审批通过、按状态过滤回归，并通过 SQL 自动清理临时调度/计划/车队数据
  - 补充 `车队管理 / 车队维护 / 运输计划 / 调度审批` 四个页面可见性回归
- Files created/modified:
  - `scripts/phase74_fleet_core_regression.py`
  - `docs/test-reports/phase74_fleet_core_regression_2026-03-23.md`
  - `docs/test-reports/phase74_fleet_core_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase74_fleet_core_regression.py` | 车队查询、车队维护、运输计划、调度申请、调度审批统一回归 6/6 通过 | PASS |

### Phase 75: 全站自动化回归与最终验收
- **Status:** completed
- **Started:** 2026-03-23 19:37
- **Finished:** 2026-03-23 19:43
- Actions taken:
  - 新增总控脚本 `scripts/phase75_final_acceptance_regression.py`，串行拉起 `phase61-74` 已落地运行态回归批次并汇总结果
  - 修复 `scripts/phase61_plan5_pending_acceptance_regression.py` 的前端登录态注入方式，改为 `context.add_init_script`，消除访问 `平台对接中心` 页面时被登录守卫抢先重定向的偶发失败
  - 重跑 `平台对接 / 合同管理 / 处置证 / 车辆与车队 / 安全台账 / 预警事件 / 治理配置 / 经营基础 / 合同结算 / 项目统计 / 车队基础链路` 共 14 个阶段脚本
  - 生成最终验收汇总报告，确认当前总验收统计为 `179 / 179 PASS`
- Files created/modified:
  - `scripts/phase61_plan5_pending_acceptance_regression.py`
  - `scripts/phase75_final_acceptance_regression.py`
  - `docs/test-reports/phase75_final_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase75_final_acceptance_regression_2026-03-23.json`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase61_plan5_pending_acceptance_regression.py` | 平台对接、合同管理、处置证统一回归 37/37 通过 | PASS |
| `python3 scripts/phase75_final_acceptance_regression.py` | `phase61-74` 全站自动化回归与最终验收 179/179 通过 | PASS |

### Phase 76: 事件/安全台账消息联动回归
- **Status:** completed
- **Started:** 2026-03-23 19:44
- **Finished:** 2026-03-23 19:50
- Actions taken:
  - 扩展 `MessageRecordService`，新增面向指定接收人的业务消息推送能力，统一沉淀 `SYSTEM` 渠道消息写入
  - 打通人工事件 `提交/通过/驳回/关闭` 与安全台账 `整改` 动作后的消息推送，补齐 `linkUrl / bizType / bizId / category` 业务跳转元数据
  - 为消息中心新增“查看业务/前往业务”入口，并接入 `MANUAL_EVENT / SECURITY_INSPECTION / SETTLEMENT / CONTRACT_TRANSFER / CONTRACT` 路由解析
  - 为事件管理页与安全台账页增加基于查询参数的详情自动展开能力，支持从消息中心直达业务详情
  - 修复 `scripts/phase76_message_linkage_regression.py` 的登录用户 ID 解析与安全台账响应 ID 提取逻辑，完成事件与安全台账消息联动运行态回归
- Files created/modified:
  - `scripts/phase76_message_linkage_regression.py`
  - `docs/test-reports/phase76_message_linkage_regression_2026-03-23.md`
  - `docs/test-reports/phase76_message_linkage_regression_2026-03-23.json`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/MessageRecordService.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/message/MessageRecordServiceImpl.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ManualEventsController.java`
  - `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/SecurityInspectionsController.java`
  - `xngl-web/src/pages/MessageCenter.tsx`
  - `xngl-web/src/pages/EventsManagement.tsx`
  - `xngl-web/src/pages/SecurityLedger.tsx`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase76_message_linkage_regression.py` | 事件消息写入、安全台账消息写入、消息中心业务跳转统一回归 4/4 通过 | PASS |

### Phase 77: 合同与结算消息深链回归
- **Status:** completed
- **Started:** 2026-03-23 20:00
- **Finished:** 2026-03-23 20:17
- Actions taken:
  - 将内拨申请与结算审批消息的 `linkUrl` 从列表页升级为精确详情深链，分别落到 `transferId` 与 `settlementId`
  - 为消息中心补充旧消息链接兼容逻辑，历史 `/contracts/transfers`、`/contracts/settlements` 消息也会自动补全详情参数
  - 为 `ContractTransfers` 与 `Settlements` 页面增加基于查询参数的详情抽屉自动展开能力，支持从消息中心直达业务详情
  - 新增 `scripts/phase77_contract_message_jump_regression.py`，覆盖内拨申请驳回消息、结算驳回消息的写入与消息中心跳转回归
  - 重新编译前后端、安装后端模块并重启本地服务，确认运行态使用最新深链实现
- Files created/modified:
  - `scripts/phase77_contract_message_jump_regression.py`
  - `docs/test-reports/phase77_contract_message_jump_regression_2026-03-23.md`
  - `docs/test-reports/phase77_contract_message_jump_regression_2026-03-23.json`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/ContractApplyServiceImpl.java`
  - `xngl-service/xngl-service-manager/src/main/java/com/xngl/manager/contract/SettlementServiceImpl.java`
  - `xngl-web/src/pages/MessageCenter.tsx`
  - `xngl-web/src/pages/ContractTransfers.tsx`
  - `xngl-web/src/pages/Settlements.tsx`
  - `progress.md`
  - `task_plan.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `mvn -q -DskipTests compile -pl xngl-service-manager -am` | 后端消息深链相关模块编译通过 | PASS |
| `npm run build` | 前端构建通过 | PASS |
| `mvn -q -DskipTests install` | 后端模块安装通过 | PASS |
| `python3 scripts/phase77_contract_message_jump_regression.py` | 内拨申请消息写入、结算消息写入、消息中心深链跳转统一回归 4/4 通过 | PASS |

### Phase 78-86: PC剩余模块 Acceptance 清仓
- **Status:** completed
- **Started:** 2026-03-23 20:20
- **Finished:** 2026-03-23 20:27
- Actions taken:
  - 新增通用聚合器 `scripts/acceptance_aggregate_helper.py`，统一收敛 acceptance 批次报告生成逻辑
  - 新增 `phase78-85` 八个 acceptance 聚合脚本，将此前已通过的经营基础、合同结算、项目配置、治理日志消息、预警事件安全、车辆安全、车队运营、车队财务报表运行态脚本重新按 A/B/C 三类归并
  - 新增 `phase86_pc_remaining_full_acceptance_regression.py`，串行执行 `phase78-85`，形成新的 PC 剩余模块总验收报告
  - 基于 `phase62-77` 与 `phase78-86` 结果，确认此前“待增强 75 模块”已全部具备 acceptance 证据，可整体移出待增强池
- Files created/modified:
  - `scripts/acceptance_aggregate_helper.py`
  - `scripts/phase78_business_core_acceptance_regression.py`
  - `scripts/phase79_project_config_acceptance_regression.py`
  - `scripts/phase80_governance_logs_acceptance_regression.py`
  - `scripts/phase81_alerts_security_acceptance_regression.py`
  - `scripts/phase82_risk_model_acceptance_regression.py`
  - `scripts/phase83_vehicle_safety_acceptance_regression.py`
  - `scripts/phase84_fleet_operations_acceptance_regression.py`
  - `scripts/phase85_fleet_finance_reports_acceptance_regression.py`
  - `scripts/phase86_pc_remaining_full_acceptance_regression.py`
  - `docs/test-reports/phase78_business_core_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase79_project_config_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase80_governance_logs_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase81_alerts_security_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase82_risk_model_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase83_vehicle_safety_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase84_fleet_operations_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase85_fleet_finance_reports_acceptance_regression_2026-03-23.md`
  - `docs/test-reports/phase86_pc_remaining_full_acceptance_regression_2026-03-23.md`
  - `progress.md`
  - `task_plan.md`
  - `docs/tech-plans/remaining_status_recalibrated_2026-03-23.md`

## Verification Results
| Command | Result | Status |
|---------|--------|--------|
| `python3 scripts/phase78_business_core_acceptance_regression.py` | 经营基础与合同结算 acceptance 17/17 通过 | PASS |
| `python3 scripts/phase79_project_config_acceptance_regression.py` | 项目配置 acceptance 7/7 通过 | PASS |
| `python3 scripts/phase80_governance_logs_acceptance_regression.py` | 治理、日志、消息 acceptance 24/24 通过 | PASS |
| `python3 scripts/phase81_alerts_security_acceptance_regression.py` | 预警、事件、安全台账 acceptance 33/33 通过 | PASS |
| `python3 scripts/phase82_risk_model_acceptance_regression.py` | 研判模型 acceptance 13/13 通过 | PASS |
| `python3 scripts/phase83_vehicle_safety_acceptance_regression.py` | 车辆与安全台账 acceptance 83/83 通过 | PASS |
| `python3 scripts/phase84_fleet_operations_acceptance_regression.py` | 车队运营 acceptance 35/35 通过 | PASS |
| `python3 scripts/phase85_fleet_finance_reports_acceptance_regression.py` | 车队财务报表 acceptance 29/29 通过 | PASS |
| `python3 scripts/phase86_pc_remaining_full_acceptance_regression.py` | PC 剩余模块总验收 241/241 通过 | PASS |
