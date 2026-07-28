<script setup lang="ts">
import { RefreshCw, Sparkles } from '@lucide/vue';
import { computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

import AnswerEditor from '@/components/interview/AnswerEditor.vue';
import QuestionPanel from '@/components/interview/QuestionPanel.vue';
import ScoreFeedback from '@/components/interview/ScoreFeedback.vue';
import { useInterviewStore } from '@/stores/interview';

const route = useRoute();
const interviewStore = useInterviewStore();
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
const availableTags = computed(() => knowledgeOptions[interviewStore.direction][interviewStore.language] ?? []);

function handleDirectionChange(): void {
  interviewStore.language = languageOptions[interviewStore.direction][0];
  handleLanguageChange();
}

function handleLanguageChange(): void {
  interviewStore.tag = '';
  interviewStore.question = '';
  interviewStore.resetScore();
}

function applyTagFromRoute(): void {
  const tag = route.query.tag;
  if (typeof tag === 'string' && tag.trim()) {
    interviewStore.tag = tag.trim().slice(0, 50);
  }
}

async function generateQuestion(): Promise<void> {
  await interviewStore.generate(true);
}

onMounted(applyTagFromRoute);
watch(() => route.query.tag, applyTagFromRoute);
</script>

<template>
  <section class="practice-page">
    <header class="page-heading">
      <div>
        <h1>面试训练</h1>
        <p>把答案说清楚，也把知识点想透彻。</p>
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
        <el-select
          v-model="interviewStore.tag"
          filterable
          allow-create
          default-first-option
          :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
          aria-label="选择知识点"
          placeholder="输入知识点"
        >
          <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
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
    </header>

    <div class="practice-layout">
      <div class="practice-layout__workbench">
        <QuestionPanel
          :tag="interviewStore.tag"
          :question="interviewStore.question"
          :loading="interviewStore.questionLoading"
          :error="interviewStore.questionError"
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
  gap: 26px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.page-heading h1 {
  max-width: 680px;
  font-size: 28px;
  line-height: 1.28;
}

.page-heading > div > p {
  margin-top: 6px;
  color: var(--ink-muted);
  font-size: 14px;
}

.practice-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.practice-controls :deep(.el-select) {
  width: 150px;
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

  .page-heading h1 {
    font-size: 24px;
  }

  .practice-controls {
    width: 100%;
  }

  .practice-controls :deep(.el-select) {
    width: auto;
    flex: 1;
  }
}
</style>
