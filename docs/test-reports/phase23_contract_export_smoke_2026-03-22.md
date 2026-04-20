# Phase 23 Contract Export Smoke (2026-03-22)

- Total: 7
- Passed: 7
- Failed: 0

## Checks
- [PASS] API login / `admin/admin` 登录成功并获取 JWT
- [PASS] Contract list query / 合同列表查询成功，取到可用于筛选导出的合同样本
- [PASS] Export task create / `POST /api/contracts/export` 成功创建导出任务
- [PASS] Export task complete / `GET /api/export-tasks/{id}` 返回 `COMPLETED`
- [PASS] Export download / `GET /api/export-tasks/{id}/download` 成功返回 CSV 文件流
- [PASS] Filtered CSV content / 导出文件包含表头与按 `contractNo` 关键词筛出的合同数据
- [PASS] Frontend entry + build / `ContractsManagement` 已接入真实导出按钮，前后端构建通过，`http://127.0.0.1:5173/contracts` 可正常返回页面

## Notes
- 后端补齐了导出任务状态流转、CSV 文件生成与下载流接口，导出文件落在本机临时目录。
- `ExportTaskController` 现对外返回统一下载地址，不再暴露内部临时文件路径。
- 页面自动化下载原计划用 Playwright 执行，但当前环境缺少内置浏览器包且系统 Chrome 的 headless 启动卡住，因此本批以前后端构建、dev 路由可达和接口级下载冒烟为准。
