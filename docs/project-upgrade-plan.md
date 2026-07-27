# AI Interview Assistant 项目升级总规划

## 1. 项目定位

当前项目已经具备登录、AI 出题、答案评分、错题和进度统计的雏形。升级后的目标不是一个简单的 AI 问答 Demo，而是：

> AI 面试训练平台：支持用户登录、知识点选择、AI 出题、作答评分、错题沉淀、学习进度分析和个性化复习建议。

核心业务闭环：

```text
用户登录
  -> 选择知识点
  -> AI 生成面试题
  -> 用户作答
  -> AI 评分和建议
  -> 保存练习记录
  -> 形成错题本和学习进度
  -> 反向生成复习建议
```

## 2. 整体架构

```text
Vue 3 前端
  |
  | HTTP / SSE
  v
Spring Boot 后端
  |
  | MyBatis-Plus
  v
MySQL

Spring Boot 后端
  |
  | Redis
  v
缓存 / 限流 / 登录辅助

Spring Boot 后端
  |
  | DashScope API
  v
大模型服务
```

推荐技术栈：

```text
前端：Vue 3、Vite、TypeScript、Vue Router、Pinia、Axios、Element Plus、ECharts
后端：Spring Boot 2.7.x、Java 17、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、DashScope API
工程化：Maven、环境变量配置、接口文档、单元测试、Docker Compose
```

## 3. 前端目标形态

前端建议独立为 `frontend/` 项目，不再继续把正式页面放在 `src/main/resources/static`。

主要页面：

```text
LoginView.vue              登录
RegisterView.vue           注册
DashboardView.vue          首页仪表盘
InterviewPracticeView.vue  AI 面试训练
MistakeBookView.vue        错题本
ProgressView.vue           学习进度
HistoryView.vue            历史记录
ProfileView.vue            个人信息
```

核心体验：

- 登录后进入仪表盘，展示练习次数、平均分、错题数量、薄弱知识点。
- 训练页支持选择知识点、生成题目、输入答案、提交评分。
- 评分结果展示分数、参考答案、改进建议和薄弱点。
- 错题本支持按知识点筛选、重新练习、标记掌握。
- 进度页用图表展示各知识点练习次数和平均分趋势。

详细前端设计见：[frontend-architecture.md](./frontend-architecture.md)。

## 4. 后端目标形态

后端保持 Spring Boot 单体应用，但要拆清楚分层。

```text
controller  接收请求，返回响应
service     业务逻辑
client      外部大模型调用
mapper      数据库访问
entity      数据库实体
dto         请求和响应对象
security    JWT 鉴权
config      配置类
exception   统一异常处理
common      通用响应模型
```

后端核心模块：

```text
用户模块
  注册、登录、当前用户、JWT 生成和校验

面试训练模块
  生成题目、提交答案、AI 评分、流式评分

记录模块
  保存练习记录、查询历史详情

错题模块
  低分自动加入错题、错题筛选、重新练习

学习进度模块
  练习次数、平均分、薄弱知识点、趋势统计

系统能力
  统一响应、统一异常、参数校验、限流、配置外置
```

详细后端设计见：[backend-architecture.md](./backend-architecture.md)。

## 5. 核心接口规划

认证接口：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/user/me
```

面试训练接口：

```text
POST /api/interview/questions
POST /api/interview/answers/score
GET  /api/interview/answers/score/stream
```

历史记录接口：

```text
GET    /api/conversations
GET    /api/conversations/{id}
DELETE /api/conversations/{id}
```

错题本接口：

```text
GET  /api/conversations/mistakes
POST /api/conversations/{id}/retry
```

学习进度接口：

```text
GET /api/progress/summary
GET /api/progress/tags
GET /api/progress/trend
```

统一响应格式：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

## 6. 数据库规划

第一阶段保留现有 `user` 和 `conversation` 表即可，先把业务跑稳。

建议基础表：

```text
user
  id
  username
  password
  nickname
  create_time
  update_time

conversation
  id
  user_id
  tag
  question
  user_answer
  score
  correct_answer
  suggestion
  ai_raw_response
  create_time

question_tag
  id
  name
  category
  sort_order
  enabled
```

后期可扩展：

```text
user_practice_stat
  id
  user_id
  tag
  total_count
  average_score
  last_practice_time
```

前期不一定要建 `user_practice_stat`，可以直接从 `conversation` 聚合查询。

## 7. Redis 规划

Redis 先只做真正有价值的场景：

```text
缓存 AI 生成题目
key: question:{userId}:{tag}

AI 接口限流
key: rate:ai:{userId}:{minute}

JWT 黑名单或登录辅助
key: jwt:blacklist:{tokenId}
```

不要为了使用 Redis 而强行加复杂设计。

## 8. 大模型调用设计

大模型调用必须从 Controller 拆出去，单独封装：

```text
DashScopeClient
  generateQuestion(tag)
  scoreAnswer(question, answer)
  streamScore(question, answer)
```

Service 层只关心业务结果，不关心 DashScope 的 URL、API Key、请求 JSON 结构。

推荐评分结果结构：

```json
{
  "score": 8,
  "correctAnswer": "标准答案要点",
  "suggestion": "改进建议",
  "weakPoints": ["并发安全", "扩容机制"]
}
```

## 9. 开发阶段顺序

### 第一阶段：后端地基

优先做：

1. 密钥外置，移除代码和配置里的明文密钥。
2. 新增统一响应 `ApiResponse`。
3. 新增统一异常处理 `GlobalExceptionHandler`。
4. 拆出 `DashScopeClient`。
5. 拆出 `InterviewService` 和 `ConversationService`。
6. 把核心接口从 GET query 改成 POST JSON。

### 第二阶段：安全体系

1. 新增 JWT Filter。
2. Spring Security 只放行登录、注册和静态资源。
3. 其他 `/api/**` 必须登录。
4. 登录注册增加参数校验。
5. 当前用户从安全上下文中获取。

### 第三阶段：核心业务闭环

1. 生成题目。
2. 提交答案。
3. AI 评分。
4. 保存练习记录。
5. 查询错题本。
6. 查询学习进度。

### 第四阶段：前端独立化

1. 新建 Vue 3 + Vite + TypeScript 项目。
2. 实现登录注册。
3. 实现面试训练页。
4. 实现错题本。
5. 实现学习进度图表。

### 第五阶段：稳定性和展示

1. Redis 限流。
2. 真实 SSE 流式输出。
3. 单元测试和接口测试。
4. Docker Compose。
5. README 和部署文档。

## 10. 简历表达参考

项目名称：

```text
AI 面试训练系统
```

技术栈：

```text
Spring Boot、Vue 3、MySQL、Redis、MyBatis-Plus、Spring Security、JWT、SSE、DashScope API
```

项目描述：

- 设计并实现用户认证、AI 模拟面试、答案评分、错题沉淀、学习进度分析等模块，形成完整面试训练闭环。
- 基于 Spring Security + JWT 实现登录认证和接口鉴权，通过 Redis 支持热点题目缓存和 AI 接口限流。
- 封装 DashScope 大模型调用客户端，设计结构化 Prompt，将 AI 评分结果转换为可持久化业务数据。
- 使用 `SseEmitter` + `EventSource` 实现 AI 评分内容流式返回，降低长耗时接口的等待感。
- 采用统一响应模型、全局异常处理、DTO 分层和参数校验，提升接口稳定性和前后端联调效率。
