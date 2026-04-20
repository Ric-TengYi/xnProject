# xngl-mp - 消纳移动端

**一套代码** 编译为：

- **微信小程序**
- **钉钉小程序**
- **H5**（浏览器 / 企微 / 钉钉工作台 H5 应用）

技术栈：Taro 3 + React + TypeScript，与 PC 端 `xngl-web` 同栈，后端对接 `技术方案_小程序.md` 中的 `/api/mini/*` 接口。

## 脚本

| 命令 | 说明 |
|------|------|
| `npm run dev:h5` | H5 开发（默认 http://localhost:10086） |
| `npm run build:h5` | H5 构建 |
| `npm run dev:weapp` | 微信小程序开发（产出在 dist，用微信开发者工具打开） |
| `npm run build:weapp` | 微信小程序构建 |
| `npm run dev:dd` | 钉钉小程序开发（产出在 dist，用钉钉开发者工具打开） |
| `npm run build:dd` | 钉钉小程序构建 |

## 配置

- 微信：`project.config.json` 中 `appid` 改为实际小程序 AppID。
- 钉钉：`project.dd.json` 中 `appid` 改为钉钉小程序 AppID。
- 接口 baseURL：在 `src/utils/request.ts`（待建）或构建环境变量中配置，与 xngl-service 同域或网关。

## 架构说明

见仓库根目录：`docs/tech-plans/技术方案_移动端架构选型.md`。
