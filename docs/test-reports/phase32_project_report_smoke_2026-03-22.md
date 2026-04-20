# Phase 32 项目报表冒烟测试报告

- 测试时间：2026-03-22 14:26 CST
- 测试范围：`87 项目报表`
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
- [PASS] API project report summary：项目汇总返回 `1` 个项目，累计完成率 `2%`
- [PASS] API project report list：项目报表列表返回 `1` 条记录，首条项目 `项目-001` 本期消纳量 `1200.0` 方
- [PASS] API project report trend：项目趋势返回 `6` 个周期，最近周期 `2026-03`
- [PASS] API project report export：项目报表导出任务创建成功，`taskId=12`
- [PASS] UI page load：项目报表页面成功打开
- [PASS] UI report controls：页面可见导出、刷新和关键字查询控件
- [PASS] UI report table：项目报表表格已加载并展示真实项目记录
- [PASS] UI summary cards：统计卡片与趋势区域已渲染

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# 真实登录后校验 /api/reports/projects/{summary,list,trend,export}
PY
python3 -u - <<'PY'
# 通过 localStorage 注入 token 打开 /projects/reports，验证页面联调
PY
```

## 结论

- 已补齐项目报表独立页面，支持项目维度日 / 月 / 年统计、趋势查看、关键字筛选、按项目筛选和导出任务创建。
- 后端已补齐 `/api/reports/projects/summary`、`/api/reports/projects/list`、`/api/reports/projects/trend`、`/api/reports/projects/export`，并与真实合同/票据数据源联动。
