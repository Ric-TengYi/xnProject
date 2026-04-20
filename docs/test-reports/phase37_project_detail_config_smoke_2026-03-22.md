# Phase 37 项目详情合同清单/场地清单/配置冒烟测试报告

- 测试时间：2026-03-22 16:30:06 CST
- 测试范围：`22 项目合同清单`、`23 场地清单`、`26/27/28/29/32 项目配置`
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

- [PASS] API login：admin/admin 登录成功并获取 token
- [PASS] API project detail aggregates：项目详情返回 15 条合同、1 个场地和真实配置
- [PASS] API project contract summary：合同 HT-MSG-1774153918 返回方量汇总和剩余方量
- [PASS] API project site summary：场地 场地-001 返回合同方量 77241.5 和已消纳 5308.0
- [PASS] API project config summary：打卡配置、位置判断、预扣值、线路和违规围栏均已返回真实字段
- [PASS] UI project contract/site tab：项目详情已展示真实合同清单和项目场地清单
- [PASS] UI project config tab：项目配置页已展示打卡账号、位置判断、预扣值和违规围栏
- [PASS] UI project route/fence preview：项目配置页已展示线路状态和违规围栏状态

## 产出文件

- API 结果：`docs/test-reports/phase37_project_detail_config_smoke_2026-03-22.json`
- UI 结果：`docs/test-reports/phase37_project_detail_config_ui_2026-03-22.json`

## 结论

- 项目详情接口已聚合合同方量、场地方量和项目配置，不再依赖静态合同/场地/配置占位数据。
- 前端 `ProjectDetail` 已展示真实项目合同清单、项目场地清单，以及打卡/位置判断/预扣值/线路/违规围栏配置。
