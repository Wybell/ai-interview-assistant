<script setup lang="ts">
import { ArrowLeft, ArrowRight, Sparkles } from '@lucide/vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getKnowledgeTopics } from '@/api/knowledge-api';
import { getTechnicalTopics } from '@/api/interview-api';
import AnswerEditor from '@/components/interview/AnswerEditor.vue';
import QuestionPanel from '@/components/interview/QuestionPanel.vue';
import ScoreFeedback from '@/components/interview/ScoreFeedback.vue';
import { useInterviewStore } from '@/stores/interview';
import type { KnowledgeTopic } from '@/types/knowledge';

type PracticeMode = 'custom' | 'knowledge' | 'technical';

const route = useRoute();
const interviewStore = useInterviewStore();
const practiceMode = ref<PracticeMode>('custom');
const practiceModeOptions: ReadonlyArray<{ value: PracticeMode; label: string }> = [
  { value: 'custom', label: '自定义知识点' },
  { value: 'knowledge', label: '知识库专题' },
  { value: 'technical', label: '技术知识点' },
];
const knowledgeTopics = ref<KnowledgeTopic[]>([]);
const knowledgeLoading = ref(false);
const knowledgeError = ref('');
const technicalTopics = ref<string[]>([]);
const technicalLoading = ref(false);
const technicalError = ref('');
const languageOptions = {
  frontend: ['JavaScript', 'TypeScript', 'Vue', 'React'],
  backend: ['Java', 'Python', 'Go', 'C#', 'Node.js', 'TypeScript'],
} as const;
const selectedKnowledgeTopic = computed(
  () => knowledgeTopics.value.find((topic) => topic.id === interviewStore.knowledgeTopicId) ?? null,
);
const sourceLabel = computed(() => {
  if (practiceMode.value === 'knowledge' && selectedKnowledgeTopic.value) {
    return `知识库专题：${selectedKnowledgeTopic.value.title}`;
  }
  if (practiceMode.value === 'technical') {
    return `技术知识点：${interviewStore.tag || '未选择'}`;
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
  interviewStore.resetQuestionHistory();
}

function handlePracticeModeChange(mode: PracticeMode): void {
  practiceMode.value = mode;
  interviewStore.questionMode = mode === 'knowledge'
    ? 'KNOWLEDGE_BASE'
    : mode === 'technical'
      ? 'TECHNICAL_TOPIC'
      : 'CUSTOM_TOPIC';
  interviewStore.knowledgeTopicId = null;
  interviewStore.tag = '';
  if (mode === 'knowledge' && knowledgeTopics.value.length === 1) {
    interviewStore.knowledgeTopicId = knowledgeTopics.value[0].id;
    interviewStore.tag = knowledgeTopics.value[0].title;
  }
  if (mode === 'technical' && technicalTopics.value.length === 1) {
    interviewStore.tag = technicalTopics.value[0];
  }
  interviewStore.resetQuestionHistory();
}

function handleKnowledgeTopicChange(topicId: number): void {
  const topic = knowledgeTopics.value.find((item) => item.id === topicId);
  if (topic) {
    interviewStore.tag = topic.title;
  }
  interviewStore.resetQuestionHistory();
}

function handleTechnicalTopicChange(): void {
  interviewStore.resetQuestionHistory();
}

async function loadKnowledgeTopics(): Promise<void> {
  knowledgeLoading.value = true;
  knowledgeError.value = '';
  technicalLoading.value = true;
  technicalError.value = '';
  try {
    const [loadedKnowledgeTopics, loadedTechnicalTopics] = await Promise.all([
      getKnowledgeTopics(interviewStore.direction, interviewStore.language),
      getTechnicalTopics(interviewStore.direction, interviewStore.language),
    ]);
    knowledgeTopics.value = loadedKnowledgeTopics;
    technicalTopics.value = loadedTechnicalTopics;
    if (practiceMode.value === 'knowledge' && !knowledgeTopics.value.some((topic) => topic.id === interviewStore.knowledgeTopicId)) {
      interviewStore.knowledgeTopicId = knowledgeTopics.value.length === 1
        ? knowledgeTopics.value[0].id
        : null;
    }
    if (practiceMode.value === 'knowledge') {
      interviewStore.tag = selectedKnowledgeTopic.value?.title ?? '';
    }
    if (practiceMode.value === 'technical' && !technicalTopics.value.includes(interviewStore.tag)) {
      interviewStore.tag = technicalTopics.value.length === 1 ? technicalTopics.value[0] : '';
    }
  } catch (error) {
    knowledgeTopics.value = [];
    technicalTopics.value = [];
    const message = error instanceof Error ? error.message : '选项加载失败';
    knowledgeError.value = message;
    technicalError.value = message;
  } finally {
    knowledgeLoading.value = false;
    technicalLoading.value = false;
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
    handlePracticeModeChange('knowledge');
    interviewStore.knowledgeTopicId = topicId;
  }
  if (typeof tag === 'string' && tag.trim()) {
    interviewStore.tag = tag.trim().slice(0, 50);
  }
}

async function validateQuestionSource(): Promise<boolean> {
  if (practiceMode.value === 'knowledge' && !interviewStore.knowledgeTopicId) {
    interviewStore.questionError = '请先选择知识库专题';
    return false;
  }
  if (practiceMode.value === 'knowledge' && knowledgeTopics.value.length === 0) {
    interviewStore.questionError = '当前方向和语言暂无可用知识库专题';
    return false;
  }
  if (practiceMode.value === 'technical' && !interviewStore.tag.trim()) {
    interviewStore.questionError = '请先选择技术知识点';
    return false;
  }
  return true;
}

async function generateQuestion(): Promise<void> {
  if (!(await validateQuestionSource())) {
    return;
  }
  await interviewStore.generate(true);
}

async function generateNextQuestion(): Promise<void> {
  if (!(await validateQuestionSource())) {
    return;
  }
  await interviewStore.generate(true);
}

onMounted(() => {
  applyRouteSelection();
  interviewStore.questionMode = practiceMode.value === 'knowledge'
    ? 'KNOWLEDGE_BASE'
    : practiceMode.value === 'technical'
      ? 'TECHNICAL_TOPIC'
      : 'CUSTOM_TOPIC';
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
      <div
        class="practice-mode"
        role="radiogroup"
        aria-label="选择出题来源"
      >
        <button
          v-for="option in practiceModeOptions"
          :key="option.value"
          type="button"
          class="practice-mode__button"
          :class="{ 'is-active': practiceMode === option.value }"
          :aria-checked="practiceMode === option.value"
          role="radio"
          :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
          @click="handlePracticeModeChange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <el-input
        v-if="practiceMode === 'custom'"
        v-model="interviewStore.tag"
        class="source-select"
        :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
        aria-label="输入自定义知识点"
        placeholder="输入知识点"
      />
      <el-select
        v-else-if="practiceMode === 'knowledge'"
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
      <el-select
        v-else
        v-model="interviewStore.tag"
        class="source-select"
        :loading="technicalLoading"
        :disabled="interviewStore.questionLoading || interviewStore.scoreStatus === 'streaming'"
        aria-label="选择技术知识点"
        placeholder="选择技术知识点"
        @change="handleTechnicalTopicChange"
      >
        <el-option v-for="topic in technicalTopics" :key="topic" :label="topic" :value="topic" />
      </el-select>
      <span class="source-note">{{ knowledgeError || technicalError || sourceLabel }}</span>
      <el-button
        :icon="ArrowLeft"
        :disabled="
          interviewStore.questionLoading ||
          interviewStore.scoreStatus === 'streaming' ||
          interviewStore.questionHistoryIndex <= 0
        "
        @click="interviewStore.goToPreviousQuestion"
      >
        上一题
      </el-button>
      <el-button
        type="primary"
        :icon="Sparkles"
        :loading="interviewStore.questionLoading"
        :disabled="interviewStore.scoreStatus === 'streaming'"
        @click="generateQuestion"
      >
        生成题目
      </el-button>
      <el-button
        :icon="ArrowRight"
        :loading="interviewStore.questionLoading"
        :disabled="!interviewStore.question || interviewStore.scoreStatus === 'streaming'"
        @click="generateNextQuestion"
      >
        下一题
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

.practice-mode__button {
  min-width: 112px;
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 600;
  transition: border-color var(--transition-fast), background var(--transition-fast), color var(--transition-fast);
}

.practice-mode__button:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.practice-mode__button.is-active {
  background: var(--primary);
  border-color: var(--primary);
  color: #ffffff;
}

.practice-mode__button:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

.practice-mode__button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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
