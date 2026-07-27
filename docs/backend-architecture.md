# 后端架构设计

## 1. 后端升级目标

当前后端已经能跑通 AI 出题、评分和记录查询，但业务逻辑集中在 Controller 中，安全配置也偏 Demo。升级目标是把后端整理成清晰、可维护、可继续扩展的 Spring Boot 单体应用。

目标：

- Controller 只负责接收请求和返回响应。
- Service 承载业务流程。
- Client 封装外部 AI 调用。
- DTO 隔离前端入参、出参和数据库实体。
- Security 统一处理 JWT 鉴权。
- Exception 统一处理业务错误和系统错误。
- Redis 用于缓存、限流和登录辅助。
- 配置全部外置，避免明文密钥进入代码。

## 2. 推荐目录结构

```text
src/main/java/com/example/aiinterviewassistant/
  AiInterviewAssistantApplication.java

  config/
    SecurityConfig.java
    RedisConfig.java
    WebConfig.java
    AsyncConfig.java
    DashScopeProperties.java
    JwtProperties.java

  security/
    JwtAuthenticationFilter.java
    CurrentUser.java
    UserPrincipal.java

  controller/
    AuthController.java
    InterviewController.java
    ConversationController.java
    ProgressController.java
    UserController.java

  service/
    AuthService.java
    InterviewService.java
    ConversationService.java
    ProgressService.java
    UserService.java
    RateLimitService.java

  client/
    DashScopeClient.java

  mapper/
    UserMapper.java
    ConversationMapper.java
    QuestionTagMapper.java

  entity/
    User.java
    Conversation.java
    QuestionTag.java
    UserPracticeStat.java

  dto/
    request/
      LoginRequest.java
      RegisterRequest.java
      GenerateQuestionRequest.java
      ScoreAnswerRequest.java

    response/
      LoginResponse.java
      QuestionResponse.java
      ScoreResponse.java
      ConversationResponse.java
      ProgressResponse.java

  common/
    ApiResponse.java
    PageResponse.java

  exception/
    BusinessException.java
    ErrorCode.java
    GlobalExceptionHandler.java

  util/
    JwtUtil.java
```

## 3. 分层职责

```text
Controller
  接收参数、调用 Service、返回 ApiResponse

Service
  编排业务逻辑，例如生成题目、评分、保存记录、查询错题

Client
  调用 DashScope，不把外部 API 细节泄漏给业务层

Mapper
  数据库访问

Entity
  对应数据库表

DTO
  前后端交互对象，避免直接暴露 Entity

Security
  JWT 解析、认证、当前用户上下文

Exception
  统一异常处理
```

## 4. 核心接口设计

认证：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/user/me
```

面试训练：

```text
POST /api/interview/questions
POST /api/interview/answers/score
GET  /api/interview/answers/score/stream
```

历史记录：

```text
GET    /api/conversations
GET    /api/conversations/{id}
DELETE /api/conversations/{id}
```

错题本：

```text
GET  /api/conversations/mistakes
POST /api/conversations/{id}/retry
```

学习进度：

```text
GET /api/progress/summary
GET /api/progress/tags
GET /api/progress/trend
```

## 5. 统一响应

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = "ok";
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> failure(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }
}
```

返回格式：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

## 6. 统一异常

建议新增：

```text
exception/
  BusinessException.java
  ErrorCode.java
  GlobalExceptionHandler.java
```

常见错误：

- 参数错误。
- 未登录。
- 权限不足。
- 用户名已存在。
- 用户名或密码错误。
- AI 服务调用失败。
- 请求过于频繁。
- 练习记录不存在。

## 7. DTO 设计

登录请求：

```java
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

生成题目请求：

```java
public class GenerateQuestionRequest {
    @NotBlank(message = "知识点不能为空")
    private String tag;

    private boolean refresh;
}
```

评分请求：

```java
public class ScoreAnswerRequest {
    @NotBlank(message = "题目不能为空")
    private String question;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @NotBlank(message = "知识点不能为空")
    private String tag;
}
```

评分响应：

```java
public class ScoreResponse {
    private Integer score;
    private String correctAnswer;
    private String suggestion;
    private List<String> weakPoints;
}
```

## 8. DashScopeClient 设计

职责：

```text
generateQuestion(tag)
scoreAnswer(question, answer)
streamScore(question, answer)
```

要求：

- API Key 从配置读取。
- HTTP 调用细节不进入 Controller。
- 统一处理超时、异常和响应解析。
- 尽量要求模型返回结构化 JSON。

配置示例：

```properties
dashscope.api-key=${DASHSCOPE_API_KEY}
dashscope.model=qwen-turbo
dashscope.timeout=30s
```

## 9. Spring Security + JWT

放行：

```text
/api/auth/login
/api/auth/register
/index.html
/login.html
/register.html
/static/**
```

需要登录：

```text
/api/interview/**
/api/conversations/**
/api/progress/**
/api/user/me
```

流程：

```text
用户登录
  -> 后端生成 JWT
  -> 前端保存 token
  -> 请求携带 Authorization: Bearer xxx
  -> JwtAuthenticationFilter 解析 token
  -> 写入 SecurityContext
  -> Service 获取当前用户
```

## 10. Redis 使用

题目缓存：

```text
question:{userId}:{tag}
```

AI 限流：

```text
rate:ai:{userId}:{minute}
```

JWT 黑名单：

```text
jwt:blacklist:{tokenId}
```

建议先实现 AI 限流和题目缓存，JWT 黑名单可以后置。

## 11. SSE 流式评分

后端使用：

```java
SseEmitter
```

注意点：

- 不要每个请求都临时创建线程池。
- 使用 Spring 管理的 `ThreadPoolTaskExecutor`。
- 设置超时时间。
- 监听完成、超时和异常回调。
- 发送 `done` 事件表示结束。
- 异常时发送 `error` 事件。

## 12. 配置外置

不要在代码或配置文件中写死真实密钥。

```properties
spring.datasource.url=${MYSQL_URL}
spring.datasource.username=${MYSQL_USERNAME}
spring.datasource.password=${MYSQL_PASSWORD}
dashscope.api-key=${DASHSCOPE_API_KEY}
jwt.secret=${JWT_SECRET}
```

当前代码里的 DashScope Key、JWT Secret、数据库密码都应该迁移到环境变量。

## 13. 测试规划

优先补这些测试：

```text
AuthServiceTest
UserServiceTest
InterviewServiceTest
DashScopeClientTest
ConversationServiceTest
AuthControllerTest
InterviewControllerTest
```

测试策略：

- Service 用 JUnit 5 + Mockito。
- Controller 用 `@WebMvcTest` + MockMvc。
- Mapper 后续可以用 Testcontainers 或测试数据库。

## 14. 后端开发顺序

1. 密钥外置。
2. 新增 `ApiResponse`。
3. 新增 `BusinessException` 和 `GlobalExceptionHandler`。
4. 新增请求和响应 DTO。
5. 拆出 `DashScopeClient`。
6. 拆出 `InterviewService`。
7. 拆出 `ConversationService`。
8. GET 接口改 POST JSON。
9. 增加 JWT Filter。
10. 增加 Redis 限流。
