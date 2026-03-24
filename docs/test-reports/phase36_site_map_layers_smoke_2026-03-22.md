# Phase 36 消纳场地红线与设备地图冒烟测试报告

- 测试时间：2026-03-22 16:19:53 CST
- 测试范围：`91 消纳场地`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：8
- 通过：8
- 失败：0

## 用例明细

- [PASS] API login：admin/admin 登录成功并获取 token
- [PASS] API site map layers：场地图层返回 1 个场地，首个场地设备 3 个
- [PASS] API sites list geo fields：场地列表已返回经纬度字段，首个坐标 120.17652, 30.25784
- [PASS] API site detail boundary and devices：场地详情返回红线和 3 个设备点位
- [PASS] UI dashboard map page load：地图展示页成功打开并进入已登录态
- [PASS] UI dashboard map site overlays：页面已展示场地设备统计、场地名称和红线状态文案
- [PASS] UI site detail config devices：场地配置页已展示真实设备清单与在线状态
- [PASS] UI site detail geo preview：场地详情页已展示红线预览卡片与中心坐标

## 产出文件

- API 结果：`docs/test-reports/phase36_site_map_layers_smoke_2026-03-22.json`
- UI 结果：`docs/test-reports/phase36_site_map_layers_ui_2026-03-22.json`

## 结论

- 场地图层接口已返回真实红线 GeoJSON 和设备点位数据，场地列表也补齐了经纬度字段。
- 天地图页面已叠加场地红线范围和设备分布，场地详情页也已替换为真实设备配置与地图预览。
