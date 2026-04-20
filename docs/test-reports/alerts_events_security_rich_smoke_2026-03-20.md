# 预警中心/事件管理/安全台账增强冒烟测试报告 2026-03-20

- 生成时间: 2026-03-20 15:47:24
- 前端地址: `http://127.0.0.1:5173`
- 后端地址: `http://127.0.0.1:8090/api`
- 通过: 13
- 失败: 0

## 结果明细

- [PASS] 环境 / backend可访问 / status=200
- [PASS] 环境 / frontend可访问 / status=200
- [PASS] 认证 / API登录 / user=admin
- [PASS] 预警中心 / 汇总/分析/过滤列表 / total=3, analyticsLevels=2, list=1
- [PASS] 预警中心 / 详情接口 / id=1
- [PASS] 事件管理 / 创建/筛选/编辑/详情 / id=10, list=5
- [PASS] 事件管理 / 提审/审核/关闭闭环 / id=10, auditCount=5
- [PASS] 安全台账 / 创建/筛选/详情 / id=16, list=13
- [PASS] 安全台账 / 整改流转/汇总 / id=16, nextCheck=2026-03-20T23:30:00
- [PASS] 前端页面 / 登录页登录 / url=http://127.0.0.1:5173/
- [PASS] 前端页面 / 路由冒烟 /alerts / 预警与监控中心
- [PASS] 前端页面 / 路由冒烟 /alerts/events / 事件管理
- [PASS] 前端页面 / 路由冒烟 /alerts/security / 安全台账管理
