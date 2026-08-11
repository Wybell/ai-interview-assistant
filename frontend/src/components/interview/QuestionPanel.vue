<script setup lang="ts">
import { AlertCircle, FileQuestion, LoaderCircle } from '@lucide/vue';

import AppEmptyState from '@/components/common/AppEmptyState.vue';

defineProps<{
  tag: string;
  source: string;
  question: string;
  loading: boolean;
  error: string;
}>();
</script>

<template>
  <section class="question-panel" aria-labelledby="question-heading">
    <div class="question-panel__header">
      <div>
        <p class="section-label">当前题目</p>
        <h2 id="question-heading">{{ tag || '未选择知识点' }}</h2>
        <p class="question-panel__source">{{ source }}</p>
      </div>
      <FileQuestion :size="22" stroke-width="1.7" aria-hidden="true" />
    </div>

    <div v-if="loading" class="question-panel__loading" aria-live="polite">
      <LoaderCircle class="spin" :size="20" />
      正在生成题目
    </div>
    <div v-else-if="error" class="question-panel__error" role="alert">
      <AlertCircle :size="19" />
      <span>{{ error }}</span>
    </div>
    <p v-else-if="question" class="question-panel__content">{{ question }}</p>
    <AppEmptyState
      v-else
      title="还没有题目"
      description="选择主题后生成第一道练习题。"
      :icon="FileQuestion"
    />
  </section>
</template>

<style scoped>
.question-panel {
  min-height: 224px;
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}
.question-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  color: var(--primary);
}
.section-label {
  margin-bottom: 3px;
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 700;
}
.question-panel h2 {
  color: var(--ink-strong);
  font-size: 19px;
  line-height: 1.35;
}
.question-panel__source {
  margin-top: 5px;
  color: var(--ink-muted);
  font-size: 13px;
}
.question-panel__content {
  margin-top: 22px;
  color: var(--ink);
  font-size: 17px;
  line-height: 1.9;
  white-space: pre-wrap;
}
.question-panel__loading,
.question-panel__error {
  display: flex;
  min-height: 126px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-muted);
}
.question-panel__error {
  color: var(--danger);
}
.spin {
  animation: spin 800ms linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
