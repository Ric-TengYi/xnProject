# Phase 31 车辆地图冒烟测试报告

- 测试时间：2026-03-22 12:44 CST
- 测试范围：`65 车辆地图`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：6
- 通过：6
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API vehicle track history：车辆 `浙A12345` 历史轨迹接口返回 4 个真实轨迹点
- [PASS] API vehicle track fallback：无轨迹车辆返回空轨迹数组，前端可退化为实时定位展示
- [PASS] UI page load：车辆追踪页面正常打开
- [PASS] UI track query controls：页面可见时间范围查询、查询轨迹按钮和轨迹状态标签
- [PASS] UI vehicle list：车辆列表正常加载

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl -e "source xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/036_vehicle_track_history_foundation.sql"
python3 - <<'PY'
# 验证 /api/vehicles/{id}/track-history 的真实轨迹点与空轨迹退化
PY
python3 -u - <<'PY'
# 通过 localStorage 注入 token 打开 /vehicles/tracking，验证轨迹查询控件与车辆列表
PY
```

## 结论

- 车辆追踪页已切换为天地图真实轨迹模式，后端新增历史轨迹接口和轨迹基础表，前端不再使用伪造路径。
- 页面支持按时间范围查询单车轨迹，加载有轨迹车辆时可回放真实轨迹；无轨迹车辆会退化为实时定位展示。
