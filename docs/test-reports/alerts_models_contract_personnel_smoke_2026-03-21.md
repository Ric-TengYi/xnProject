# 预警模型/合同人员预警冒烟测试报告 2026-03-21

- 生成时间: 2026-03-21 11:55:54
- 前端地址: `http://127.0.0.1:5173`
- 后端地址: `http://127.0.0.1:8090/api`
- 通过: 13
- 失败: 0

## 结果明细

- [PASS] 环境 / backend端口可访问 / tcp=8090
- [PASS] 前端 / / / status=200
- [PASS] 前端 / /alerts / status=200
- [PASS] 前端 / /alerts/config / status=200
- [PASS] 认证 / API登录 / user=admin
- [PASS] 预警汇总 / 合同与人员计数 / contract=1, user=1, total=5
- [PASS] 模型覆盖 / 合同与人员场景 / coverage={'CONTRACT': 2, 'USER': 2, 'PROJECT': 1, 'SITE': 1, 'VEHICLE': 5}
- [PASS] 高风险排行 / 合同目标 / top=HT-001
- [PASS] 高风险排行 / 人员目标 / top=Demo Admin
- [PASS] 预警列表 / 合同筛选 / count=1
- [PASS] 预警列表 / 人员筛选 / count=1
- [PASS] 合同预警 / 处置确认 / id=4, status=CONFIRMED
- [PASS] 人员预警 / 关闭原因 / id=5, status=CLOSED
