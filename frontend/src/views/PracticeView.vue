<script setup lang="ts">
import { RefreshCw, Sparkles } from '@lucide/vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getKnowledgeTopics } from '@/api/knowledge-api';
import AnswerEditor from '@/components/interview/AnswerEditor.vue';
import QuestionPanel from '@/components/interview/QuestionPanel.vue';
import ScoreFeedback from '@/components/interview/ScoreFeedback.vue';
import { useInterviewStore } from '@/stores/interview';
import type { KnowledgeTopic } from '@/types/knowledge';

type PracticeMode = 'custom' | 'knowledge';

const route = useRoute();
const interviewStore = useInterviewStore();
const practiceMode = ref<PracticeMode>('custom');
const knowledgeTopics = ref<KnowledgeTopic[]>([]);
const knowledgeLoading = ref(false);
const knowledgeError = ref('');

const knowledgeOptions: Record<'frontend' | 'backend', Record<string, readonly string[]>> = {
  frontend: {
    JavaScript: ['JavaScript 基础', '异步编程', 'DOM 与事件', '浏览器原理'],
    TypeScript: ['类型系统', '泛型', '类型体操', '工程配置'],
    Vue: ['组件通信', 'Composition API', '响应式原理', 'Vue Router'],
    React: ['组件设计', 'Hooks', '状态管理', 'React 性能优化'],
  },
  backend: {
    Java: ['集合框架', '并发编程', 'JVM', 'Spring', 'MySQL', 'Redis'],
    Python: ['Python 基础', '异步编程', 'FastAPI', 'Django'],
    Go: ['Goroutine', 'Channel', 'Gin', '服务并发'],
    'C#': ['C# 基础', '.NET', 'ASP.NET Core', 'Entity Framework'],
    'Node.js': ['事件循环', 'Express', 'NestJS', 'Node.js 性能'],
    TypeScript: ['Node.js 类型开发', 'NestJS', '异步编程', '服务架构'],
  },
};
const languageOptions = {
  frontend: ['JavaScript', 'TypeScript', 'Vue', 'React'],
  backend: ['Java', 'Python', 'Go', 'C#', 'Node.js', 'TypeScript'],
} as const;
const availableTags = computed(
  () => knowledgeOptions[interviewStore.direction][interviewStore.language] ?? [],
);
const selectedKnowledgeTopic = computed(
  () => knowledgeTopics.value.find((topic) => topic.id === interviewStore.knowledgeTopicId) ?? null,
);
const sourceLabel = computed(() => {
  if (practiceMode.value === 'knowledge' && selectedKnowledgeTopic.value) {
    return `知识库专题：${selectedKnowledgeTopic.value.title}`;
  }
  return `自定义主题：${interviewStore.tag || '未选择'}`;
});

function handleDirectionChange(): void {
  interviewStore.language = languageOptions[interviewStore.direction][0];
  handleLanguageChange();
}

function handleLanguageChange(): void {
  interviewStore.tag = '';
  interviewStore.knowledgeTopicId = null;
  interviewStore.question = '';
  interviewStore.resetScore();
}

function handlePracticeModeChange(mode: PracticeMode): void {
  practiceMode.value = mode;
  interviewStore.knowledgeTopicId = null;
  interviewStore.question = '';
  interviewStore.resetScore();
}

function handleKnowledgeTopicChange(topicId: number): void {
  const topic = knowledgeTopics.value.find((item) => item.id === topicId);
  if (topic) {
    interviewStore.tag = topic.title;
  }
  interviewStore.question = '';
  interviewStore.resetScore();
}

async function loadKnowledgeTopics(): Promise<void> {
  knowledgeLoading.value = true;
  knowledgeError.value = '';
  try {
    knowledgeTopics.value = await getKnowledgeTopics(
      interviewStore.direction,
      interviewStore.language,
    );
    if (!knowledgeTopics.value.some((topic) => topic.id === interviewStore.knowledgeTopicId)) {
      interviewStore.knowledgeTopicId = null;
    }
  } catch (error) {
    knowledgeTopics.value = [];
    knowledgeError.value = error instanceof Error ? error.message : '知识库专题加载失败';
  } finally {
    knowledgeLoading.value = false;
  }
}

function applyRouteSelection(): void {
  const direction = route.query.direction;
  const language = route.query.language;
  const tag = route.query.tag;
  const topicId = Number(route.query.knowledgeTopicId);
  if (direction === 'frontend' || direction === 'backend') {
    interviewStore.direction = direction;
  }
  if (
    typeof language === 'string' &&
    languageOptions[interviewStore.direction].includes(language as never)
  ) {
    interviewStore.language = language;
  }
  if (Number.isInteger(topicId) && topicId > 0) {
    practiceMode.value = 'knowledge';
    interviewStore.knowledgeTopicId = topicId;
  }
  if (typeof tag === 'string' && tag.trim()) {
    interviewStore.tag = tag.trim().slice(0, 50);
  }
}

async function generateQuestion(): Promise<void> {
  if (practiceMode.value === 'knowledge' && !interviewStore.knowledgeTopicId) {
    interviewStore.questionError = '请先选择知识库专题';
    return;
  }
  await interviewStore.generate(true);
}

onMounted(() => {
  applyRouteSelection();
  void loadKnowledgeTopics();
});
watch(
  () => [interviewStore.direction, interviewStore.language],
  () => void loadKnowledgeTopics(),
);
watch(() => route.query, applyRouteSelection, { deep: true });
</script>

<template>
  <section class="practice-page">
    <header class="page-heading">
      <div>
        <h1>面试训练</h1>
        <p>围绕自定义主题或已有知识库，完成一题一练的技术表达训练。</p>
      </div>
      <div class="practice-controls">
        <el-select
          v-model="interviewStore.direction"
          :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
          aria-label="选择面试方向"
          @change="handleDirectionChange"
        >
          <el-option label="前端" value="frontend" />
          <el-option label="后端" value="backend" />
        </el-select>
        <el-select
          v-model="interviewStore.language"
          :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
          aria-label="选择语言或技术栈"
          @change="handleLanguageChange"
        >
          <el-option
            v-for="language in languageOptions[interviewStore.direction]"
            :key="language"
            :label="language"
            :value="language"
          />
        </el-select>
      </div>
    </header>

    <div class="practice-source" role="group" aria-label="选择出题来源">
      <el-radio-group
        class="practice-mode"
        :model-value="practiceMode"
        :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
        @update:model-value="handlePracticeModeChange"
      >
        <el-radio-button label="custom">自定义主题</el-radio-button>
        <el-radio-button label="knowledge">知识库专题</el-radio-button>
      </el-radio-group>
      <el-select
        v-if="practiceMode === 'custom'"
        v-model="interviewStore.tag"
        class="source-select"
        filterable
        allow-create
        default-first-option
        :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
        aria-label="输入或选择知识点"
        placeholder="输入知识点"
      >
        <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
      </el-select>
      <el-select
        v-else
        v-model="interviewStore.knowledgeTopicId"
        class="source-select"
        :loading="knowledgeLoading"
        :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
        aria-label="选择知识库专题"
        placeholder="选择知识库专题"
        @change="handleKnowledgeTopicChange"
      >
        <el-option
          v-for="topic in knowledgeTopics"
          :key="topic.id"
          :label="topic.title"
          :value="topic.id"
        />
      </el-select>
      <span class="source-note">{{ knowledgeError || sourceLabel }}</span>
      <el-tooltip content="生成一题新题目" placement="bottom">
        <el-button
          circle
          :icon="RefreshCw"
          :loading="interviewStore.questionLoading"
          :disabled="interviewStore.scoreStatus === 'streaming'"
          aria-label="换一题"
          @click="generateQuestion"
        />
      </el-tooltip>
      <el-button
        type="primary"
        :icon="Sparkles"
        :loading="interviewStore.questionLoading"
        :disabled="interviewStore.scoreStatus === 'streaming'"
        @click="generateQuestion"
      >
        生成题目
      </el-button>
    </div>

    <div class="practice-layout">
      <div class="practice-layout__workbench">
        <QuestionPanel
          :tag="interviewStore.tag"
          :question="interviewStore.question"
          :loading="interviewStore.questionLoading"
          :error="interviewStore.questionError"
          :source="sourceLabel"
        />
        <AnswerEditor
          v-model="interviewStore.answer"
          :disabled="!interviewStore.question || interviewStore.questionLoading"
          :scoring="interviewStore.scoreStatus === 'streaming'"
          @score="interviewStore.score"
          @cancel="interviewStore.cancelScore"
        />
      </div>
      <ScoreFeedback
        :status="interviewStore.scoreStatus"
        :result="interviewStore.scoreResult"
        :stream-text="interviewStore.streamText"
        :error="interviewStore.scoreError"
      />
    </div>
  </section>
</template>

<style scoped>
.practice-page {
  display: grid;
  gap: 22px;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}
.page-heading h1 {
  font-size: 28px;
  line-height: 1.28;
}
.page-heading p {
  margin-top: 6px;
  color: var(--ink-muted);
  font-size: 14px;
}
.practice-controls,
.practice-source {
  display: flex;
  align-items: center;
  gap: 10px;
}
.practice-controls :deep(.el-select) {
  width: 150px;
}
.practice-source {
  flex-wrap: wrap;
  min-height: 44px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border);
}
.source-select {
  width: 240px;
}

.practice-mode :deep(.el-radio-button__inner) {
  min-width: 102px;
  border-color: var(--border);
  box-shadow: none;
  color: var(--ink-muted);
  font-size: 13px;
}

.practice-mode :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: none;
  color: #ffffff;
}
.source-note {
  min-width: 180px;
  flex: 1;
  color: var(--ink-muted);
  font-size: 13px;
}
.practice-layout {
  display: grid;
  min-height: 630px;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.75fr);
  gap: 20px;
}
.practice-layout__workbench {
  display: grid;
  align-content: start;
  gap: 20px;
}
@media (max-width: 1040px) {
  .practice-layout {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 680px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .practice-controls {
    width: 100%;
  }
  .practice-controls :deep(.el-select),
  .source-select {
    width: auto;
    flex: 1;
  }
  .source-note {
    width: 100%;
    flex-basis: 100%;
  }
}
</style>
