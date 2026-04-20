# Phase 24 合同导入冒烟测试报告

- 测试时间：2026-03-22 11:25 CST
- 测试范围：`5 合同导入`
- 测试环境：
  - 后端：`mvn spring-boot:run -pl xngl-service-starter`
  - 前端：`npm run dev -- --host 127.0.0.1`
  - 浏览器：本机 `Google Chrome` 无头模式
  - 账号：`tenantId=1 / admin / admin`

## 结果概览

- 用例数：7
- 通过：7
- 失败：0

## 用例明细

- [PASS] API login：`admin/admin` 登录成功并获取 token
- [PASS] API import preview：上传 2 行预览数据，返回 `validCount=1`、`errorCount=1`，错误明细共 4 条校验信息
- [PASS] API import commit：提交批次 `1` 成功，返回 `successCount=1`、`failCount=1`
- [PASS] API contract query：导入后按关键字查询，新增合同 `HT-IMPORT-1774149863` 已写入列表且 `sourceType=IMPORT`
- [PASS] UI import file parse：合同清单页打开“批量导入合同”，选择 CSV 后成功解析 1 条数据
- [PASS] UI import preview：点击“预览校验”后出现 `预览完成：总计 1 条，有效 1 条，异常 0 条`
- [PASS] UI import commit：点击“提交导入”后出现成功提示，并可在列表中检索到新增合同 `HT-UI-IMPORT-1774149894`

## 执行命令

```bash
cd xngl-service && mvn -q -DskipTests install
cd xngl-web && npm run build
python3 - <<'PY'
# API 预览、提交、导入后列表回查
PY
python3 -u - <<'PY'
# 前端合同导入弹窗、CSV 选择、预览校验、提交导入
PY
```

## 结论

- 合同导入已形成真实闭环：前端 CSV 解析、预览错误反馈、后端原始行持久化、有效行真实写入 `biz_contract`、导入后列表回查均可用。
- 当前模板支持中英文表头映射，历史合同可按 CSV 批量导入；无效行会在预览阶段展示错误明细，有效行可继续提交入库。
