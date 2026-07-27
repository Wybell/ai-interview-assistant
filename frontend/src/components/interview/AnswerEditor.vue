<script setup lang="ts">
import { CircleStop, Send } from '@lucide/vue';

const props = defineProps<{
  modelValue: string;
  disabled: boolean;
  scoring: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
  score: [];
  cancel: [];
}>();

function updateAnswer(value: string): void {
  emit('update:modelValue', value);
}
</script>

<template>
  <section class="answer-editor" aria-labelledby="answer-heading">
    <div class="answer-editor__header">
      <div>
        <p class="section-label">我的回答</p>
        <h2 id="answer-heading">按面试表达方式组织你的思路</h2>
      </div>
    </div>

    <el-input
      :model-value="props.modelValue"
      type="textarea"
      :rows="10"
      :maxlength="5000"
      :disabled="props.disabled || props.scoring"
      resize="none"
      show-word-limit
      placeholder="从核心流程、关键条件和边界情况开始回答。"
      @update:model-value="updateAnswer"
    />

    <div class="answer-editor__footer">
      <span>回答会在评分完成后保存到训练记录。</span>
      <el-button v-if="scoring" type="danger" plain :icon="CircleStop" @click="emit('cancel')">
        取消评分
      </el-button>
      <el-button v-else type="primary" :icon="Send" :disabled="disabled" @click="emit('score')">
        开始评分
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.answer-editor {
  display: grid;
  gap: 18px;
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.answer-editor__header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.section-label {
  margin-bottom: 3px;
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 700;
}

.answer-editor h2 {
  font-size: 17px;
  line-height: 1.45;
}

.answer-editor :deep(.el-textarea__inner) {
  min-height: 240px !important;
  padding: 14px;
  color: var(--ink);
  line-height: 1.75;
}

.answer-editor__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--ink-muted);
  font-size: 13px;
}

@media (max-width: 560px) {
  .answer-editor {
    padding: 18px;
  }

  .answer-editor__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .answer-editor__footer :deep(.el-button) {
    width: 100%;
  }
}
</style>
