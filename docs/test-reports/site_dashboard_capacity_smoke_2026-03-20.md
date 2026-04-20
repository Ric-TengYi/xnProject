# 场地报表 / 总体分析 / 运力分析烟测报告

- 日期: 2026-03-20
- 后端验证实例: `http://127.0.0.1:8091`
- 账号: `tenantId=1 / admin / admin`

## 验证结果

| 项目 | 接口 | 结果 |
|---|---|---|
| 场地报表摘要 | `GET /api/reports/sites/summary` | 通过 |
| 场地报表列表 | `GET /api/reports/sites/list` | 通过 |
| 场地报表趋势 | `GET /api/reports/sites/trend` | 通过 |
| 总体分析概览 | `GET /api/reports/dashboard/overview` | 通过 |
| 总体分析趋势 | `GET /api/reports/dashboard/trend` | 通过 |
| 项目预警列表 | `GET /api/reports/dashboard/project-alerts` | 通过 |
| 运力分析 | `GET /api/reports/vehicles/capacity-analysis` | 通过 |
| 前端构建 | `cd xngl-web && npm run build` | 通过 |
| 后端构建 | `cd xngl-service && mvn -q -DskipTests install` | 通过 |

## 说明

- 当前本机 `8090` 端口仍有旧后端实例占用，因此本轮新增接口使用 `8091` 实例完成 API 烟测。
- 详细响应摘要见 `docs/test-reports/site_dashboard_capacity_smoke_2026-03-20.json`。
