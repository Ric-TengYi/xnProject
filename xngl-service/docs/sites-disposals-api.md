# 场地与消纳清单 API 契约（联调用）

## 1. 说明

- **场地**：已实现。`biz_site` 表 + `SiteService` + `SitesController`，列表与详情为真实查询。
- **消纳清单**：当前为 **stub**。无 `biz_disposal`（或等价）表，`GET /api/sites/disposals` 固定返回空分页，便于前端先接好接口与分页结构，待建表后接入真实数据。

## 2. 场地接口

### 2.1 `GET /api/sites`

响应：`ApiResult<List<SiteListItemDto>>`

| 字段   | 类型    | 说明     |
|--------|---------|----------|
| id     | string  | 场地 ID  |
| name   | string  | 场地名称 |
| code   | string  | 编码     |
| address| string  | 地址     |
| status | integer | 状态     |

### 2.2 `GET /api/sites/{id}`

响应：`ApiResult<SiteDetailDto>`

在列表字段基础上增加：`projectId`、`orgId`、`createTime`、`updateTime`。不存在时 `code=404`。

## 3. 消纳清单接口（stub）

### 3.1 `GET /api/sites/disposals`

查询参数：

| 参数     | 类型   | 说明           |
|----------|--------|----------------|
| siteId   | long   | 可选，场地 ID  |
| keyword  | string| 可选，车牌/项目 |
| status   | string| 可选，状态     |
| pageNo   | int   | 默认 1         |
| pageSize | int   | 默认 20        |

响应：`ApiResult<PageResult<DisposalListItemDto>>`

单条 `DisposalListItemDto` 与前端 `SitesDisposals` 表格对齐：

| 字段   | 类型   | 说明         |
|--------|--------|--------------|
| id     | string | 记录编号     |
| siteId | string | 场地 ID      |
| site   | string | 场地名称     |
| time   | string | 消纳时间     |
| plate  | string | 车牌号       |
| project| string | 关联项目     |
| source | string | 来源         |
| volume | int    | 消纳量(方)   |
| status | string | 状态(正常/异常) |

**当前实现**：始终返回 `total=0`、`records=[]`。真实数据依赖后续新增消纳记录表并实现 `DisposalService`。

## 4. 阻塞点与后续

- **缺失**：消纳记录表（如 `biz_disposal`）及对应实体、Mapper、Service。建表后需在 Controller 中改为调用 DisposalService 分页查询。
- **联调**：前端可将消纳清单页从 mock 改为请求 `GET /api/sites/disposals`，先验证分页与空态；场地下拉可请求 `GET /api/sites` 使用真实数据。
