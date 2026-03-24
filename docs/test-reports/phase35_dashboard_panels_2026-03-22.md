# Phase 35 总体分析与看板统计增强冒烟测试报告

- 测试时间：2026-03-22 16:01 CST
- 测试范围：`84 总体分析`、`88 消纳场数据`、`89 项目数据`
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

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API dashboard overview org fields：总体分析返回单位总数 `5`、活跃单位 `4`
- [PASS] API dashboard org analysis：单位分析返回 `5` 条记录，首条 `浙江广源建设工程有限公司`
- [PASS] API sites yearly report：场地年报返回 `1` 条记录，本期消纳 `5308.0` 方
- [PASS] API projects yearly report：项目年报返回 `1` 条记录，累计完成率 `7%`
- [PASS] UI dashboard org analysis：总体分析页已展示单位维度卡片与单位排行表
- [PASS] UI sites board period stats：场地看板已展示周期统计卡片与真实排行
- [PASS] UI projects board period stats：项目看板已展示周期统计卡片与真实进度排行

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# 校验 /reports/dashboard/overview /reports/dashboard/org-analysis /reports/sites/* /reports/projects/*
PY
python3 -u - <<'PY'
# 通过 localStorage 注入 token 打开 / /dashboard/sites /dashboard/projects
PY
```

## 结论

- 总体分析已补齐单位维度，首页现在展示单位总量、活跃单位和重点单位运营排行。
- 场地看板与项目看板已切换为报表口径，支持日 / 月 / 年统计与自定义时间展示，不再局限于单日排行。
