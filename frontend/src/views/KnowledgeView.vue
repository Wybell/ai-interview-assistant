<script setup lang="ts">
import { BookOpen, Bookmark, ChevronRight, LoaderCircle } from '@lucide/vue';
import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { getKnowledgeTopic, getKnowledgeTopics } from '@/api/knowledge-api';
import type { KnowledgeTopic } from '@/types/knowledge';

const direction = ref<'frontend' | 'backend'>('backend');
const language = ref('Java');
const topics = ref<KnowledgeTopic[]>([]);
const selectedTopic = ref<KnowledgeTopic | null>(null);
const loading = ref(false);
const error = ref('');
const contentRef = ref<HTMLElement | null>(null);
const markedParagraph = ref<number | null>(null);
const MARKS_KEY = 'ai-interview-assistant.knowledge-marks';
const languageOptions = { frontend: ['JavaScript', 'TypeScript', 'Vue', 'React'], backend: ['Java', 'Python', 'Go', 'C#', 'Node.js', 'TypeScript'] } as const;
const currentLanguages = computed(() => languageOptions[direction.value]);
const paragraphs = computed(() => selectedTopic.value?.summary.split(/\r?\n+/).map((item) => item.trim()).filter(Boolean) ?? []);

function readMarks(): Record<string, number> {
  try { return JSON.parse(localStorage.getItem(MARKS_KEY) ?? '{}') as Record<string, number>; } catch { return {}; }
}

function restoreMark(topicId: number): void {
  const mark = readMarks()[String(topicId)];
  markedParagraph.value = typeof mark === 'number' ? mark : null;
}

function toggleMark(index: number): void {
  if (!selectedTopic.value) return;
  const marks = readMarks();
  if (markedParagraph.value === index) { delete marks[String(selectedTopic.value.id)]; markedParagraph.value = null; }
  else { marks[String(selectedTopic.value.id)] = index; markedParagraph.value = index; }
  localStorage.setItem(MARKS_KEY, JSON.stringify(marks));
}

async function scrollToMark(): Promise<void> {
  await nextTick();
  if (markedParagraph.value === null || !contentRef.value) return;
  contentRef.value.querySelector(`[data-paragraph-index="${markedParagraph.value}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function resetLanguage(): void {
  language.value = currentLanguages.value[0];
}

async function loadTopics(): Promise<void> {
  loading.value = true;
  error.value = '';
  selectedTopic.value = null;
  try {
    topics.value = await getKnowledgeTopics(direction.value, language.value);
  } catch (requestError) {
    error.value = requestError instanceof Error ? requestError.message : '知识专题加载失败';
    topics.value = [];
  } finally {
    loading.value = false;
  }
}

async function selectTopic(topic: KnowledgeTopic): Promise<void> {
  try {
    selectedTopic.value = await getKnowledgeTopic(topic.id);
    restoreMark(topic.id);
    await scrollToMark();
  } catch (requestError) {
    error.value = requestError instanceof Error ? requestError.message : '知识详情加载失败';
  }
}

watch(direction, resetLanguage);
watch([direction, language], loadTopics);
onMounted(loadTopics);
</script>

<template>
  <section class="knowledge-page">
    <header class="page-heading">
      <div><p class="eyebrow">系统化复习</p><h1>知识库</h1><p>按方向和技术栈查看核心概念、面试要点与参考解释。</p></div>
      <div class="knowledge-filters">
        <el-select v-model="direction" aria-label="选择方向"><el-option label="前端" value="frontend" /><el-option label="后端" value="backend" /></el-select>
        <el-select v-model="language" aria-label="选择语言或技术栈"><el-option v-for="item in currentLanguages" :key="item" :label="item" :value="item" /></el-select>
      </div>
    </header>
    <div v-if="error" class="state-message state-message--error">{{ error }}</div>
    <div v-else-if="loading" class="state-message"><LoaderCircle class="spin" :size="20" /> 正在加载知识专题</div>
    <div v-else class="knowledge-layout">
      <aside class="topic-list" aria-label="知识专题列表">
        <div class="topic-list__heading"><span>{{ language }}专题</span><small>{{ topics.length }} 个专题</small></div>
        <button v-for="topic in topics" :key="topic.id" type="button" class="topic-item" :class="{ 'topic-item--active': selectedTopic?.id === topic.id }" @click="selectTopic(topic)">
          <span><strong>{{ topic.title }}</strong><small>{{ topic.category }} · {{ topic.questions.length }} 个问题</small></span><ChevronRight :size="17" />
        </button>
        <div v-if="!topics.length" class="topic-list__empty">暂无该方向的专题内容</div>
      </aside>
      <article v-if="selectedTopic" class="topic-detail">
        <div class="topic-detail__icon"><BookOpen :size="22" /></div><p class="eyebrow">{{ selectedTopic.category }} · {{ selectedTopic.language }}</p><h2>{{ selectedTopic.title }}</h2><div class="reading-toolbar"><span>{{ markedParagraph === null ? '尚未设置阅读标记' : `已标记第 ${markedParagraph + 1} 段` }}</span><button type="button" class="mark-button" :class="{ 'mark-button--active': markedParagraph !== null }" @click="toggleMark(markedParagraph ?? 0)"><Bookmark :size="16" />{{ markedParagraph === null ? '标记当前位置' : '清除标记' }}</button></div><div ref="contentRef" class="summary"><p v-for="(paragraph, index) in paragraphs" :key="`${selectedTopic.id}-${index}`" :data-paragraph-index="index" class="document-paragraph" :class="{ 'document-paragraph--marked': markedParagraph === index }" @click="toggleMark(index)">{{ paragraph }}</p></div>
        <section><h3>核心要点</h3><ul><li v-for="point in selectedTopic.keyPoints" :key="point">{{ point }}</li></ul></section>
        <section v-for="item in selectedTopic.questions" :key="item.question" class="question-block"><h3>常见问题</h3><h4>{{ item.question }}</h4><p>{{ item.answer }}</p></section>
      </article>
      <article v-else class="topic-detail topic-detail--empty"><BookOpen :size="30" /><h2>选择一个知识专题</h2><p>从左侧选择专题，查看概念解释和面试要点。</p></article>
    </div>
  </section>
</template>

<style scoped>
.knowledge-page { display: grid; gap: 26px; }.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }.page-heading h1 { margin: 3px 0 6px; font-size: 28px; }.page-heading p { margin: 0; color: var(--ink-muted); font-size: 14px; }.eyebrow { color: var(--primary) !important; font-size: 12px !important; font-weight: 750; }.knowledge-filters { display: flex; gap: 10px; }.knowledge-filters :deep(.el-select) { width: 150px; }.knowledge-layout { display: grid; grid-template-columns: 310px minmax(0, 1fr); gap: 20px; align-items: start; }.topic-list, .topic-detail { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-md); }.topic-list { padding: 12px; }.topic-list__heading { display: flex; justify-content: space-between; padding: 8px 10px 12px; font-size: 13px; font-weight: 750; }.topic-list__heading small, .topic-item small { display: block; color: var(--ink-muted); font-size: 12px; font-weight: 500; }.topic-item { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 12px; padding: 13px 10px; background: transparent; border: 0; border-radius: var(--radius-sm); color: var(--ink-muted); text-align: left; cursor: pointer; }.topic-item:hover, .topic-item--active { background: var(--primary-subtle); color: var(--primary); }.topic-item strong { display: block; margin-bottom: 4px; color: var(--ink-strong); font-size: 14px; }.topic-item--active strong { color: var(--primary); }.topic-detail { min-height: 500px; padding: 30px 34px; }.topic-detail__icon { display: grid; width: 42px; height: 42px; place-items: center; margin-bottom: 20px; background: var(--primary-subtle); border-radius: var(--radius-sm); color: var(--primary); }.topic-detail h2 { margin: 6px 0 12px; color: var(--ink-strong); font-size: 26px; }.summary { max-width: 720px; line-height: 1.75; }.topic-detail section { margin-top: 28px; }.topic-detail h3 { margin-bottom: 10px; color: var(--ink-strong); font-size: 16px; }.topic-detail h4 { margin: 0 0 8px; color: var(--ink-strong); font-size: 15px; }.topic-detail li, .question-block p { color: var(--ink-muted); line-height: 1.8; }.question-block { padding-top: 20px; border-top: 1px solid var(--border); }.topic-detail--empty { display: grid; min-height: 500px; place-content: center; justify-items: center; color: var(--ink-muted); text-align: center; }.topic-detail--empty h2 { margin: 12px 0 4px; font-size: 20px; }.state-message { display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 180px; color: var(--ink-muted); }.state-message--error { color: var(--danger); }.spin { animation: spin 1s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } } @media (max-width: 760px) { .page-heading { align-items: flex-start; flex-direction: column; }.knowledge-filters { width: 100%; }.knowledge-filters :deep(.el-select) { width: auto; flex: 1; }.knowledge-layout { grid-template-columns: 1fr; }.topic-detail { padding: 24px 20px; } }
/* Keep paragraph breaks extracted from Word documents visible in the reader. */
.summary { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.9; }
.reading-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 20px 0 8px; color: var(--ink-muted); font-size: 13px; }
.mark-button { display: inline-flex; align-items: center; gap: 6px; padding: 8px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface); color: var(--ink-muted); cursor: pointer; }
.mark-button:hover, .mark-button--active { border-color: var(--primary); background: var(--primary-subtle); color: var(--primary); }
.document-paragraph { margin: 0; padding: 10px 12px; border-left: 3px solid transparent; cursor: pointer; }
.document-paragraph:hover { background: var(--surface-hover); }
.document-paragraph--marked { border-left-color: var(--primary); background: var(--primary-subtle); }
</style>
