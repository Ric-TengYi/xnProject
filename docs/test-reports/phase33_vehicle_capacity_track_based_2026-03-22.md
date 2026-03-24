# Phase 33 运力分析真实轨迹口径冒烟测试报告

- 测试时间：2026-03-22 14:34 CST
- 测试范围：`85 运力分析`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：7
- 通过：7
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API track-backed mileage：`浙A12345` 日报返回荷载里程 `1.34 km`、空载里程 `0.63 km`
- [PASS] API keyword trend consistency：按车牌筛选后趋势 `activeCount=1`，与列表车辆数一致
- [PASS] API fleet capacity summary：全量运力分析返回 `8` 台车辆
- [PASS] UI page load：运力分析页面成功打开
- [PASS] UI vehicle table：运力分析表格已加载，当前页展示 `8` 条车辆记录
- [PASS] UI track-backed metrics：页面可见带轨迹车辆及其里程口径展示

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# 真实登录后校验 /api/reports/vehicles/capacity-analysis 的轨迹里程和筛选一致性
PY
python3 -u - <<'PY'
# 通过 localStorage 注入 token 打开 /dashboard/capacity-analysis，验证页面渲染
PY
```

## 结论

- 运力分析已接入 `biz_vehicle_track_point` 真实轨迹数据，优先按轨迹点序列计算车辆里程，并据此生成荷载/空载里程与能耗口径。
- 关键字筛选后，列表、汇总和趋势现在使用同一批车辆数据，不再出现趋势全量、列表过滤后的口径错位。
