# AI 面试助手前端

这是 AI 面试助手的独立 Vue 3 前端工程，提供登录注册、模型选择、题目训练、流式评分、错题复盘与学习进度查看。它与 `../backend` 共用同一个 Git 仓库，但应在 VS Code 中单独打开本目录开发。

修改前必须先阅读 [AGENTS.md](AGENTS.md)。其中定义了产品体验、接口契约、安全边界、测试和交付规则。

## 技术栈

- Vue 3、Vite、TypeScript
- Vue Router、Pinia、Axios
- Element Plus、Lucide、ECharts
- Vitest、Vue Test Utils、Playwright

## 本地运行

前提：Node.js 22 或更高版本，以及 pnpm。

```powershell
Set-Location frontend
pnpm install
pnpm dev
```

开发服务器固定在 `http://127.0.0.1:5173`。默认情况下，Vite 将 `/api` 代理到 `http://localhost:8082`；若后端不在该地址，可在启动前设置：

```powershell
$env:VITE_BACKEND_TARGET = "http://localhost:8082"
pnpm dev
```

不要将 JWT、密码、数据库配置或 AI API Key 写入 `.env`、源码或提交记录。浏览器会话中的 JWT 仅用于当前标签页认证。

## 质量检查

```powershell
pnpm format:check
pnpm lint
pnpm test:run
pnpm build
```

## 后端契约

Spring Boot 后端位于 `../backend`。运行后可查看：

- Swagger UI：`http://localhost:8082/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8082/v3/api-docs`

普通接口统一读取 `ApiResponse.data`。受保护请求使用 `Authorization: Bearer <token>`；流式评分使用带 Bearer 请求头的 `fetch`，绝不把 JWT 放入 URL。

前端依赖与构建产物不进入 Git。请在仓库根目录执行 Git 命令，不要在此目录执行 `git init`。
