<script setup lang="ts">
import { BarChart3, RefreshCw } from '@lucide/vue';
import { onMounted, ref } from 'vue';

import { getStudyProgress } from '@/api/study-api';
import AppEmptyState from '@/components/common/AppEmptyState.vue';
import ProgressChart from '@/components/progress/ProgressChart.vue';
import type { StudyProgress } from '@/types/interview';
import { getScoreTone } from '@/utils/format';

const items = ref<StudyProgress[]>([]);
const loading = ref(false);
const error = ref('');

async function loadProgress(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    items.value = await getStudyProgress();
  } catch (requestError) {
    error.value =
      requestError instanceof Error ? requestError.message : '学习进度加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(loadProgress);
</script>

<template>
  <section class="progress-page">
    <header class="page-heading">
      <div>
        <h1>学习进度</h1>
        <p>用每个知识点的结果，决定下一轮练习重点。</p>
      </div>
      <el-button :icon="RefreshCw" :loading="loading" @click="loadProgress">刷新</el-button>
    </header>

    <div v-if="loading" class="loading-panel" aria-live="polite">正在汇总学习进度…</div>
    <div v-else-if="error" class="error-panel" role="alert">
      <span>{{ error }}</span>
      <el-button text type="primary" @click="loadProgress">重试</el-button>
    </div>
    <template v-else-if="items.length">
      <section class="progress-panel" aria-labelledby="chart-heading">
        <div class="progress-panel__header">
          <div>
            <p class="section-label">知识点表现</p>
            <h2 id="chart-heading">平均得分</h2>
          </div>
          <BarChart3 :size="21" color="var(--primary)" aria-hidden="true" />
        </div>
        <ProgressChart :items="items" />
      </section>

      <section class="progress-panel" aria-labelledby="table-heading">
        <div class="progress-panel__header">
          <div>
            <p class="section-label">明细</p>
            <h2 id="table-heading">按知识点汇总</h2>
          </div>
        </div>
        <el-table :data="items" class="progress-table">
          <el-table-column prop="tag" label="知识点" min-width="180" />
          <el-table-column prop="totalCount" label="答题次数" min-width="140" align="center" />
          <el-table-column label="平均得分" min-width="160" align="center">
            <template #default="{ row }: { row: StudyProgress }">
              <el-tag :type="getScoreTone(row.avgScore)" effect="light">
                {{ row.avgScore.toFixed(1) }} / 10
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
    <AppEmptyState
      v-else
      class="progress-empty"
      title="暂无学习进度"
      description="完成一次评分后，系统会按知识点汇总你的练习情况。"
      :icon="BarChart3"
    />
  </section>
</template>

<style scoped>
.progress-page {
  display: grid;
  gap: 22px;
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
  max-width: 760px;
  font-size: 28px;
  line-height: 1.28;
}

.page-heading > div > p:not(.section-label) {
  margin-top: 6px;
  color: var(--ink-muted);
  font-size: 14px;
}

.progress-panel,
.loading-panel,
.error-panel,
.progress-empty {
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.progress-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 0;
}

.progress-panel h2 {
  font-size: 17px;
}

.progress-table {
  margin-top: 18px;
}

.loading-panel,
.error-panel {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-muted);
}

.error-panel {
  color: var(--danger);
}

@media (max-width: 640px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-heading h1 {
    font-size: 24px;
  }

  .progress-panel__header {
    padding: 18px 18px 0;
  }
}
</style>
