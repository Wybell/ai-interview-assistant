<script setup lang="ts">
import { BookOpen, ChevronRight, FileWarning, RefreshCw, RotateCcw } from '@lucide/vue';
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { getMistakes } from '@/api/study-api';
import AppEmptyState from '@/components/common/AppEmptyState.vue';
import type { MistakeRecord } from '@/types/interview';
import { formatDateTime, getScoreTone } from '@/utils/format';

const router = useRouter();
const items = ref<MistakeRecord[]>([]);
const selectedMistake = ref<MistakeRecord | null>(null);
const loading = ref(false);
const error = ref('');
const drawerOpen = computed({
  get: () => selectedMistake.value !== null,
  set: (open: boolean) => {
    if (!open) {
      selectedMistake.value = null;
    }
  },
});

async function loadMistakes(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    items.value = await getMistakes();
  } catch (requestError) {
    error.value =
      requestError instanceof Error ? requestError.message : '错题本加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function openMistake(record: MistakeRecord): void {
  selectedMistake.value = record;
}

async function practiceAgain(): Promise<void> {
  if (!selectedMistake.value) {
    return;
  }
  await router.push({ name: 'practice', query: { tag: selectedMistake.value.tag } });
  ElMessage.success('已带入知识点，可生成新的练习题');
}

onMounted(loadMistakes);
</script>

<template>
  <section class="mistakes-page">
    <header class="page-heading">
      <div>
        <h1>错题本</h1>
        <p>把低分回答留给下一次更好的表达。</p>
      </div>
      <el-button :icon="RefreshCw" :loading="loading" @click="loadMistakes">刷新</el-button>
    </header>

    <section class="mistakes-table" aria-labelledby="mistakes-heading">
      <div class="mistakes-table__header">
        <div>
          <p class="section-label">待复盘记录</p>
          <h2 id="mistakes-heading">评分低于 6 分的回答</h2>
        </div>
        <span v-if="items.length" class="record-count">{{ items.length }} 条</span>
      </div>

      <div v-if="loading" class="table-loading" aria-live="polite">正在读取错题本…</div>
      <div v-else-if="error" class="table-error" role="alert">
        <FileWarning :size="21" />
        <span>{{ error }}</span>
        <el-button text type="primary" @click="loadMistakes">重试</el-button>
      </div>
      <AppEmptyState
        v-else-if="items.length === 0"
        title="还没有错题记录"
        description="完成评分后，低分回答会自动出现在这里。"
        :icon="BookOpen"
      />
      <el-table v-else :data="items" class="mistakes-table__data" @row-click="openMistake">
        <el-table-column prop="tag" label="知识点" min-width="130" />
        <el-table-column label="题目" min-width="340">
          <template #default="{ row }: { row: MistakeRecord }">
            <p class="question-cell">{{ row.question }}</p>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="108" align="center">
          <template #default="{ row }: { row: MistakeRecord }">
            <el-tag :type="getScoreTone(row.score)" effect="light">{{ row.score }} / 10</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="训练时间" width="138">
          <template #default="{ row }: { row: MistakeRecord }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="" width="54" align="right">
          <template #default>
            <ChevronRight :size="18" class="row-chevron" />
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="drawerOpen" direction="rtl" size="560px" :with-header="false">
      <article v-if="selectedMistake" class="mistake-detail">
        <header class="mistake-detail__header">
          <div>
            <p class="section-label">{{ selectedMistake.tag }}</p>
            <h2>训练复盘</h2>
          </div>
          <el-tag :type="getScoreTone(selectedMistake.score)" effect="light">
            {{ selectedMistake.score }} / 10
          </el-tag>
        </header>

        <section>
          <h3>面试题目</h3>
          <p>{{ selectedMistake.question }}</p>
        </section>
        <section>
          <h3>你的回答</h3>
          <p>{{ selectedMistake.userAnswer }}</p>
        </section>
        <section>
          <h3>改进建议</h3>
          <p>{{ selectedMistake.suggestion }}</p>
        </section>
        <section>
          <h3>参考答案</h3>
          <p>{{ selectedMistake.correctAnswer }}</p>
        </section>

        <el-button type="primary" :icon="RotateCcw" @click="practiceAgain"
          >围绕此知识点重新练习</el-button
        >
      </article>
    </el-drawer>
  </section>
</template>

<style scoped>
.mistakes-page {
  display: grid;
  gap: 26px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.section-label {
  margin-bottom: 5px;
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.page-heading h1 {
  font-size: 28px;
  line-height: 1.28;
}

.page-heading > div > p:not(.section-label) {
  margin-top: 6px;
  color: var(--ink-muted);
  font-size: 14px;
}

.mistakes-table {
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.mistakes-table__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
  border-bottom: 1px solid var(--border);
}

.mistakes-table h2 {
  font-size: 17px;
}

.record-count {
  color: var(--ink-muted);
  font-size: 13px;
}

.question-cell {
  display: -webkit-box;
  overflow: hidden;
  color: var(--ink);
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.mistakes-table__data :deep(.el-table__row) {
  cursor: pointer;
}

.mistakes-table__data :deep(.el-table__row:hover > td) {
  background: var(--surface-hover) !important;
}

.row-chevron {
  color: var(--ink-muted);
}

.table-loading,
.table-error {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-muted);
}

.table-error {
  color: var(--danger);
}

.mistake-detail {
  display: grid;
  gap: 26px;
  padding: 28px;
}

.mistake-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.mistake-detail h2 {
  font-size: 24px;
}

.mistake-detail h3 {
  margin-bottom: 8px;
  color: var(--ink-strong);
  font-size: 14px;
}

.mistake-detail section p {
  color: var(--ink);
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 640px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-heading h1 {
    font-size: 24px;
  }

  .mistakes-table__header {
    padding: 18px;
  }
}
</style>
