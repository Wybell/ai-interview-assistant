# AI Interview Assistant 项目升级文档

这个目录用于规划当前项目从玩具级 Demo 升级为秋招简历项目的实施路线。

当前项目基础：

- 后端：Spring Boot 2.7.18、Java 17、Maven
- 数据库：MySQL
- ORM：MyBatis-Plus
- 缓存：Redis
- 安全：Spring Security、JWT
- AI：DashScope SDK
- 前端：当前为 `src/main/resources/static` 下的静态 HTML 页面

建议目标：

> 将项目升级为一个基于 Spring Boot + Vue 3 + MySQL + Redis + 大模型 API 的智能面试训练平台，支持用户登录、AI 模拟面试、流式评分、错题沉淀、学习进度分析和个性化复习建议。

文档列表：

- [项目升级总规划](./project-upgrade-plan.md)
- [前端架构设计与实现细节](./frontend-architecture.md)
- [后端架构设计与实现细节](./backend-architecture.md)
- [前后端联调方案](./frontend-backend-integration.md)

推荐实施顺序：

1. 后端先完成分层重构：Controller、Service、DTO、统一响应、统一异常。
2. 新建独立前端项目，迁移现有 HTML 页面到 Vue 3 组件。
3. 打通登录注册、JWT 鉴权、前端路由守卫、Axios 拦截器。
4. 重做 AI 面试主流程：开始面试、提交答案、流式评分、保存记录。
5. 增加错题本和学习报告，形成数据闭环。
6. 引入 Redis 缓存、限流、AI 调用保护。
7. 补充 README、接口文档、部署文档和测试用例。
