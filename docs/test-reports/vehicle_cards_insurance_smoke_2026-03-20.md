# 车辆油电卡与保险联调测试报告 (2026-03-20)

- 生成时间: 2026-03-20 14:04:41
- 后端地址: `http://127.0.0.1:8090/api`
- 前端地址: `http://127.0.0.1:5173`
- 结果: 16 通过 / 0 失败
- 说明: Playwright 浏览器缺失时自动退化为 HTTP 页面可达性检查

## 用例结果

- [PASS] api / API login - admin/admin 登录成功
- [PASS] api / vehicle-cards summary - {"totalCards": 4, "fuelCards": 2, "electricCards": 2, "boundCards": 3, "lowBalanceCards": 1, "totalBalance": 8750.5, "fuelBalance": 5200.5, "electricBalance": 3550.0}
- [PASS] api / vehicle-cards list - total=4
- [PASS] api / vehicle-insurances summary - {"totalPolicies": 3, "activePolicies": 1, "expiringPolicies": 1, "expiredPolicies": 1, "totalCoverageAmount": 6720000.0, "totalPremiumAmount": 31000.0, "totalClaimAmount": 23200.0}
- [PASS] api / vehicle-insurances list - total=3
- [PASS] api / vehicles list for bind - total=8
- [PASS] api / vehicle-cards bind - card=CARD-FUEL-0003 -> vehicle=浙A97610
- [PASS] api / vehicle-cards unbind restore - card=CARD-FUEL-0003 已恢复未绑定
- [PASS] api / vehicle-cards recharge - card=CARD-ELEC-0002 amount=50
- [PASS] api / vehicle-cards recharge restore - card=CARD-ELEC-0002 已恢复原始余额
- [PASS] api / vehicle-insurances update - policy=POL-2025-0003 remark updated
- [PASS] api / vehicle-insurances restore - policy=POL-2025-0003 已恢复原始备注
- [PASS] ui / ui browser fallback - Playwright 浏览器不可用，改用 HTTP 路由检查: BrowserType.launch: Executable doesn't exist at /Users/tengyi/Library/Caches/ms-playwright/chromium_headless_shell-1208/chrome-headless-shell-mac-x64/chrome-headless-shell
╔════════════════════════════════════════════════════════════╗
║ Looks like Playwright was just installed or updated.       ║
║ Please run the following command to download new browsers: ║
║                                                            ║
║     playwright install                                     ║
║                                                            ║
║ <3 Playwright Team                                         ║
╚════════════════════════════════════════════════════════════╝
- [PASS] ui / route /login - 前端路由可访问并返回应用壳 HTML
- [PASS] ui / route /vehicles/cards - 前端路由可访问并返回应用壳 HTML
- [PASS] ui / route /vehicles/insurances - 前端路由可访问并返回应用壳 HTML
