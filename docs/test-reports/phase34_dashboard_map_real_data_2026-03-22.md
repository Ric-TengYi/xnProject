# Phase 34 地图展示真实数据冒烟测试报告

- 测试时间：2026-03-22 14:42 CST
- 测试范围：`90 地图展示`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：9
- 通过：9
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API sites list：场地列表返回 `1` 条真实记录
- [PASS] API projects list：项目列表返回 `1` 条真实记录
- [PASS] API vehicles list：车辆列表返回 `8` 条真实记录
- [PASS] API track history：车辆 `1` 轨迹查询返回 `4` 个轨迹点
- [PASS] API dashboard overview：总体概览返回在线车辆 `3`、预警 `4`
- [PASS] UI page load：地图监控页面成功打开
- [PASS] UI distribution panels：左侧场地、项目、车辆分布面板已渲染
- [PASS] UI real data binding：页面已展示真实项目和车辆数据，而非静态样例

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# 真实登录后校验 /sites /projects /vehicles /vehicles/{id}/track-history /reports/dashboard/overview
PY
python3 -u - <<'PY'
# 通过 localStorage 注入 token 打开 /dashboard/map，验证页面联调与真实数据渲染
PY
```

## 结论

- `DashboardMap` 已去除静态样例，改为使用真实场地、项目、车辆和总体概览数据源。
- 车辆轨迹查询已接通真实 `track-history` 接口，并在地图中支持轨迹查询与回放控制。
- 项目与场地主数据尚未维护经纬度字段时，页面使用稳定回退坐标算法展示分布，避免再次退回静态写死数据。
