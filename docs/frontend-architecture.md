# 前端架构设计

## 1. 技术选型

推荐使用独立前端项目：

```text
frontend/
```

技术栈：

```text
Vue 3
Vite
TypeScript
Vue Router
Pinia
Axios
Element Plus
ECharts
EventSource / SSE
```

选择 Vue 3 的原因：上手快、适合中后台和工具类项目、和当前项目升级节奏匹配。

## 2. 目录结构

```text
frontend/
  index.html
  package.json
  vite.config.ts
  tsconfig.json
  src/
    main.ts
    App.vue

    api/
      request.ts
      authApi.ts
      interviewApi.ts
      conversationApi.ts
      progressApi.ts

    router/
      index.ts

    stores/
      authStore.ts
      interviewStore.ts

    views/
      LoginView.vue
      RegisterView.vue
      DashboardView.vue
      InterviewPracticeView.vue
      MistakeBookView.vue
      ProgressView.vue
      HistoryView.vue
      ProfileView.vue

    components/
      layout/
        AppLayout.vue
        Sidebar.vue
        Topbar.vue

      interview/
        TagSelector.vue
        QuestionPanel.vue
        AnswerEditor.vue
        ScoreResult.vue
        StreamScorePanel.vue

      common/
        BaseButton.vue
        BaseDialog.vue
        LoadingState.vue
        EmptyState.vue

    types/
      api.ts
      auth.ts
      interview.ts
      progress.ts

    utils/
      token.ts
      sse.ts
      format.ts
```

## 3. 页面规划

### 登录页

路由：

```text
/login
```

功能：

- 用户名和密码登录。
- 登录成功后保存 JWT。
- 自动跳转到首页仪表盘。
- 登录失败展示错误提示。

### 注册页

路由：

```text
/register
```

功能：

- 用户名、密码、确认密码。
- 前端做基础校验。
- 注册成功后跳转登录页。

### 首页仪表盘

路由：

```text
/dashboard
```

展示：

- 今日练习次数。
- 总练习次数。
- 平均分。
- 错题数量。
- 最近练习记录。
- 薄弱知识点 Top 5。

### AI 面试训练页

路由：

```text
/interview
```

这是核心页面。

页面区域：

```text
顶部：知识点选择、刷新题目、开始训练
左侧：当前题目、历史题目
中间：答案编辑区
右侧：评分结果、参考答案、改进建议
底部：提交答案、下一题、加入错题本
```

核心状态：

```text
currentTag
currentQuestion
answer
scoreResult
streamingText
loading
```

### 错题本页

路由：

```text
/mistakes
```

功能：

- 展示低分题目。
- 按知识点筛选。
- 查看原答案、参考答案和建议。
- 重新练习。
- 标记掌握。

### 学习进度页

路由：

```text
/progress
```

展示：

- 各知识点练习次数。
- 各知识点平均分。
- 最近 7 天练习趋势。
- 薄弱项排行。

### 历史记录页

路由：

```text
/history
```

功能：

- 查看所有练习记录。
- 按知识点、分数、时间筛选。
- 查看单次练习详情。

## 4. API 封装

统一封装 Axios：

```ts
import axios from 'axios'
import { getToken, removeToken } from '@/utils/token'

export const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

统一响应类型：

```ts
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}
```

## 5. 状态管理

### authStore

负责：

- token。
- 当前用户。
- 登录。
- 退出登录。
- 判断是否已登录。

```ts
interface AuthState {
  token: string | null
  user: {
    id: number
    username: string
    nickname?: string
  } | null
}
```

### interviewStore

负责：

- 当前知识点。
- 当前题目。
- 当前答案。
- 当前评分结果。
- 流式输出文本。
- loading 状态。

```ts
interface InterviewState {
  currentTag: string
  currentQuestion: string
  answer: string
  streamingText: string
  scoreResult: ScoreResult | null
  loading: boolean
}
```

## 6. 路由设计

```text
/login
/register
/dashboard
/interview
/mistakes
/progress
/history
/profile
```

需要登录才能访问：

```text
/dashboard
/interview
/mistakes
/progress
/history
/profile
```

路由守卫：

```ts
router.beforeEach((to) => {
  const token = getToken()
  if (to.meta.requiresAuth && !token) {
    return '/login'
  }
})
```

## 7. SSE 流式输出

前端使用 `EventSource` 接收后端流式评分。

```ts
export function createScoreStream(
  url: string,
  onMessage: (text: string) => void,
  onDone: () => void
) {
  const source = new EventSource(url)

  source.onmessage = (event) => {
    onMessage(event.data)
  }

  source.addEventListener('done', () => {
    source.close()
    onDone()
  })

  source.onerror = () => {
    source.close()
  }

  return source
}
```

注意：原生 `EventSource` 不方便设置 `Authorization` 请求头。正式方案建议使用 Cookie 鉴权，或者使用 `fetch` 读取 `text/event-stream`。开发阶段可以先用 query token，但生产环境不推荐。

## 8. 前端开发顺序

1. 搭建 Vue 3 + Vite + TypeScript 项目。
2. 配置路由和布局。
3. 封装 Axios 请求。
4. 实现登录和注册。
5. 实现面试训练页。
6. 实现错题本。
7. 实现学习进度图表。
8. 接入 SSE 流式评分。
