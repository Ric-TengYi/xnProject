# xngl-service

消纳管理平台后端服务。

## 技术栈

- Java 17
- Spring Boot 3.3.x
- MyBatis-Plus、MySQL、Redis
- JWT 鉴权
- SpringDoc OpenAPI

## 模块

- `xngl-service-starter`：启动入口与配置
- `xngl-service-web`：Controller、DTO、异常处理、OpenAPI
- `xngl-service-manager`：业务编排与领域服务
- `xngl-service-infrastructure`：Mapper、Entity、Redis、文件等

## 启动时库表校对

当 `app.schema-sync.enabled=true` 时，每次启动会执行 `db/schema/baseline.sql`（缺失表则创建），并扫描 `db/schema/patches/*.sql` 对缺失字段补充；对配置中的**大表**仅打日志提示，不执行 ALTER。**稳定上线后建议关闭**，避免启动变慢：设置 `app.schema-sync.enabled=false` 或使用 profile `prod`（`--spring.profiles.active=prod`）。

## 本地运行

1. 安装 MySQL 与 Redis，创建库 `xngl`，在 `application.yml` 或 `application-local.yml` 中配置 `spring.datasource` 与 `spring.data.redis`。
2. 启动：

```bash
cd xngl-service-starter
mvn spring-boot:run
```

或从根目录：

```bash
mvn spring-boot:run -pl xngl-service-starter
```

3. 服务端口：`8080`。  
   - 健康检查：`GET http://localhost:8080/api/health`  
   - OpenAPI 文档：`http://localhost:8080/swagger-ui.html`  
   - 登录：`POST /api/auth/login`，获取 token 后请求头带 `Authorization: Bearer <token>` 访问需鉴权接口（如 `GET /api/me`）。

## 与前端联调

前端项目 `xngl-web` 已配置 Vite 代理：请求 `/api` 会转发到 `http://localhost:8080`。  
本地同时启动 `xngl-service`（端口 8080）和 `xngl-web`（npm run dev）即可联调。

## 构建

```bash
mvn clean install -DskipTests
```

可执行 jar：`xngl-service-starter/target/xngl-service-starter-1.0.0-SNAPSHOT.jar`。

## 设计文档

用户体系拆分产物位于 `docs/user-system/`：

- `00-shared-model.md`：共享命名、权限码、接口路径规范
- `01-er-ddl-design.md`：ER/DDL 设计文档
- `02-api-dto-contract.md`：API/DTO 契约文档
