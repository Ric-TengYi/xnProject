# phase62_vehicle_master_enhancement_regression_2026-03-23

- 日期：2026-03-23
- 通过：15
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| vehicles_controller_export_endpoint | PASS | VehiclesController.java contains @GetMapping("/export") |
| vehicles_controller_batch_status_endpoint | PASS | VehiclesController.java contains @PutMapping("/batch-status") |
| vehicles_controller_batch_delete_endpoint | PASS | VehiclesController.java contains @PostMapping("/batch-delete") |
| vehicle_models_controller_export_endpoint | PASS | VehicleModelsController.java contains @GetMapping("/export") |
| vehicles_page_batch_actions | PASS | VehiclesManagement.tsx contains 批量设为禁用 |
| vehicles_page_export_action | PASS | VehiclesManagement.tsx contains 导出台账 |
| vehicle_models_page_export_action | PASS | VehicleModelsManagement.tsx contains 导出车型库 |
| vehicles_export_runtime | PASS | bytes=2090 |
| vehicles_create_temp_runtime | PASS | id=13 |
| vehicles_batch_status_runtime | PASS | updated=1, status=3 |
| vehicles_batch_delete_runtime | PASS | deleted=1 |
| vehicle_models_export_runtime | PASS | bytes=442 |
| vehicles_page_buttons | PASS | 车辆页面导出/批量按钮已渲染 |
| vehicle_models_page_buttons | PASS | 车型页面导出按钮已渲染 |

## 备注

- 前端构建 `npm run build` 已通过。
- 后端全量 `mvn -q -DskipTests compile` 已通过，`mvn spring-boot:run -pl xngl-service-starter` 已重启成功。
- 本报告同时覆盖源码落地校验、运行态 API 校验与前端页面可见性回归。