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
