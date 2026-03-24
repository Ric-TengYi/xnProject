# Phase54 场地资料与场地报表专项烟测

- 日期：2026-03-23
- 覆盖需求：`38 场地资料`、`86 消纳场地报表`
- 结果：`10 / 10 PASS`

## 覆盖内容
- 场地资料全生命周期扩展到 `审批 / 建设 / 运营 / 移交`
- 场地资料汇总支持按 `stageCode`、`approvalType`、关键字过滤
- 场地资料上传格式校验覆盖建设阶段资料类型
- 场地报表支持 `DAY / MONTH / YEAR / CUSTOM`
- 场地报表自定义时间趋势支持短区间按日展开
- 场地报表导出任务支持生成、轮询完成、文件下载

## 测试结果
| 用例 | 结果 | 说明 |
|---|---|---|
| 登录获取 token | PASS | 管理员账号登录成功 |
| 建设阶段资料汇总查询 | PASS | 返回 2 类建设阶段资料 |
| 审批类型过滤 | PASS | `approvalType=CONSTRUCTION` 过滤生效 |
| 场地资料格式校验 | PASS | `CONSTRUCTION_PLAN` 上传 `docx` 被业务拒绝 |
| 建设阶段资料新增 | PASS | `BOUNDARY_SURVEY` 资料新增成功 |
| 自定义时间汇总 | PASS | 返回 `2026-03-01 ~ 2026-03-23` |
| 自定义时间趋势 | PASS | 返回 5 个按日周期 |
| 导出任务完成 | PASS | 生成 `site_reports_20260323105719.csv` |
| 导出文件下载 | PASS | CSV 表头正确 |
| 临时数据清理 | PASS | 烟测创建资料已删除 |

## 验证命令
```bash
mysql -h127.0.0.1 -P3306 -uroot -p'ForkliftDev2025' -D xngl < xngl-service/xngl-service-starter/src/main/resources/db/schema/patches/050_site_documents_lifecycle_seed.sql
cd xngl-service && mvn -q -DskipTests compile
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
cd xngl-service && mvn spring-boot:run -pl xngl-service-starter
python3 phase54 smoke
```
