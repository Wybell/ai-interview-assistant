# AI Interview Assistant 协作指南

## 企业级开发总原则

从现在起，这个项目整体按真实企业级软件开发标准设计和演进，而不是只让当前 Demo 能运行。该原则覆盖需求、架构、数据库、后端、前端、测试、配置、文档、发布和运维。

企业级标准不等于在当前阶段一次性引入所有中间件或无限拆分代码。项目仍按真实业务需求小步迭代，但每一步必须具备明确职责、可追踪变更、可验证结果，并为后续扩展和生产发布保留合理边界。

所有后续工作遵守以下规则：

- 需求先于实现：先明确用户流程、业务规则、验收标准和数据归属，再设计代码或表结构。
- 架构按职责分层：Controller、业务 Service、数据访问、外部客户端、传输适配器和安全组件不得混合职责。
- 数据库结构受版本管理：生产结构变更使用项目内 SQL 迁移文件，禁止只在 Navicat 手工修改后不提交 SQL。
- 接口契约优先：公开 HTTP 和 SSE 接口必须有明确请求、响应、错误和认证约定；后续以 OpenAPI 作为正式契约。
- 配置与密钥外置：密码、Token、API Key、数据库地址等不进入 Git，不出现在日志、文档示例或前端代码中。
- 测试随风险增加：业务规则有单元测试，HTTP 与安全链有 Web 测试，核心持久化流程逐步补充数据库集成测试。
- 变更可回溯：代码、数据库、配置和部署变更必须有项目记录、版本控制和验证结果；已发布的数据库迁移不得随意修改。
- 发布可控：本地验证后进入测试或云端环境；数据库迁移与匹配的后端版本作为一个发布单元，并具备备份、健康检查和回滚或恢复方案。
- 可观测性与安全默认开启：错误处理统一，日志不泄露敏感数据，认证、授权、输入校验和外部调用失败处理必须覆盖关键路径。
- 避免形式主义：不为了“企业级”而增加空接口、空表、未使用中间件或过度抽象；每一项技术选择必须解决当前或已确认的近期问题。

当“快速完成 Demo”和“长期可维护、可部署、可协作”发生冲突时，默认优先后者；如果成本明显增加，先向用户说明收益、代价和替代方案，再按用户确认推进。

## 项目协作方式

这个项目的目标是从当前玩具级 Demo，逐步完善成一个成熟的 AI 面试训练平台。

协作时默认采用“带做模式”：

- 不直接替用户修改业务代码，除非用户明确说“帮我改”“直接改”“生成文件”。
- 默认从教学和引导角度出发，告诉用户应该改哪个文件、为什么改、怎么改、改完如何验证。
- 每次只推进一个明确的小步骤，不要一次性给出五六步；进入下一步前，先等用户完成当前步骤或确认继续。
- 每一步都必须解释“为什么要这样做”，再给出具体操作。
- 每一步都尽量小而清晰，让用户可以在 VS Code 中手动完成。
- 给代码时优先提供可复制的小片段，并说明应放到哪个类、哪个包、哪个位置。
- 每完成一个小阶段，都给出验证命令、预期结果和常见错误排查。
- 每次完成代码、配置或结构调整后，必须同步更新本文件的“当前实际进度”和“当前最高优先级”，记录已完成内容、未完成内容、下一步与验证状态。
- 遇到设计选择时，先解释推荐方案，再说明取舍，不直接堆技术。
- 不急着一次性接入所有中间件，先把基础结构、安全、分层和工程化打牢。

## 当前项目定位

当前项目的后端仍是 Spring Boot 单体应用，仓库已为独立前端预留边界：

- 后端：Spring Boot 2.7.18、Java 17、Maven
- 数据库：MySQL
- ORM：MyBatis-Plus
- 缓存：Redis
- 安全：Spring Security、JWT
- AI：DashScope
- 前端：独立 Vue 3 工程将位于仓库根目录的 `frontend/`；`backend/src/main/resources/static` 下的静态 HTML 仅保留为历史兼容页面

当前状态更接近“能跑通功能的 Demo”，还不是成熟项目。

## 当前仓库目录结构

当前项目使用单 Git 仓库管理前后端，目录边界如下：

```text
repository-root/
  .git/          # 唯一 Git 仓库根目录
  backend/       # Spring Boot、Maven Wrapper、数据库迁移与后端测试
  frontend/      # 独立 Vue 3 前端工程，在 VS Code 中打开
    AGENTS.md    # 前端专属产品、体验、接口与验证规则
  docs/          # 前后端共享架构与集成文档
  AGENTS.md
  README.md
```

- IDEA 只打开 `backend/`；VS Code 只打开 `frontend/`。
- 所有 Git 命令在仓库根目录执行；不要在 `backend/` 或 `frontend/` 再执行 `git init`。
- 所有 Maven 命令在 `backend/` 执行，例如 `Set-Location backend; .\mvnw.cmd test`。
- 修改 `frontend/` 下的任何内容前，必须先阅读 `frontend/AGENTS.md`；它继承本文件的企业级原则，并补充前端体验、OpenAPI、JWT、SSE 和验证约束。
- 历史记录中未加 `backend/` 前缀的旧路径，表示目录重组前的路径；后续新增或修改的路径以此处结构为准。

## 近期变更记录（2026-07-23）

1. 配置外置化：MySQL、Redis、DashScope 和 JWT 改为从环境变量读取；`JWT_SECRET` 不再写死，并要求满足 HS256 最低 256 bit 密钥长度。
2. 统一接口基础：新增 `ApiResponse<T>`、`BusinessException`、`GlobalExceptionHandler`；登录和注册改为使用 `AuthRequest` 和 Bean Validation。
3. AI 调用收口：`AiService` 负责出题、评分提示词和 AI 响应校验；`DashScopeAiClient` 负责 DashScope HTTP 请求。
4. 面试业务分层：新增 `InterviewService` / `InterviewServiceImpl`，承载出题缓存、AI 评分和 `Conversation` 保存；`StudyService` / `StudyServiceImpl` 承载错题本和学习进度。
5. Controller 瘦身：`AiController` 的普通出题、评分、错题本和进度接口均改为调用业务 Service，并统一返回 `ApiResponse`。
6. SSE 评分整理：移除 Controller 中重复的 DashScope HTTP 请求和手动 JWT 解析；新增 `SseExecutorConfig`、`InterviewScoreSseAdapter`，由 Spring 管理评分线程池和 SSE 事件推送。
7. 认证页面适配：`login.html`、`register.html` 已改为读取 `ApiResponse<AuthTokenResponse>` 的 `data.token` 与 `data.username`；主页面 `index.html` 的普通接口适配暂缓。
8. JWT 安全基础：新增 `JwtUserPrincipal` 和 `JwtAuthenticationFilter`，用于将标准 Bearer Token 转换为 `SecurityContext` 认证信息；过滤器已通过 `addFilterBefore(...)` 注册到无状态安全链，`UserContext` 已优先从 `SecurityContext` 获取用户。
9. 路由权限启用：登录、注册、静态资源和旧 SSE 评分接口明确放行；其余接口已改为 `authenticated()`，必须携带有效 Bearer Token。
10. SSE 认证兼容：旧静态页面的 SSE 评分仍通过 URL Token 认证，但该回退逻辑已限制为 `/api/question/score/stream`，不再向其他接口开放。
11. 统一安全响应：新增 `RestSecurityExceptionHandler` 并注册到安全链；缺失、非法或过期 Token 返回 `ApiResponse` 格式的 `401`，拒绝访问返回 `ApiResponse` 格式的 `403`。
12. JWT 过滤器测试：新增 `JwtAuthenticationFilterTest`，覆盖有效 Token、缺失 Token 和非法 Token；使用 Mockito 模拟 `JwtUtil`，测试尚未执行。
13. Security Web 测试：新增 `SecurityApiTest`，覆盖 `/api/mistakes` 的缺失 Token `401`、非法 Token `401` 和有效 Token `200`，验证过滤器、安全链与统一错误响应的联通行为。
14. 验证状态：当前代理环境执行 Maven Wrapper 时缺少 `powershell` 命令，未能运行测试；用户本地环境需要执行 `.\mvnw.cmd test`。历史上 `contextLoads` 曾通过，本轮新增测试尚未实际执行。

## 当前实际进度（2026-07-23）

当前已经完成或部分完成的内容：

- 配置外置化已完成：MySQL、Redis、JWT、DashScope 配置使用环境变量占位符。
- `JWT_SECRET` 已改为由环境变量提供，密钥必须至少满足 HS256 的 256 bit 要求。
- 已新增 `ApiResponse<T>`、`BusinessException`、`GlobalExceptionHandler`。
- 登录注册已使用 `AuthRequest` 和 Bean Validation，并返回 `ApiResponse`。
- `AiService` 已负责出题、评分提示词和评分结果解析；`client/DashScopeAiClient` 已负责 DashScope HTTP 调用。
- `InterviewService` 已开始承载面试训练业务：出题和评分逻辑已从 `AiController` 移入 `InterviewServiceImpl`。
- 错题本和学习进度已迁移到 `StudyService`；`AiController` 不再直接操作 `ConversationMapper`。
- SSE 评分已复用 `InterviewService.scoreAnswer`，不再在 Controller 中重复发起 DashScope HTTP 请求；评分结果会统一保存到数据库。
- 新增 `SseExecutorConfig` 和 Spring 管理的 `sseTaskExecutor`，避免每次 SSE 评分临时创建线程池。
- 新增 `sse/InterviewScoreSseAdapter`，负责 SSE 生命周期、异步调度、结果序列化和事件推送；它属于 Web 传输适配层而不是业务 Service，`AiController.streamScore` 已缩为参数接收和适配器委托。
- 登录和注册页已适配 `ApiResponse<AuthTokenResponse>`，能够从 `data.token` 和 `data.username` 保存登录态。
- 已新增 `JwtUserPrincipal` 和 `JwtAuthenticationFilter`；过滤器已在 `UsernamePasswordAuthenticationFilter` 前注册，安全链使用 `STATELESS` 会话策略。
- `UserContext` 已改为优先从 `SecurityContext` 的 `JwtUserPrincipal` 获取用户 ID；仅旧 SSE 评分接口保留 URL Token 回退。
- 登录、注册、静态资源和旧 SSE 评分接口已放行；其余接口已要求 `authenticated()`。
- `RestSecurityExceptionHandler` 已统一 Spring Security 的 `401/403` JSON 响应，返回结构与 `ApiResponse` 一致。
- 已新增 `JwtAuthenticationFilterTest`，覆盖有效、缺失和非法 Token 的认证上下文行为。
- 已新增 `SecurityApiTest`，覆盖受保护接口在缺失、非法和有效 Token 下的 `401/200` 响应。
- 最近一次可用的上下文测试报告为通过；每次 Service 重构后都必须重新执行 Maven 测试。

当前仍未完成的内容：

- 静态主页面 `index.html` 的出题、错题本和学习进度尚未适配 `ApiResponse.data`。用户当前决定优先完善后端并为前后端分离做准备，因此暂停继续修改静态前端。
- 当前 SSE 使用 URL Token 仅为旧静态页面兼容方案，不应作为前后端分离后的正式认证方式；后续需改为 Cookie 或支持请求头的流式方案。
- JWT 过滤器和 Security Web 测试已添加但尚未执行；仍缺少 `InterviewService`、`StudyService` 和 SSE 的针对性测试。

## 长期目标技术栈

目标是逐步完善为成熟单体项目，后续可演进到微服务：

- Spring Boot
- MySQL
- MyBatis-Plus
- Redis
- Nacos 配置中心
- Swagger / OpenAPI
- Spring Security + JWT
- XXL-JOB
- GitLab
- Jenkins
- Vue 3 前端项目
- Docker 化部署

## 分层与职责约定

分层的目标不是“一个 HTTP 接口对应一个类”，而是按业务能力分组，并让每层只负责自己的事情。

推荐调用方向：

```text
Controller
    -> Service 接口
        -> Service 实现
            -> AI 客户端 / Redis / Mapper
                -> DashScope / MySQL
```

各层职责：

- Controller：路由、HTTP 参数接收、参数校验、调用 Service、返回 `ApiResponse` 或 `SseEmitter`。
- 业务 Service：业务规则、登录判断、缓存协调、AI 评分流程、数据库保存流程。
- AI 客户端或 `AiService`：只负责 DashScope 请求、响应解析和 AI 结果校验。
- Mapper：只负责 MyBatis-Plus 数据库访问，不在其中编写业务流程。
- Entity：数据库表映射；DTO：HTTP 请求和响应模型。

业务能力分组约定：

- `InterviewService` 负责面试训练相关用例，例如出题和评分。
- `StudyService` 负责错题本和学习进度。
- 不按“一个接口一个 Service”拆分，也不为了形式给每个 Mapper 手写空的实现类；MyBatis-Plus Mapper 的实现由框架生成。
- Service 接口和实现类用于明确业务边界；如果某个类没有独立边界，不强行增加空接口。

当前目标结构：

```text
controller/
  AuthController.java
  AiController.java              # 逐步瘦身，后续可按资源重命名或拆分

service/
  AiService.java                 # 提示词编排和 AI 结果解析
  InterviewService.java          # 面试训练业务接口
  StudyService.java              # 错题本和学习进度业务接口
  UserService.java
  impl/InterviewServiceImpl.java # 出题、评分及其业务编排
  impl/StudyServiceImpl.java     # 错题本和学习进度业务编排

security/
  JwtAuthenticationFilter.java   # Bearer Token -> SecurityContext
  JwtUserPrincipal.java          # 当前认证用户

config/
  SseExecutorConfig.java         # SSE 专用线程池

sse/
  InterviewScoreSseAdapter.java  # SSE Web 传输适配器

client/
  AiClient.java                  # AI 文本生成客户端抽象
  DashScopeAiClient.java         # DashScope HTTP 客户端

mapper/
  UserMapper.java
  ConversationMapper.java
```

Controller 瘦身时，先按一个完整业务用例迁移并验证，再迁移下一个用例；不要把多个未验证的重构同时合并。

## 推荐改造顺序

### 第 1 阶段：基础治理

目标：让项目从 Demo 变成可维护的后端骨架。

优先事项：

1. 敏感配置外置化
   - DashScope API Key
   - JWT Secret
   - MySQL 用户名和密码
   - Redis 配置

2. 统一响应结构
   - 新增 `ApiResponse<T>`
   - Controller 不再直接返回 `String`、`Map`

3. 统一异常处理
   - 新增 `GlobalExceptionHandler`
   - 新增业务异常类，例如 `BusinessException`

4. 参数校验
   - 引入 Bean Validation
   - 使用 DTO 接收入参

5. Controller 瘦身和业务分层
   - `AiController` 只负责 HTTP 入参和响应
   - 相关用例按业务能力分组，不按一个接口一个 Service
   - 出题和评分移动到 `InterviewService`
   - 错题本和进度统计移动到 `StudyService`
   - SSE 的传输编排与面试评分业务分开，避免 Controller 同时处理业务和线程细节

### 第 2 阶段：数据模型和 MyBatis-Plus

目标：让业务数据结构清晰。

建议表：

- `user`
- `interview_question`
- `answer_record`
- `score_result`
- `mistake_record`
- `study_progress`
- `ai_call_log`

MyBatis-Plus 配置：

- 自动填充 `create_time`、`update_time`
- 分页插件
- 逻辑删除，按需
- 乐观锁，按需

### 第 3 阶段：安全体系

目标：让登录和接口权限真正可靠。

事项：

- 不再使用 `anyRequest().permitAll()`
- 登录、注册、Swagger 放行
- 业务接口必须携带 JWT
- 新增 JWT 过滤器
- 用户上下文从 SecurityContext 获取
- 密码使用 BCrypt
- Token 过期、非法、缺失都返回统一错误

### 第 4 阶段：Swagger / OpenAPI

目标：让接口可查看、可调试、方便前后端联调。

事项：

- 接入 `springdoc-openapi`
- 配置 Swagger UI
- 给 Controller、DTO、字段加注解
- 配置 JWT 认证入口

### 第 5 阶段：Redis

目标：让缓存、限流和临时状态有明确边界。

适用场景：

- AI 出题缓存
- 用户每日调用次数
- 防重复提交
- Token 黑名单
- 热门知识点缓存
- 学习统计缓存

原则：

- 核心业务记录必须落 MySQL
- Redis 只存可重建数据或临时状态

### 第 6 阶段：Nacos

目标：统一管理不同环境配置。

先作为配置中心使用：

- `dev`
- `test`
- `prod`

可放入 Nacos 的配置：

- 数据库连接
- Redis 连接
- JWT 配置
- AI 模型配置
- API Key
- 限流配置
- XXL-JOB 配置

### 第 7 阶段：XXL-JOB

目标：处理后台定时任务。

适合任务：

- 每日学习报告
- 清理过期缓存
- 统计学习进度
- AI 调用日志归档
- 失败任务补偿

不适合：

- 实时 AI 评分
- 同步接口主流程

### 第 8 阶段：GitLab + Jenkins

目标：建立完整工程协作和部署流程。

GitLab：

- `main`
- `develop`
- `feature/*`
- `hotfix/*`

Jenkins：

1. 拉取代码
2. Maven 编译
3. 单元测试
4. 打包
5. 构建 Docker 镜像
6. 推送镜像
7. 部署测试环境
8. 人工确认后部署生产

### 第 9 阶段：前端独立化

目标：从静态 HTML 升级成正式前端工程。

建议：

## Latest change (2026-07-25)

- Added provider selection through app.ai.provider: dashscope remains the default and change2proapi selects the new relay client.
- Added Change2ProResponsesAiClient for the relay's OpenAI Responses-compatible protocol. It maps the existing AiClient system prompt to instructions, user content to input, optional reasoning effort to reasoning.effort, and disabled response storage to store=false.
- Added external-only Change2Pro settings: CHANGE2PRO_API_KEY, CHANGE2PRO_ENDPOINT (full endpoint URL), CHANGE2PRO_MODEL (default gpt-5.6-luna), CHANGE2PRO_REASONING_EFFORT, and CHANGE2PRO_DISABLE_RESPONSE_STORAGE.
- Renamed the Spring-managed HTTP client bean from dashScopeHttpClient to provider-neutral aiHttpClient; DashScope and Change2Pro clients share its timeout policy.
- Made the inactive DashScope API key optional so a Change2Pro-only deployment does not require DASHSCOPE_API_KEY.
- Mirrored the provider-selection and Change2Pro environment-variable contract in application-prod.properties, so local and production profiles do not diverge.
- Added Change2ProResponsesAiClientTest for request construction, top-level and nested Responses API output extraction, non-2xx error handling, and incomplete relay configuration handling.
- Verification passed: the client test suite passed (4 tests), the full regression passed (41 tests, 0 failures, 0 errors), and Spring context startups with app.ai.provider=change2proapi passed for both the default and prod profiles using test-only placeholder values.
- A real local Change2Pro end-to-end smoke test is complete: after starting local Redis and authenticating with a fresh JWT, POST /api/question/ask with refresh=true returned code 200 and generated a new question through the configured https://api.change2pro.com/v1/responses endpoint and gpt-5.6-luna model.
- The real scoring flow is also verified: POST /api/question/score returned a score result and Navicat confirmed the matching AnswerRecord row was inserted into answer_record.
- No API key is stored in project files. The next planned provider work is the direct DeepSeek Chat Completions implementation and its equivalent smoke test.
- Added `DeepSeekAiClient` as a third conditional `AiClient` implementation. It is active only when `AI_PROVIDER=deepseek`, calls the OpenAI-compatible DeepSeek Chat Completions endpoint, and maps the shared system prompt and user content to `messages` without changing Controllers, business Services, persistence, or frontend code.
- Added external-only DeepSeek settings in both local and production property files: `DEEPSEEK_API_KEY`, `DEEPSEEK_ENDPOINT` (default `https://api.deepseek.com/chat/completions`), and `DEEPSEEK_MODEL` (default `deepseek-chat`). No DeepSeek credential is stored in the project.
- Added `DeepSeekAiClientTest` for Chat Completions request construction, response extraction from `choices[0].message.content`, non-2xx handling, and incomplete configuration rejection before any HTTP request is sent.
- Verification passed: the DeepSeek client test passed (3 tests), a Spring context startup with `app.ai.provider=deepseek` and test-only placeholder configuration passed, and the full regression passed with 44 tests, 0 failures, and 0 errors while Flyway was disabled.
- The remaining DeepSeek work is a user-controlled local smoke test after valid environment variables are configured; it should verify question generation and scoring without exposing the API key.
- The approved product requirement is now runtime per-user AI model selection with no backend restart. A user with no explicit preference falls back to the system default `deepseek / deepseek-v4-flash`; users select only from an allowlisted catalog and never submit raw provider names, model codes, or credentials.

## Runtime User AI Model Selection Plan (2026-07-25)

### Scope and Ownership

- The selection is a persistent user preference, not a process-wide `AI_PROVIDER` switch and not a request-level free-form override.
- API keys, endpoints, and provider authentication remain external configuration. Database rows contain only non-sensitive provider and model identifiers.
- A user may read and update only their own preference. The initial version has no public model catalog administration endpoint because the project does not yet have an administrator role model.
- New users and users without a preference use the policy default `deepseek / deepseek-v4-flash`. A disabled or unavailable model must not be selectable by new requests.

### Data Contract and Migration Rules

- `V2__add_user_ai_model_selection.sql` is the next production schema migration. It creates `ai_model`, the singleton `ai_model_policy`, and `user_ai_preference`.
- `ai_model` is the allowlist of selectable models. V2 initially seeded `deepseek / v4-flash`; V3 corrects that existing row in place to the official `deepseek / deepseek-v4-flash` identifier. `change2proapi / gpt-5.6-luna` remains another selectable catalog item.
- `user_ai_preference.user_id` is both the primary key and user foreign key, enforcing at most one explicit preference per user at the database level.
- `answer_record.score_ai_model_id` is nullable for legacy records and will record the actual scoring model for all new records after the runtime routing phase. Historical records are not falsely backfilled.
- V1 is immutable because it has already run locally. V2 must be applied by Flyway as part of the matching backend release; it must not be executed manually in production through Navicat.

### HTTP Contract

- `GET /api/ai/models` will return only selectable and configured models as `AiModelResponse` records.
- `GET /api/users/me/ai-preference` will return the explicit preference or the policy default as `AiModelPreferenceResponse`.
- `PUT /api/users/me/ai-preference` will accept `UpdateAiModelPreferenceRequest { modelId }`; the user ID comes exclusively from JWT authentication.
- The existing question and scoring request contracts will not expose raw provider or model fields. Their Services will resolve the effective model internally.

### Runtime Design for the Next Phase

- All provider clients will become available to Spring. `AiClientRegistry` will route every invocation by the effective model provider at request time.
- `AiService` will keep prompt construction and result parsing. `InterviewService` will resolve the authenticated user's effective model before invoking it.
- Question cache keys will include `aiModelId`, for example `interview:question:{aiModelId}:{tag}`, preventing cross-model cache reuse.
- Provider configuration is validated when a model is selected or invoked. Missing credentials produce a non-sensitive model-unavailable error and never expose a secret.

### Delivery Phases and Current State

1. Define the schema and HTTP DTO contract: V2 and immutable DTOs are present in source control, and V2 has been applied to the confirmed local database through Flyway.
2. Verify V2's seeded catalog rows, default policy, foreign keys, and indexes in the local database before mapping persistence code. Completed locally.
3. Add entities, mappers, preference/catalog Services, and protected preference APIs. This phase and its local authenticated HTTP smoke verification are complete.
4. Replace the startup-only client selection with the runtime `AiClientRegistry`, model-aware cache keys, and scoring-model persistence. Completed in source and unit tests.
5. Add unit, Web, Flyway integration, cache-isolation, and two-user no-restart smoke tests before any cloud release. Unit and cache-isolation coverage is complete; authenticated local HTTP smoke verification remains before a cloud release.

- All provider clients are now Spring-managed at the same time. `AiClientRegistry` routes each invocation by the effective model provider and converts unavailable client configuration into a non-sensitive `503` response.
- The temporary `AI_PROVIDER` compatibility path was subsequently removed. Provider/model identity now comes only from the database allowlist and resolved user preference; provider connection settings remain external configuration.
- Phase 1 source verification passed: Maven full regression ran with `spring.flyway.enabled=false` and completed 44 tests with 0 failures and 0 errors.
- Local migration verification status: the user started the application with `FLYWAY_ENABLED=true`, Flyway successfully applied V2 to the confirmed local database, and Navicat shows the new tables. The user then verified the initial V2 `deepseek / v4-flash` default policy, `answer_record.score_ai_model_id`, and the expected foreign keys through read-only SQL queries. V3 is pending the next local application restart. No cloud database operation has been executed.
- Added `AiModel`, `AiModelPolicy`, and `UserAiPreference` persistence mappings with their MyBatis-Plus mappers; `AnswerRecord` now maps the nullable `scoreAiModelId` column introduced by V2.
- Added immutable `EffectiveAiModel`, `UserAiPreferenceService`, and `AiModelCatalogService`. Effective model resolution now prefers an enabled user preference and otherwise falls back to the enabled policy default; a broken default policy fails with a non-sensitive server error.
- Updating a preference validates that the requested catalog model exists and is enabled, then uses a MySQL atomic `INSERT ... ON DUPLICATE KEY UPDATE` mapper method to avoid a select-then-insert race.
- Added focused catalog and preference Service tests for enabled selection, fallback, disabled-model protection, authentication, default-policy validation, response mapping, and query filtering.
- Verification passed with Flyway disabled: targeted model-selection Service tests passed (11 tests) and the full Maven regression passed with 55 tests, 0 failures, and 0 errors. No cloud database operation was performed.
- Added `AiModelController` with authenticated `GET /api/ai/models`, `GET /api/users/me/ai-preference`, and `PUT /api/users/me/ai-preference` routes. The Controller receives the user ID exclusively from `UserContext`; update requests accept only the validated `modelId` DTO field.
- No security allowlist changed: the existing `anyRequest().authenticated()` rule protects all three routes. Controllers delegate to catalog/preference Services and do not access mappers or credentials directly.
- Added `AiModelControllerTest` covering missing-token `401`, catalog retrieval, effective-preference retrieval, current-user-only update delegation, non-positive `modelId` validation, and a disabled-model business error.
- Verification passed with Flyway disabled: the new MVC test slice passed (6 tests) and the full Maven regression passed with 61 tests, 0 failures, and 0 errors. No local or cloud migration was executed during tests.
- User-controlled local HTTP smoke verification passed against the confirmed local `interview_db`: authenticated model catalog and default-preference reads returned the then-current V2 `deepseek / v4-flash` model, then `PUT /api/users/me/ai-preference` persisted model ID `2`; a subsequent authenticated read returned `change2proapi / gpt-5.6-luna` with `defaultSelection=false`. No cloud database operation was performed.

- Added `AiClient` and `DashScopeAiClient`, moving DashScope request construction, authentication headers, HTTP status handling, and response-text extraction out of `AiService`.
- Added `HttpClientConfig` so the DashScope HTTP client is Spring-managed rather than manually constructed inside a business service.
- Added `AiServiceTest` for question delegation, valid score parsing, out-of-range scores, and malformed score payloads; full Maven verification passed with all 37 tests successful.
- Moved `InterviewScoreStreamService` out of the business-service layer and renamed it to `sse/InterviewScoreSseAdapter`.
- The SSE adapter retains `SseEmitter`, asynchronous dispatch, serialization, and transport error handling, while `InterviewService` retains scoring and persistence.
- Renamed and moved the SSE unit test to `InterviewScoreSseAdapterTest`; full Maven verification passed with all 33 tests successful.
- Removed the unused `AiService` dependency from `AiController` and its MVC test slice; the controller now declares only the collaborators required by its HTTP use cases.
- Surefire reports confirm that all 33 tests pass after this dependency-only cleanup; the outer Maven command exceeded the tool timeout after tests completed.
- Added immutable `MistakeResponse` and `StudyProgressResponse` API DTOs. `Conversation` entities and MyBatis aggregation maps are now mapped inside `StudyServiceImpl` rather than returned from the HTTP layer.
- Extended MVC coverage to verify the mistake response does not expose `userId` and that progress returns named, typed fields.
- Maven verification passed: all 33 tests completed with no failures or errors.
- The normal scoring endpoint now supports `POST /api/question/score` with validated `ScoreRequest` JSON input; the legacy GET endpoint remains temporarily for the static page.
- Added Security MVC tests for successful JSON scoring, blank answers, and answers exceeding the 5,000-character limit.

- Fixed `SecurityConfig` compilation compatibility by using explicit `AntPathRequestMatcher` instances for all path rules.
- Authorization rules are unchanged: static resources, authentication APIs, and the legacy SSE endpoint remain public; other APIs require JWT authentication.
- Fixed `SecurityApiTest` slice setup by mocking `ConversationMapper`, so security tests do not require a MyBatis `sqlSessionFactory`.
- Also mocked `UserMapper`, the second mapper registered by the application-wide `@MapperScan`.
- Maven verification completed successfully: 7 tests passed, including the application context, JWT filter, and protected API security tests.
- The project wrapper still cannot start in this environment because `powershell` is missing from `PATH`; direct Maven execution was used for verification.
- Added `InterviewServiceImplTest` for Redis cache hits, forced question refresh, score persistence, and unauthenticated access.
- The first Service test run exposed and fixed a test-only unnecessary Redis stub and an incorrectly encoded login-message assertion.
- `InterviewServiceImplTest` now passes: 4 tests passed with no failures or errors.
- Added `StudyServiceImplTest` for mistake queries, progress queries, query conditions, and unauthenticated access.
- `StudyServiceImplTest` now passes: 4 tests passed with no failures or errors.
- Maven reports only an unchecked-operation warning from Mockito's generic `QueryWrapper` captor; the next step is the combined regression run.
- Added `InterviewScoreSseAdapterTest` for unauthenticated access, successful scoring completion, and scoring failure completion.
- The first SSE test run showed that `SseEmitter.onCompletion` is container-driven and cannot be used as a direct unit-test assertion; the test now focuses on task execution, scoring delegation, serialization, and swallowed failure handling.
- A follow-up test compilation issue was fixed by declaring the checked `ObjectMapper` exception on the test methods.
- `InterviewScoreSseAdapterTest` now passes: 3 tests passed with no failures or errors.
- The next step is the combined full regression run.
- Refactored `AiController` to constructor injection and removed the unused `/api/hello` and `/api/stream-test` demo endpoints; business endpoints remain unchanged.
- Full regression after the Controller change passed: 18 tests passed, with 0 failures and 0 errors.
- Added `AuthService` and `AuthServiceImpl`; registration, login, and JWT creation are now orchestrated outside `AuthController`.
- Full regression after the authentication refactor passed: 18 tests passed, with 0 failures and 0 errors.
- Added `AuthServiceImplTest` for registration, login, JWT response creation, and failed-login behavior.
- `AuthServiceImplTest` passed with 3 tests, and the full regression now passes with 21 tests, 0 failures, and 0 errors.
- Added `UserServiceTest` for password encoding, duplicate usernames, valid login, missing users, and invalid passwords.
- `UserServiceTest` passed with 5 tests, and the full regression now passes with 26 tests, 0 failures, and 0 errors.
- Added `PasswordEncoderConfig` and refactored `UserService` to constructor injection with the shared `PasswordEncoder` dependency.
- The `UserService` refactor passed both its targeted tests and the full regression: 26 tests passed, with 0 failures and 0 errors.
- Removed the unused `ConversationMapper.getProgressStats()` method and standardized mapper registration on application-level `@MapperScan`.
- Full regression after the mapper cleanup passed: 26 tests passed, with 0 failures and 0 errors.
- Added `QuestionRequest` and `ScoreRequest` DTOs with validation constraints for the future JSON interview API contract; existing routes remain unchanged.
- Full regression after adding interview DTOs passed: 26 tests passed, with 0 failures and 0 errors.
- Added the validated JSON `POST /api/question/ask` endpoint while retaining the legacy GET endpoint for static-page compatibility, and added Web tests for valid and invalid JSON requests.
- The question endpoint migration passed its 6 Web tests and the full regression now passes with 29 tests, 0 failures, and 0 errors.
- Added the collaboration rule that every proposed next step must explain its reason, benefits, and consequences of not doing it; code changes require explicit user confirmation such as `开始改`, `直接做`, or `帮我改`.

## Next-step explanation rule

When telling the user what to do next, always explain all four parts before asking for confirmation:

1. What will be changed and which files or layer are involved.
2. Why this change is needed at the current project stage.
3. What concrete benefits the change provides.
4. What risks, limitations, or maintenance problems remain if it is not changed.

After the explanation, stop and wait for the user to confirm. Do not edit project code, configuration, tests, or structure merely because the user asked `下一步`.

Only start modifications when the user explicitly says `开始改`, `直接做`, `帮我改`, `修改`, or an equivalent unambiguous instruction. After an explicit confirmation, make only the stated small change, update this file, and run the proportionate verification.

## Current progress and backend roadmap (2026-07-24)

This section is the current status correction for the older 2026-07-23 notes above.

### Verified progress

- The first-round backend foundation is complete: externalized configuration, `ApiResponse`, centralized business exceptions, validation for authentication requests, JWT authentication, protected routes, `SecurityContext` user lookup, Service extraction, SSE transport orchestration, and targeted tests.
- Targeted tests now cover `JwtAuthenticationFilterTest`, `SecurityApiTest`, `AiServiceTest`, `InterviewServiceImplTest`, `StudyServiceImplTest`, and `InterviewScoreSseAdapterTest`.
- The user has confirmed that the test run is complete. The project still requires the normal local Maven command to remain the final verification entry point: `./mvnw.cmd test`.

### Still incomplete

- `AiController` still contains demo endpoints, field injection, and the `/api/stream-test` endpoint creates a raw `Thread`.
- `AuthController` still creates JWT tokens directly; authentication orchestration has not been moved into an `AuthService` boundary.
- Some HTTP responses still expose persistence entities or untyped maps instead of response DTOs.
- Interview endpoints still use GET query parameters for question and answer content and have no dedicated request DTO validation.
- SSE still supports URL Token authentication for legacy static-page compatibility and does not yet have the final frontend-separated authentication design or complete disconnect cancellation handling.
- `UserService` still uses field injection and creates `BCryptPasswordEncoder` directly; it is not yet consistent with the Service interface/implementation pattern.
- `ConversationMapper.getProgressStats()` is unused, and mapper registration currently duplicates `@Mapper` with application-wide `@MapperScan`.
- The database model is still centered on `user` and `conversation`; the mature domain model and database integration tests are not complete.
- The static `index.html` still needs `ApiResponse.data` adaptation, but frontend migration remains paused until the backend contract is stable.

### Backend completion roadmap

#### Phase 1: Clean the existing backend boundaries

Goal: remove demo code and make each layer obey its responsibility without changing core behavior.

Work:

1. Refactor `AiController` to constructor injection.
2. Remove or isolate `/api/hello` and `/api/stream-test` as development-only endpoints.
3. Refactor `AuthController` to constructor injection.
4. Add `AuthService` and move register/login orchestration and JWT creation out of the Controller.
5. Remove the unused `ConversationMapper.getProgressStats()` method.
6. Keep one mapper registration strategy: prefer application-level `@MapperScan` and remove duplicate mapper annotations.

Completion criteria:

- Controllers only receive HTTP input, validate it, call a Service, and format the response.
- No Controller creates threads, calls database mappers, hashes passwords, or creates JWT tokens.
- Run `./mvnw.cmd test` after each small refactor.

#### Phase 2: Freeze the HTTP API contract

Goal: make the backend ready for an independent frontend.

Work:

1. Add request DTOs such as `QuestionRequest` and `ScoreRequest`.
2. Change state-changing and answer-submission endpoints to POST.
3. Use `@RequestBody` for JSON requests and keep form binding only where explicitly required.
4. Add `@NotBlank`, `@Size`, and reasonable maximum lengths for tag, question, and answer fields.
5. Add response DTOs such as `MistakeResponse` and `ProgressResponse` instead of returning `Conversation` or `Map<String, Object>`.
6. Extend global validation handling to cover `MethodArgumentNotValidException` and constraint violations.

Completion criteria:

- Every public business endpoint has a documented request and response type.
- No persistence Entity is returned directly from a Controller.
- Invalid, empty, and oversized input returns the same `ApiResponse` error shape.

#### Phase 3: Stabilize the domain and data layer

Goal: make data ownership explicit before adding more features.

Work:

1. Decide whether `conversation` remains a transitional aggregate or is split into question, answer, score, mistake, and progress records.
2. Add database unique constraints, especially for usernames.
3. Move password encoding behind an injected `PasswordEncoder` Bean.
4. Add transaction boundaries for registration and score persistence where multiple writes are introduced.
5. Add MyBatis-Plus conventions only when needed: pagination, automatic timestamps, logical deletion, and optimistic locking.
6. Add database integration tests after the schema is stable.

Completion criteria:

- Entity, Mapper, and Service responsibilities are explicit.
- Registration is protected against duplicate-user races by a database constraint.
- Core persistence workflows have integration coverage, not only Mockito tests.

#### Phase 4: Finish security and SSE for frontend separation

Goal: remove legacy authentication compromises and make streaming behavior production-safe.

Work:

1. Choose the final authentication transport for the independent frontend: HttpOnly Cookie or a streaming client that can send the Authorization header.
2. Remove URL Token fallback from `UserContext` after the frontend no longer depends on it.
3. Add SSE timeout, client disconnect, error, and task cancellation handling.
4. Ensure rejected requests use consistent JSON for normal HTTP endpoints and an explicit SSE error event for streaming endpoints.
5. Add tests for expired tokens, forbidden access, stream disconnects, and service failures.

Completion criteria:

- Tokens are never placed in query strings in the production authentication path.
- A disconnected SSE client does not leave an unbounded task running.
- Authentication behavior is covered for missing, invalid, expired, and valid credentials.

#### Phase 5: API documentation and operations

Goal: document a stable backend contract after the preceding API work is complete.

Work:

1. Add OpenAPI/Swagger only after request and response DTOs are stable.
2. Document authentication, error responses, SSE behavior, and validation rules.
3. Add a JWT security scheme for Swagger UI.
4. Add environment profiles and confirm required secrets fail fast outside development.
5. Add structured logging around authentication failures and AI calls without logging tokens, passwords, or full answers.

Completion criteria:

- Swagger describes the same contract consumed by the future frontend.
- Sensitive configuration and user content are not exposed in logs or documentation examples.

#### Phase 6: Independent frontend

Goal: replace static-page coupling with a Vue 3 frontend only after the backend contract is frozen.

Work:

1. Create a separate Vue 3/Vite project.
2. Add Axios, router, state management, route guards, and API error handling.
3. Adapt login, question, scoring, mistakes, and progress flows to the final DTOs.
4. Implement the chosen SSE authentication approach.
5. Configure CORS for development and production origins.

Completion criteria:

- The frontend does not depend on server-rendered static HTML behavior.
- All API calls use the documented request and response contracts.

### Runtime Routing Update (2026-07-26)

- `AiClient` now receives the runtime `modelCode`; DashScope, DeepSeek, and Change2Pro clients are all registered concurrently and are selected by `AiClientRegistry` using the resolved provider.
- `AiService` retains prompt construction and score-result validation, while its public generation and scoring methods now require `EffectiveAiModel`.
- `InterviewServiceImpl` resolves `UserAiPreferenceService.resolveEffectiveModel(userId)` for every question and scoring request. A preference change therefore takes effect on the next request without a backend restart.
- Question cache keys now include the current user and model ID in the form `question:{userId}:model:{aiModelId}:{tag}`, preventing reuse of a question generated by a different selected model.
- New scoring records persist `answer_record.score_ai_model_id` from the effective model. Existing records remain unchanged because the V2 column is nullable.
- Added routing tests for explicit model calls, model-switch cache isolation, two users selecting different models without a restart, and score-model persistence. The targeted suite passed 12 tests; the full Maven regression passed 73 tests with 0 failures and 0 errors using `-Dspring.flyway.enabled=false`.
- This code and test work did not run Flyway, change local MySQL data, or perform any cloud database operation.

### Runtime Model Availability Update (2026-07-26)

- `AiClient` now declares `isConfigured()`. DashScope, DeepSeek, and Change2Pro implement it by checking their required API key and endpoint values without logging or exposing either value.
- `AiClientRegistry.isModelAvailable(provider, modelCode)` now combines provider registration, a nonblank model code, and completed provider configuration. Runtime generation also fails with the existing non-sensitive `503` before sending an HTTP request when configuration is incomplete.
- `AiModelCatalogServiceImpl` now filters the database-enabled allowlist through runtime availability, so `GET /api/ai/models` does not return models whose Provider is not configured in the active process.
- `UserAiPreferenceServiceImpl` rejects a database-enabled but runtime-unavailable model with `503` and does not persist the preference. A previously saved preference whose Provider becomes unavailable falls back to the configured policy default; an unavailable policy default fails with a non-sensitive `503`.
- Added focused client, registry, catalog, and preference tests for configuration status, catalog filtering, rejected updates, fallback behavior, and unavailable defaults. Targeted verification passed 31 tests; full Maven regression passed 78 tests with 0 failures and 0 errors using `-Dspring.flyway.enabled=false`.
- This hardening added no database migration, did not run Flyway, and did not call an external AI Provider.

### Legacy Provider Default Removal (2026-07-26)

- Removed the unused process-wide `AI_PROVIDER` compatibility path, legacy default-model fields, and unused `AiService` overloads. There is no remaining business entry point that can select a model without an `EffectiveAiModel` resolved from user preference and the database allowlist.
- `AiProperties` now contains only provider connection settings: API keys, endpoints, and Change2Pro technical options. `DASHSCOPE_MODEL`, `DEEPSEEK_MODEL`, `CHANGE2PRO_MODEL`, and `AI_PROVIDER` are no longer read by the application; model codes are stored in `ai_model`.
- Updated local and production property templates and focused tests. Existing environment variables with the removed names are harmless but should be deleted from IDE run configurations and deployment manifests to prevent operator confusion.
- Targeted verification passed 26 tests. Full Maven regression passed 76 tests with 0 failures and 0 errors using `-Dspring.flyway.enabled=false`; the count decreased from 78 because two tests dedicated solely to the removed legacy path were intentionally deleted.
- This refactor did not change database schema, run Flyway, modify local data, or call an external AI Provider.

### True Upstream SSE Scoring (2026-07-26)

- Replaced the former character-by-character simulated output in `InterviewScoreSseAdapter` with true upstream streaming. The adapter now forwards each model text delta as a normal SSE message as soon as it arrives, so the legacy static page can continue appending `onmessage` payloads without an immediate frontend change.
- Added the provider-neutral `AiTextDeltaConsumer`, `AiSseEventReader`, and `AiStreamCancelledException` contracts in the client layer. `AiClient`, `AiClientRegistry`, `AiService`, and `InterviewService` now expose a streaming score path while preserving their existing synchronous APIs.
- DeepSeek uses OpenAI-compatible Chat Completions with `stream: true`; Change2Pro uses the Responses API with `stream: true` and parses `response.output_text.delta`; DashScope requests SSE with `X-DashScope-SSE: enable` and `parameters.incremental_output: true`.
- The score stream is accumulated server-side, then validated as `AiScoreResult` and persisted exactly once through `InterviewServiceImpl` only after the upstream stream has completed. A malformed or incomplete stream therefore does not create an answer record.
- Successful streams emit a named `done` event containing the serialized score result. Failures emit a named `error` event with a non-sensitive message. The existing static page can ignore these named events temporarily because it already receives the raw text deltas as normal messages.
- Added bounded SSE sessions through `app.sse.score-timeout-millis=${SSE_SCORE_TIMEOUT_MILLIS:45000}`. Completion, timeout, and connection errors trigger best-effort cancellation of the submitted task; client delivery failures also stop upstream consumption through interruption.
- Added client SSE parsing tests for DeepSeek, Change2Pro, and DashScope, plus registry, Service, persistence, and SSE adapter coverage. Maven reported `BUILD SUCCESS`: 82 tests passed with 0 failures and 0 errors using `-Dspring.flyway.enabled=false`.
- No application instance was started for a real model request, no external AI Provider was called, no Flyway migration ran, and no database data changed. A real authenticated browser smoke test remains necessary to verify each relay's actual event format and cancellation behavior; Change2Pro parsing follows the standard OpenAI Responses SSE event contract.

### Change2Pro Real Stream Verification (2026-07-27)

- Change2Pro was selected temporarily to verify the real streaming path. It is not the product's policy default.
- A real authenticated `curl.exe -N` call to `/api/question/score/stream` produced multiple upstream `data:` chunks before `event: done`, proving that Change2Pro is now delivering true upstream SSE rather than the removed character-animation behavior.
- The server emitted the final parsed `done` payload with score `7`; Navicat confirmed the matching newest `answer_record` row has `tag=HashMap`, `score=7`, and `score_ai_model_id=2`.
- The policy default remains DeepSeek. After V3 is applied, the same policy model row will resolve as `deepseek / deepseek-v4-flash`; `change2proapi / gpt-5.6-luna` remains an enabled user-selectable option. No cloud database was touched.

### DeepSeek Upstream Failure Diagnostics (2026-07-27)

- DeepSeek connection settings are configured externally in the IDEA run configuration. Before V3, the authenticated catalog exposed the stale V2 value `deepseek / v4-flash` and the real upstream request returned `400`.
- A live DeepSeek question request returned the public non-sensitive `502` response. The selected client reached the upstream request stage, but the previous implementation discarded the upstream HTTP status, so the root cause remains unknown until the retry below.
- `DeepSeekAiClient` now logs only a safe operation name and upstream HTTP status for non-2xx responses, for example `deepseek_upstream_failure operation=generate status=401`. IO/configuration failures use a safe category only. Logs never include API keys, Authorization headers, prompts, answers, or upstream response bodies.
- Added coverage for non-2xx streaming calls. The focused `DeepSeekAiClientTest` suite passed 5 tests with 0 failures and 0 errors using `-Dspring.flyway.enabled=false`.

### DeepSeek V4 Flash Model Identifier Correction (2026-07-27)

- The official DeepSeek API documentation confirms that the direct Chat Completions model identifier is `deepseek-v4-flash`; the configured `/chat/completions` endpoint is correct. The earlier `400` was caused by the stale catalog value `v4-flash`.
- Added immutable Flyway migration `V3__correct_deepseek_v4_flash_model_code.sql`. It updates the existing DeepSeek catalog row in place, so the policy default, persisted user preferences, and historical `answer_record.score_ai_model_id` references retain their current IDs.
- V2 remains unchanged because it has already been applied locally. The user restarted the local application with `FLYWAY_ENABLED=true`; Flyway applied V3 and the authenticated model catalog now returns `deepseek-v4-flash` for model ID `1`.
- Focused routing, catalog, preference, controller, and DeepSeek client verification passed: 41 tests, 0 failures, 0 errors, with Flyway disabled.
- Full Maven regression passed: 84 tests, 0 failures, 0 errors, with Flyway disabled. No local or cloud database migration was executed during test verification.

### DeepSeek SSE Completion Diagnostics (2026-07-27)

- A real DeepSeek SSE score request delivered many upstream deltas to the client, proving upstream streaming and delta forwarding work. It then emitted the named `error` event instead of `done`, so the final score JSON parsing, persistence, or completion delivery stage still requires diagnosis.
- DeepSeek synchronous question generation and synchronous scoring both succeeded after V3; the normal scoring request also inserted an `answer_record` with `score_ai_model_id=1`. The stream-specific error is therefore not a missing model configuration or a general database availability failure.
- `InterviewScoreSseAdapter` now separates safe failure logging into `task_submission`, `score_generation`, and `completion_delivery` phases. Logs contain only the phase, exception class, and `BusinessException` code where present; they never include exception messages, prompts, answers, model output, JWTs, or API keys.
- Added an adapter test that uses sensitive simulated exception/input values and verifies they are absent from log output. Targeted adapter verification passed 4 tests; the latest full Maven regression passed 84 tests, 0 failures, 0 errors, with Flyway disabled.

### DeepSeek SSE Score JSON Normalization (2026-07-27)

- The `score_generation` failure was isolated to final score-result parsing after upstream DeepSeek deltas had already been delivered. `AiService` now reinforces the exact-JSON output requirement and safely extracts the first balanced JSON object from Markdown-fenced or prose-wrapped model output before parsing it.
- The score contract remains strict: the root must be an object with numeric integral `score`, textual `correct_answer`, and textual `suggestion`; scores outside `0` through `10` and malformed or incomplete JSON still return the existing non-sensitive `502` failure.
- Added focused `AiServiceTest` coverage for Markdown code fences, leading/trailing prose, braces within JSON string values, and malformed JSON. The focused suite passed 8 tests; full Maven regression passed 87 tests with 0 failures and 0 errors using `-Dspring.flyway.enabled=false`.
- This change does not alter model selection, database schema, persistence rules, provider credentials, or logging policy. After a backend restart, a real DeepSeek stream-score request produced upstream deltas followed by `event:done` with score `7`, confirming the parsing and completion path locally.

### Current actual progress (2026-07-27)

- Runtime per-user model switching is complete and verified without a backend restart. The product default is `deepseek / deepseek-v4-flash`; Change2Pro GPT-5.6 Luna is optional.
- Change2Pro question generation, scoring persistence, and true upstream SSE have been verified locally.
- DeepSeek configuration and model correction are complete locally: V3 is applied, real question generation works, normal scoring works, and scoring persistence uses model ID `1`. The remaining issue is only the final completion stage of real SSE scoring.
- The DeepSeek SSE score parser now tolerates a balanced JSON object wrapped by Markdown or explanatory text while retaining schema and score-range validation. A real authenticated DeepSeek stream-score retry completed with `event:done`; the returned score was `7`, with no `event:error` or `sse_score_failure` reported. A read-only Navicat query confirmed the newest stream-created `answer_record` has `score_ai_model_id=1`.
- Added the root `README.md` as the repository entry point. It documents the verified backend scope, non-sensitive environment-variable contract, local startup and test commands, API areas, migration rules, and project navigation; later updates complete the Swagger/OpenAPI and JWT usage documentation.
- Started Swagger/OpenAPI integration by adding `org.springdoc:springdoc-openapi-ui:1.7.0`, which is compatible with Spring Boot `2.7.18`. Maven compile completed successfully; no application instance, Flyway migration, database mutation, or external AI call occurred. The next Swagger step is metadata, JWT Bearer documentation, and API-path configuration.
- Added `OpenApiConfig`, which registers the API title, version, description, and reusable `bearerAuth` JWT scheme. Maven compile passed with 62 main source files.
- Local backend start configuration recovery and integration verification (2026-07-27): the completed local configuration now lives in an external per-user directory rather than Maven resources, and the stale `target/classes` copy was removed so Maven cannot package local secrets into the JAR. IDEA now loads it through `SPRING_PROFILES_ACTIVE=local` and `SPRING_CONFIG_ADDITIONAL_LOCATION`; the backend started successfully, and the user confirmed real local frontend-backend integration through Vite `5173` to Spring Boot `8082`, including database-backed registration. No secret was added to source control or documentation.

### Independent Vue Frontend Update (2026-07-27)

- `frontend/` is now a Vue 3 + Vite + TypeScript application managed with pnpm. It contains route guards, session-scoped JWT storage, Axios response/error handling, a Vite `/api` proxy, Pinia stores, Element Plus controls, Lucide icons, and modular ECharts loading.
- Implemented routes are `/login`, `/register`, `/practice`, `/mistakes`, and `/progress`. The app consumes the documented authentication, model catalog/preference, question generation, scoring, mistake, and progress contracts.
- The training flow supports model selection, forced question generation, a 5,000-character answer limit, true SSE parsing through `fetch` with an `Authorization: Bearer` header, error/cancellation states, and score feedback. It never places the JWT in a URL.
- The mistake view has a real API-backed list/detail drawer and retry path; the progress view renders only the backend-provided per-tag count and average score. No fake dashboard metrics were added.
- Frontend quality verification passed locally: Prettier check, ESLint with zero warnings, 5 Vitest tests, and the Vite production build. Playwright browser checks passed for successful and failed login, expired-token logout, model selection, question generation, SSE `done` parsing, score display, mistake drawer, progress chart, empty progress state, and desktop/mobile navigation behavior. Browser-plugin support was unavailable, so the checks used local Chrome through Playwright.
- The local backend was not running during this browser QA. Mocking was restricted to the exact `/api` requests for visual and interaction verification; live login, provider requests, model switching, scoring persistence, mistake retrieval, and progress retrieval must be rechecked after starting `backend/` on port `8082`.
- Added Springdoc paths and environment switches. Local development enables `/v3/api-docs` and `/swagger-ui.html` by default; production keeps both disabled unless `OPENAPI_ENABLED=true` and `SWAGGER_UI_ENABLED=true` are explicitly supplied.
- `SecurityConfig` now permits only `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, and `/v3/api-docs/**` without JWT. All `/api/**` business routes remain protected by the existing `anyRequest().authenticated()` rule. Targeted `SecurityApiTest` verification passed: 10 tests, 0 failures, 0 errors, with Flyway disabled.
- Added `OpenApiDocumentationIntegrationTest` as a Spring MVC slice with explicit Springdoc MVC/UI configuration and mocked Controller dependencies. It verifies anonymous `/v3/api-docs` access, API title/version, the reusable HTTP Bearer `bearerAuth` scheme, Swagger UI redirection, and that an anonymous `/api/mistakes` call remains `401`. Targeted verification passed: 3 tests, 0 failures, 0 errors, with Flyway disabled and no database, Redis, or AI invocation.
- Local browser verification is complete after a backend restart: `/swagger-ui.html` opened successfully, `/v3/api-docs` returned the generated document, and a fresh JWT authorized an authenticated Swagger call successfully.
- Documented the public authentication APIs with OpenAPI tags, operation summaries, form request bodies, real response-status descriptions, and DTO field schemas. Password is marked `writeOnly`; no credentials or real tokens are embedded in the document. `OpenApiDocumentationIntegrationTest` now verifies the registration/login summaries, form media type, and password schema flag. Targeted verification passed: 3 tests, 0 failures, 0 errors, with Flyway disabled.
- Documented `AiModelController` as the protected AI model-selection API using class-level `bearerAuth`. The model catalog, effective-preference, and preference-update operations now describe their runtime-availability rules and real `400`, `401`, and `503` outcomes; model DTO schemas explain default versus current-user selection without exposing credentials. `OpenApiDocumentationIntegrationTest` now verifies protected model operations, public authentication operations, and the JSON update request contract. Targeted verification passed: 3 tests, 0 failures, 0 errors, with Flyway disabled.
- Completed the remaining Swagger/OpenAPI contract for `AiController`. The protected JSON question-generation, synchronous-scoring, mistake-book, and study-progress operations now document JWT authentication, request bodies, response types, and actual `400`, `401`, `502`, and `503` behavior where applicable. Legacy GET question and score routes are marked deprecated without changing runtime behavior.
- Documented the legacy SSE score stream as `text/event-stream`. Its contract now distinguishes ordinary `data:` deltas, `event: done` with `AiScoreResult` JSON, and `event: error` text. Bearer authentication is the formal mechanism; the deprecated URL `token` query parameter is documented only as old static-page compatibility, and the documented behavior correctly avoids a false ordinary HTTP `401` response.
- Added schemas and safe examples for `ApiResponse`, question/scoring requests, score results, mistake records, and study-progress records. `OpenApiConfig` now provides reusable typed `ApiResponse` composition schemas so Swagger retains the actual `data` shape instead of erasing generic response types.
- Expanded `OpenApiDocumentationIntegrationTest` to verify protected JSON operations, deprecated legacy operations, SSE media type/event/auth caveats, typed response references, DTO schemas, and public documentation/auth behavior. Targeted verification passed: 4 tests, 0 failures, 0 errors, with Flyway disabled. Full Maven regression also passed: 91 tests, 0 failures, 0 errors, with Flyway disabled and no database migration, Redis call, or AI call.
- Updated `README.md` with local Swagger URLs, local/production enablement rules, Swagger JWT authorization instructions, deprecated legacy endpoint status, and the SSE query-token caveat.
- Reorganized the repository into a single Git root named `AI面试助手`, with `backend/` for the Spring Boot application and `frontend/` reserved for the independent Vue 3 application. The existing Git history and uncommitted work were retained; no commit was created during the structural move. Root documentation and ignore rules now reflect the new command and build-output paths.
- The requested outer-folder rename is complete. Do not move `backend/`, `frontend/`, or `.git` again when opening the project in IDEA or VS Code.
- Structural verification ran from `backend/` after the repository move. The current Surefire reports confirm 94 tests with 0 failures and 0 errors; the outer Maven command exceeded the execution-tool wait window only after the reports were written. No Flyway migration, database operation, Redis call, or AI call was performed for this verification.
- Added `frontend/AGENTS.md` as the frontend-specific collaboration contract. It extracts applicable root rules and adds mandatory polished-minimal UX, real API-data boundaries, OpenAPI/JWT/SSE integration rules, responsive accessibility, and frontend verification requirements. The frontend remains unscaffolded.

### Current highest priority

The local configuration and frontend-backend integration verification are complete. Preserve the DeepSeek policy default and do not edit `ai_model` manually in Navicat. The independent Vue frontend in `frontend/` continues to treat OpenAPI as the source of truth and use protected JSON APIs rather than deprecated legacy routes. The next separately scoped cleanup is retirement of the legacy static pages and their compatibility-only routes; the two-user no-restart preference smoke test and stream-disconnect cancellation smoke test remain required before cloud deployment.

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- 登录态管理
- 路由守卫
- SSE 流式评分组件

## 每次开发时的推荐流程

每次继续这个项目时，优先按下面流程进行：

1. 明确本次只做一个小目标
2. 说明为什么要做这个目标
3. 列出涉及的文件
4. 只给出当前这一步的手动修改方式
5. 给出当前这一步需要的代码片段
6. 给出当前这一步的验证命令
7. 给出预期结果
8. 用户确认完成后，再进入下一步

## VS Code 开发约定

用户主要在 VS Code 中手动开发。

默认提供：

- Windows PowerShell 命令
- Maven Wrapper 命令，例如 `.\mvnw.cmd test`
- 文件路径和包名
- 手动创建类的步骤
- 修改前后的代码片段

不要默认要求用户切换到 IntelliJ IDEA。

## 默认沟通风格

- 用中文说明。
- 不直接代替用户完成代码修改。
- 不跳步骤。
- 不只讲结论，要讲为什么。
- 遇到复杂任务，先拆成 15 到 40 分钟可以完成的小任务。
- 每一步都要能验证。

## 历史优先级（已被上方最新状态取代）

配置外置化、登录注册响应统一、Service 分层抽取、SSE 传输编排、JWT 认证、`UserContext` 上下文迁移、业务接口鉴权和统一安全响应已经完成。用户当前决定优先完成数据库重构设计和本地验证，再进入 OpenAPI 与前后端分离；静态前端仅保留必要维护。

后续继续按一个小步骤推进：

1. 先设计并审查本地 `user` 与 `answer_record` 的重构 SQL，不执行删除操作。
2. 用户确认 SQL 后，在 Navicat 的本地 `interview_db` 执行重建并验证结构。
3. 再同步重命名后端持久化代码并执行全量测试；云端迁移、OpenAPI 和 Vue 迁移均延后。

当前最近的小目标是：已完成 V1 表结构的字段、类型、外键和索引审查，并已迁移到 `src/main/resources/db/migration/V1__create_core_schema.sql`；本地专用重置脚本已位于 `database/local/reset_interview_db.sql`，但尚未执行。下一步是接入 Flyway Maven 依赖和配置，仍不执行本地或云端的破坏性数据库操作。

状态修正（2026-07-25）：`AiController` 的构造器注入和演示接口清理已经完成；`AuthService` 及其单元测试已经完成，`UserService` 的单元测试、构造器注入和共享密码编码器也已完成；Mapper 冗余已经清理；面试请求 DTO 已建立；出题和普通评分接口已迁移到 POST、JSON 和参数校验，同时保留旧 GET 兼容；错题本和学习进度已改为明确的不可变响应 DTO，`Conversation` 实体与 MyBatis 聚合 `Map` 不再作为 HTTP 响应暴露；`AiController` 及其 MVC 测试切片中已经清理无用的 `AiService` 依赖；SSE 传输已迁移至 `sse/InterviewScoreSseAdapter`，`SseEmitter` 不再进入业务 Service 层；DashScope HTTP 调用已迁移至 `client/DashScopeAiClient`，`AiService` 仅保留提示词和评分结果校验；全量 37 项测试通过。用户已将当前工作重点切换到数据库重构：目标是以 `user` 和 `answer_record` 支撑现有功能，先在本地设计、审查、执行和验证，再将同一版本的 SQL 与后端同步发布到云端。`database/schema/v1_core_schema.sql` 已完成与现有 DTO、Entity 的对齐审查：ID 使用 Java `Long` 对应的有符号 `BIGINT`，用户名、题目和回答长度与请求校验一致，AI 评分结果字段为非空；尚未创建重建脚本或执行任何 SQL。用户已要求采用企业级数据库交付标准，正式迁移文件将使用 Flyway 的 `src/main/resources/db/migration/` 路径和版本命名。新版数据库重构目标与流程以本文件下方的 `Database Refactor Design And Execution Plan (2026-07-25)` 为准。

## Database Refactor Design And Execution Plan (2026-07-25)

### Goal

将 Demo 阶段的持久化模型整理为能稳定支撑当前功能的最小结构。数据库重构 SQL 必须在 IDEA 项目中维护、先在本地执行验证、再与匹配的后端版本一起发布到云端。

当前用户的 Navicat 连接指向本地 MySQL，而不是云端数据库。初始阶段只操作本地 `interview_db`，不执行任何云端数据库操作。

### Target V1 Model

```text
user
  - 用户身份和密码哈希

answer_record
  - 一名用户针对一道面试题的回答、AI 评分及建议
```

`answer_record` 用于替代语义不明确的 `conversation`。它保留现有业务所需数据：用户 ID、知识点、题目、用户回答、分数、标准答案、改进建议和创建时间。

V1 明确不创建以下表：

- `mistake_record`：错题本由 `answer_record` 中 `score < 6` 的记录查询得到。
- `study_progress`：学习进度由 `answer_record` 按 `tag` 聚合得到。
- `score_result`：评分结果当前属于一次答题记录，不单独拆表。
- `interview_question`：只有确认要做题库或题目管理时才创建。
- `ai_call_log`：只有出现明确的运维审计需求时才创建。

### Required Constraints And Indexes

- `user.username` 必须有数据库唯一约束，不能只依赖 Service 的重复查询。
- `user.password` 只能保存编码后的密码哈希。
- `answer_record.user_id` 必须通过外键关联 `user.id`。
- `answer_record.score` 必须限制在 0 到 10。
- `answer_record` 需要当前查询所需索引：`(user_id, create_time)`、`(user_id, score)`、`(user_id, tag)`。
- `user` 维护 `create_time`、`update_time`；`answer_record` 维护 `create_time`。

### Enterprise Database Delivery Standard

本项目采用企业级数据库交付规范：表结构属于应用基础设施，必须与后端代码一样被版本控制、审查、测试和发布。

#### SQL File Locations

```text
src/main/resources/db/migration/
  V1__create_core_schema.sql
  V2__add_user_ai_model_selection.sql
  V3__correct_deepseek_v4_flash_model_code.sql

database/local/
  reset_interview_db.sql
```

- `src/main/resources/db/migration/` 是生产结构迁移 SQL 的唯一正式位置，Flyway 从应用 classpath 读取这些文件。
- 迁移文件命名格式为 `V<version>__<description>.sql`，版本号和描述之间使用双下划线。
- `database/local/` 只存放明确标为本地、可破坏数据的开发重置脚本；这些脚本不得被 Flyway 自动发现或执行。
- `src/main/resources/db/migration/V1__create_core_schema.sql` 是已审查的 V1 核心结构迁移。该文件已处于最终企业级目录，即使 Flyway 依赖尚未添加，也不得在其他位置维护重复的生产结构文件。

#### Migration Rules

- 不得修改已经在测试、共享或云端环境执行过的迁移文件；必须新增下一个版本的迁移文件。
- 不得只通过 Navicat 修改云端表结构；完全相同的 SQL 必须先提交到项目。
- 表结构迁移、匹配的 Entity/Mapper/Service 修改、测试和发布属于同一个发布单元。
- 生产数据变更优先采用可兼容的扩展和迁移步骤；破坏性重置仅可用于用户明确确认的数据可丢弃的本地环境。
- 不得用 MyBatis Mapper、Java Entity 注解或应用启动代码创建、修改生产表；Mapper 只操作已存在的表，迁移 SQL 才定义表。
- SQL 执行前必须审查表名、字段类型、可空性、约束、外键、索引和回滚或恢复影响。

#### Configuration And Verification Rules

- Flyway 是 Maven 依赖并由 Spring Boot 配置，不需要单独安装桌面软件。
- 迁移 SQL 不得包含密码、Token、服务器地址；连接信息只来自环境变量。
- 本地、测试和云端使用同一顺序的迁移历史，但使用不同的数据库连接环境变量。
- 每次迁移先在本地验证结构和核心业务流程；同步代码修改后执行 `./mvnw.cmd test`。
- 云端迁移前必须备份，迁移和后端发布后必须完成冒烟测试。

### Naming And Code Alignment

表重构必须与后端持久化代码作为同一小阶段完成：

```text
conversation table       -> answer_record table
Conversation entity      -> AnswerRecord entity
ConversationMapper       -> AnswerRecordMapper
```

必须同步更新 `InterviewServiceImpl`、`StudyServiceImpl`、DTO 映射、MVC 测试和 Service 测试。Controller 的 HTTP 路径和响应契约保持不变，因为这次是持久化重构，不是 API 重构。

### Execution Stages

1. 在 IDEA 中设计并审查 SQL
   - 生产目标表结构最终放在 `src/main/resources/db/migration/`；本地重建脚本放在 `database/local/`。
   - 本地重建脚本必须明显标注为破坏性操作，禁止用于云端，也不得被 Flyway 自动发现。
   - 此阶段只创建并审查 SQL，不执行 SQL。

2. 在 Navicat 重建本地数据库
   - 用户确认 SQL 后，仅对本地连接的 `interview_db` 执行脚本。
   - 本地旧的 `user`、`conversation` 数据会按用户确认被清空。
   - 验证只存在 `user`、`answer_record`，并检查索引和外键。

3. 同步后端持久化代码
   - 重命名 Entity、Mapper 和相关 Service 实现。
   - 执行 `./mvnw.cmd test`。
   - 手动验证本地注册、登录、评分写入、错题本和学习进度。

4. 建立后续迁移规范
   - 接入 Flyway 或等价迁移工具后，每次表或字段变更都新增受版本管理的 SQL。
   - 不允许只通过 Navicat 修改云端结构而不提交 SQL 到项目。

5. 最后再处理云端发布
   - 先确认云端数据库地址与本地 Navicat 连接不同。
   - 即使数据当前不重要，也要在云端破坏性操作前做备份。
   - 执行同一版本的云端 SQL 后，立刻发布匹配的新后端版本。
   - 验证云端注册、评分、错题本和学习进度。

### Verification Checklist

```text
Local database
  - user 和 answer_record 存在
  - username 唯一约束存在
  - answer_record 外键和索引存在

Backend
  - 注册写入 user
  - 评分写入 answer_record
  - 错题本读取 score < 6 的记录
  - 学习进度按 tag 聚合
  - ./mvnw.cmd test 通过

Cloud release
  - 云端表结构与后端版本一致
  - 没有代码继续访问旧 conversation 表
  - 发布后的冒烟测试通过
```

### Guardrails

- 本地设计和验证阶段不删除或修改云端表。
- 不为尚未确认用户流程的新功能预建表。
- Navicat 只用于执行、查看和验证；项目中的 SQL 才是表结构的唯一正式记录。
- 表结构与后端持久化代码必须作为一个发布单元，不能先删表再长期运行旧版本后端。

### Database Refactor Current Status

- The reviewed V1 core schema is now stored at `src/main/resources/db/migration/V1__create_core_schema.sql`.
- The former root-level schema draft has been removed so there is one production schema source of truth.
- `database/local/reset_interview_db.sql` defines the local-only, destructive reset workflow, including removal of the legacy `mistake_view` before its dependent tables. It has been executed only against the confirmed local `interview_db`, which is now empty.
- Flyway is now added through the Spring Boot-managed `org.flywaydb:flyway-core` dependency.
- `application.properties` configures `classpath:db/migration`, validation before migration, disabled Flyway clean operations, and disabled automatic baseline. `FLYWAY_ENABLED` defaults to `false`, so no migration runs until an environment explicitly enables it.
- The local fallback JDBC URL now includes `allowPublicKeyRetrieval=true` for MySQL 8 `caching_sha2_password` authentication when local SSL is disabled. The production profile continues to require an externally supplied `MYSQL_URL` and should use TLS instead.
- The persistence-code naming refactor is complete: `Conversation` / `ConversationMapper` have been replaced with `AnswerRecord` / `AnswerRecordMapper`, and `AnswerRecord` maps to the V1 `answer_record` table.
- `InterviewServiceImpl`, `StudyServiceImpl`, `MistakeResponse`, security MVC setup, and the relevant Service tests now use the renamed persistence types. Public HTTP routes and JSON contracts are unchanged.
- A source-and-test scan confirms no active Java code still references `Conversation`, `ConversationMapper`, or the old `conversation` table.
- The first local Flyway startup attempt failed before any migration SQL ran because JDBC could not retrieve the MySQL public key. A second attempt authenticated successfully but stopped before V1 because `flyway-core` alone did not support MySQL 8.0.
- Added `org.flywaydb:flyway-mysql` with `${flyway.version}`, keeping the MySQL module aligned with Spring Boot-managed Flyway Core 8.5.13.
- Full Maven regression passed after the MySQL Flyway module was added: 37 tests, 0 failures, and 0 errors, with `FLYWAY_ENABLED=false`.
- No `flyway_schema_history` table has been created and no cloud database SQL has been executed.
- The next controlled step is to restart the local application with `FLYWAY_ENABLED=true`, then verify that V1 creates `user`, `answer_record`, and Flyway history records.

## Current Status (2026-07-28)

- Streaming scoring now uses `POST /api/question/score/stream` with a JSON body and Bearer authentication.
- Legacy static HTML pages and URL-token SSE compatibility have been removed.
- Frontend cleanup and POST SSE request coverage are complete.
- Backend OpenAPI contract verification passed. Frontend lint, 6 tests, and production build passed.
- The next verification is the full backend regression and an authenticated local POST SSE smoke test.
