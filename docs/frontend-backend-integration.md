# 前后端联调方案

## 1. 联调目标

前后端分离后，开发环境建议：

```text
前端：http://localhost:5173
后端：http://localhost:8082
```

前端不直接请求完整后端地址，而是通过 Vite 代理访问：

```text
/api/**
```

这样可以减少跨域问题，并保持开发和生产环境请求路径一致。

## 2. 开发环境启动方式

### 2.1 后端启动

在项目根目录启动：

```bash
cd backend
./mvnw spring-boot:run
```

Windows：

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

默认端口：

```text
8082
```

后端地址：

```text
http://localhost:8082
```

### 2.2 前端启动

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

默认端口：

```text
5173
```

前端地址：

```text
http://localhost:5173
```

## 3. Vite 代理配置

文件：

```text
frontend/vite.config.ts
```

配置：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true
      }
    }
  }
})
```

前端请求：

```ts
http.get('/auth/me')
```

如果 `baseURL` 是 `/api`，实际请求会变成：

```text
http://localhost:5173/api/auth/me
```

再由 Vite 转发到：

```text
http://localhost:8082/api/auth/me
```

## 4. 接口返回约定

所有后端接口建议返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败示例：

```json
{
  "code": 401,
  "message": "登录已过期",
  "data": null
}
```

前端判断规则：

- HTTP 状态码 200，但 `code != 0`：业务失败。
- HTTP 状态码 401：未登录或登录过期。
- HTTP 状态码 500：系统异常。

## 5. 登录联调流程

### 5.1 注册

前端：

```text
POST /api/auth/register
```

请求：

```json
{
  "username": "test",
  "password": "123456",
  "nickname": "测试用户"
}
```

后端：

- 校验用户名是否存在。
- 加密密码。
- 保存用户。
- 返回成功。

### 5.2 登录

前端：

```text
POST /api/auth/login
```

请求：

```json
{
  "username": "test",
  "password": "123456"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx",
    "user": {
      "id": 1,
      "username": "test",
      "nickname": "测试用户"
    }
  }
}
```

前端处理：

- 保存 token 到 localStorage。
- 保存用户信息到 Pinia。
- 跳转 `/dashboard`。

### 5.3 后续请求鉴权

前端请求头：

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx
```

后端处理：

- `JwtAuthenticationFilter` 解析 token。
- 校验 token 是否有效。
- 获取用户 ID。
- 写入 `UserContext` 或 `SecurityContext`。
- Controller / Service 获取当前用户。

## 6. AI 面试联调流程

### 6.1 创建面试会话

接口：

```text
POST /api/interviews
```

请求：

```json
{
  "category": "JAVA_BACKEND",
  "difficulty": "MEDIUM",
  "questionCount": 5
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sessionId": 1001,
    "title": "Java 后端中级模拟面试",
    "status": "IN_PROGRESS"
  }
}
```

### 6.2 生成题目

接口：

```text
POST /api/interviews/1001/questions
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "questionId": 2001,
    "title": "请解释 Redis 的 RDB 和 AOF 持久化机制有什么区别？",
    "category": "Redis",
    "difficulty": "MEDIUM",
    "tags": ["Redis", "持久化", "RDB", "AOF"]
  }
}
```

### 6.3 提交答案

接口：

```text
POST /api/interviews/1001/answers
```

请求：

```json
{
  "questionId": 2001,
  "answer": "RDB 是快照，AOF 是追加日志..."
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answerId": 3001,
    "scoreStreamUrl": "/api/interviews/1001/score/stream?answerId=3001"
  }
}
```

### 6.4 流式评分

前端建立 SSE：

```ts
const source = new EventSource('/api/interviews/1001/score/stream?answerId=3001')

source.onmessage = (event) => {
  console.log(event.data)
}

source.addEventListener('done', () => {
  source.close()
})
```

后端推送内容：

```text
你的回答提到了 RDB 和 AOF 的基本区别。
但对 AOF 重写机制、恢复速度和数据安全性的比较还不够完整。
综合评分：82 分。
```

最后可以推送结构化结果：

```json
{
  "totalScore": 82,
  "accuracyScore": 80,
  "completenessScore": 75,
  "expressionScore": 88,
  "knowledgeScore": 85,
  "weaknesses": ["缺少 AOF 重写机制说明"],
  "suggestions": ["补充 RDB 和 AOF 在恢复速度、数据安全、文件大小方面的区别"],
  "knowledgeTags": ["Redis", "RDB", "AOF"]
}
```

## 7. 错题本联调流程

自动加入错题本规则：

```text
totalScore < 70
```

或者：

```text
用户点击“加入错题本”
```

查询错题：

```text
GET /api/mistakes?tag=Redis&status=TODO
```

更新状态：

```text
PATCH /api/mistakes/1/status
```

请求：

```json
{
  "status": "MASTERED"
}
```

## 8. 学习报告联调流程

首页概览：

```text
GET /api/reports/overview
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "interviewCount": 12,
    "averageScore": 78.5,
    "mistakeCount": 18,
    "masteredMistakeCount": 6,
    "weakTags": ["Redis", "JVM", "MySQL 索引"]
  }
}
```

进度报告：

```text
GET /api/reports/progress
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tagScores": [
      {
        "tag": "Redis",
        "averageScore": 68,
        "practiceCount": 5
      },
      {
        "tag": "Spring Boot",
        "averageScore": 82,
        "practiceCount": 8
      }
    ]
  }
}
```

## 9. 跨域处理

开发阶段优先使用 Vite 代理，不需要后端额外配置 CORS。

如果必须直接从前端访问后端：

```text
http://localhost:5173 -> http://localhost:8082
```

后端需要配置 CORS。

但推荐方式仍然是：

```text
前端请求 /api
Vite 代理到后端
```

## 10. SSE 鉴权问题

原生 `EventSource` 不能方便地设置 `Authorization` 请求头。

可选方案：

### 方案一：JWT 放 Cookie

登录后后端设置 HttpOnly Cookie。

优点：

- SSE 自动携带 Cookie。
- 不需要在 URL 中暴露 token。

缺点：

- 后端认证逻辑需要支持 Cookie。
- 需要注意 CSRF。

### 方案二：使用短期 streamToken

流程：

1. 前端正常用 JWT 请求创建评分任务。
2. 后端返回一次性 `streamToken`。
3. 前端用 `EventSource` 连接：

```text
/api/interviews/1001/score/stream?streamToken=xxx
```

优点：

- 不暴露长期 JWT。
- 实现相对简单。

缺点：

- 需要后端维护短期 token。

### 方案三：使用 fetch 读取流

用 `fetch` 可以带请求头，但前端处理流会比 EventSource 复杂。

建议：

> 项目初期用方案二，后续如果要做得更规范，再切到 Cookie 或 fetch streaming。

## 11. 联调检查清单

后端检查：

- 后端是否启动在 `8082`。
- 数据库是否启动。
- Redis 是否启动。
- `application.properties` 配置是否正确。
- 登录和注册接口是否放行。
- 需要登录的接口是否能正确解析 JWT。

前端检查：

- 前端是否启动在 `5173`。
- Vite 代理是否配置 `/api`。
- Axios 是否自动加 token。
- 401 是否自动跳登录。
- SSE 连接是否能正常关闭。

接口检查：

- 请求路径是否统一以 `/api` 开头。
- 请求体字段是否和 DTO 一致。
- 返回结构是否统一。
- 错误信息是否能被前端展示。

## 12. 推荐联调顺序

1. 注册接口。
2. 登录接口。
3. 获取当前用户接口。
4. 创建面试会话。
5. 生成题目。
6. 提交答案。
7. 普通评分接口。
8. SSE 流式评分接口。
9. 错题本接口。
10. 学习报告接口。

不要一开始就联调 SSE。先把普通 JSON 接口打通，再升级为流式响应，会稳很多。
