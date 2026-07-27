# 前端项目

此目录用于 AI 面试助手的独立 Vue 3 前端。

前端工程尚未初始化。开始实现时在此目录创建 Vite 工程，并继续使用仓库根目录现有的 Git 仓库；不要在此执行 `git init`。

修改前端前必须先阅读 [AGENTS.md](AGENTS.md)。其中规定了产品体验、视觉质量、安全、接口契约和验证要求。

后端提供的 OpenAPI 文档是接口契约：

- Swagger UI：`http://localhost:8082/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8082/v3/api-docs`

Spring Boot 后端位于 `../backend`，Maven 命令在该目录执行。前端依赖与构建产物不进入 Git；根目录 `.gitignore` 已忽略 `node_modules/` 和 `dist/`。
