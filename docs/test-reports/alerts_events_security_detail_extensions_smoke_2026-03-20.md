# 事件管理/安全台账扩展字段冒烟测试报告 2026-03-20

- 生成时间: 2026-03-20 18:49:22
- 前端地址: `http://127.0.0.1:5173`
- 后端地址: `http://127.0.0.1:8090/api`
- 通过: 9
- 失败: 0

## 结果明细

- [PASS] 环境 / backend端口可访问 / tcp=8090
- [PASS] 前端 / / / status=200
- [PASS] 前端 / /alerts/events / status=200
- [PASS] 前端 / /alerts/security / status=200
- [PASS] 认证 / API登录 / user=admin
- [PASS] 事件管理 / 汇总接口 / total=12, pending=1, buckets=3
- [PASS] 事件管理 / 扩展字段 + 流转闭环 / id=13, status=CLOSED, audits=5
- [PASS] 安全台账 / 汇总增强接口 / month=18, dangerBuckets=3
- [PASS] 安全台账 / 扩展字段 + 整改流转 / id=19, status=RECTIFYING, danger=HIGH
