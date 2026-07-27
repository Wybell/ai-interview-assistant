<script setup lang="ts">
import { AlertCircle, BookOpen, CheckCircle2, Lightbulb, LoaderCircle } from '@lucide/vue';
import { computed, ref, watch } from 'vue';

import AppEmptyState from '@/components/common/AppEmptyState.vue';
import ScoreGauge from '@/components/common/ScoreGauge.vue';
import { getScoreLabel } from '@/utils/format';
import type { AiScoreResult, ScoreStatus } from '@/types/interview';

const props = defineProps<{
  status: ScoreStatus;
  result: AiScoreResult | null;
  streamText: string;
  error: string;
}>();

const activeTab = ref<'suggestion' | 'answer'>('suggestion');
const isStreaming = computed(() => props.status === 'streaming');
const isComplete = computed(() => props.status === 'complete' && props.result);

watch(
  () => props.result,
  () => {
    activeTab.value = 'suggestion';
  },
);
</script>

<template>
  <aside class="score-feedback" aria-labelledby="feedback-heading">
    <div class="score-feedback__header">
      <div>
        <p class="section-label">评分反馈</p>
        <h2 id="feedback-heading">实时评估与复盘</h2>
      </div>
      <LoaderCircle v-if="isStreaming" class="spin" :size="20" aria-label="正在评分" />
    </div>

    <div v-if="isStreaming" class="stream-state" aria-live="polite">
      <div class="stream-state__marker"><span /><span /><span /></div>
      <p>{{ streamText || '正在分析回答结构与关键知识点…' }}</p>
    </div>

    <div v-else-if="isComplete && result" class="completed-state">
      <div class="completed-state__summary">
        <ScoreGauge :score="result.score" />
        <div>
          <p class="completed-state__label">{{ getScoreLabel(result.score) }}</p>
          <p class="completed-state__hint">本次结果已保存，可在错题本和学习进度中继续复盘。</p>
        </div>
      </div>

      <div class="feedback-tabs" role="tablist" aria-label="评分内容">
        <button
          type="button"
          :class="{ 'feedback-tabs__button--active': activeTab === 'suggestion' }"
          role="tab"
          :aria-selected="activeTab === 'suggestion'"
          @click="activeTab = 'suggestion'"
        >
          <Lightbulb :size="16" />
          改进建议
        </button>
        <button
          type="button"
          :class="{ 'feedback-tabs__button--active': activeTab === 'answer' }"
          role="tab"
          :aria-selected="activeTab === 'answer'"
          @click="activeTab = 'answer'"
        >
          <BookOpen :size="16" />
          参考答案
        </button>
      </div>

      <div class="feedback-content" role="tabpanel">
        <p v-if="activeTab === 'suggestion'">{{ result.suggestion }}</p>
        <p v-else>{{ result.correctAnswer }}</p>
      </div>
    </div>

    <div v-else-if="status === 'error' || status === 'cancelled'" class="error-state" role="alert">
      <AlertCircle :size="22" />
      <div>
        <strong>{{ status === 'cancelled' ? '评分已取消' : '评分未完成' }}</strong>
        <p>{{ error || '请检查网络或模型配置后重试。' }}</p>
      </div>
    </div>

    <AppEmptyState
      v-else
      title="等待评分结果"
      description="提交回答后，这里会展示实时反馈、分数和参考答案。"
      :icon="CheckCircle2"
    />
  </aside>
</template>

<style scoped>
.score-feedback {
  display: grid;
  min-height: 100%;
  align-content: start;
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.score-feedback__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-label {
  margin-bottom: 3px;
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 700;
}

.score-feedback h2 {
  font-size: 17px;
  line-height: 1.45;
}

.spin {
  color: var(--primary);
  animation: spin 800ms linear infinite;
}

.stream-state {
  display: grid;
  gap: 14px;
  margin-top: 34px;
}

.stream-state__marker {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stream-state__marker span {
  width: 7px;
  height: 7px;
  background: var(--primary);
  border-radius: 50%;
  animation: pulse 1s infinite ease-in-out;
}

.stream-state__marker span:nth-child(2) {
  animation-delay: 120ms;
}

.stream-state__marker span:nth-child(3) {
  animation-delay: 240ms;
}

.stream-state p,
.feedback-content p,
.error-state p {
  color: var(--ink);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.completed-state {
  display: grid;
  gap: 22px;
  margin-top: 24px;
}

.completed-state__summary {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--border);
}

.completed-state__label {
  margin-bottom: 4px;
  color: var(--ink-strong);
  font-size: 16px;
  font-weight: 800;
}

.completed-state__hint {
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.65;
}

.feedback-tabs {
  display: flex;
  gap: 16px;
  border-bottom: 1px solid var(--border);
}

.feedback-tabs__button {
  display: flex;
  height: 36px;
  align-items: center;
  gap: 7px;
  padding: 0 0 8px;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 700;
}

.feedback-tabs__button--active {
  border-bottom-color: var(--primary);
  color: var(--primary);
}

.feedback-content {
  min-height: 160px;
}

.error-state {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  padding: 14px;
  background: var(--danger-subtle);
  border: 1px solid #f4c7d0;
  border-radius: var(--radius-sm);
  color: var(--danger);
}

.error-state strong {
  display: block;
  margin-bottom: 3px;
  font-size: 14px;
}

.error-state p {
  color: var(--danger);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 560px) {
  .score-feedback {
    padding: 18px;
  }
}
</style>
