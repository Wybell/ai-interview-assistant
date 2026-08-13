<script setup lang="ts">
import { ArrowLeft, ClipboardCheck, FileText, RefreshCw } from '@lucide/vue';
import { computed, onMounted, ref } from 'vue';

import {
  generateMockInterviewReview,
  getMockInterviewReview,
  getMockInterviewReviews,
} from '@/api/mock-interview-api';
import type {
  InterviewRound,
  MockInterviewReview,
  MockInterviewReviewSummary,
  MockInterviewTurn,
} from '@/types/interview';

const reviews = ref<MockInterviewReviewSummary[]>([]);
const selectedReview = ref<MockInterviewReview | null>(null);
const loading = ref(false);
const generating = ref(false);
const error = ref('');

const roundLabels: Record<InterviewRound, string> = {
  FIRST: '初轮技术面',
  SECOND: '深入技术面',
  THIRD: '综合终面',
  HR: 'HR 沟通面',
};

const selectedTurns = computed(() => selectedReview.value?.turns ?? []);

function getErrorMessage(requestError: unknown, fallback: string): string {
  return requestError instanceof Error ? requestError.message : fallback;
}

function statusLabel(status: MockInterviewReviewSummary['status']): string {
  return status === 'COMPLETED' ? '已完成' : '已提前结束';
}

function formatTime(value: string | null): string {
  if (!value) return '未记录';
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

async function loadReviews(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    reviews.value = await getMockInterviewReviews();
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '面试复盘列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function openReview(item: MockInterviewReviewSummary): Promise<void> {
  if (!item.reviewGenerated) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    selectedReview.value = await getMockInterviewReview(item.sessionId);
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '面试复盘加载失败');
  } finally {
    loading.value = false;
  }
}

async function generateReview(item: MockInterviewReviewSummary): Promise<void> {
  generating.value = true;
  error.value = '';
  try {
    selectedReview.value = await generateMockInterviewReview(item.sessionId);
    await loadReviews();
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '面试复盘生成失败');
  } finally {
    generating.value = false;
  }
}

function closeDetail(): void {
  selectedReview.value = null;
}

function turnLabel(turn: MockInterviewTurn): string {
  return turn.turnType === 'FOLLOW_UP'
    ? `第 ${turn.sequenceNo} 题追问 ${turn.followUpNo}`
    : `第 ${turn.sequenceNo} 道主问题`;
}

onMounted(() => {
  void loadReviews();
});
</script>

<template>
  <section class="review-page">
    <template v-if="!selectedReview">
      <header class="page-heading">
        <div>
          <h1>面试复盘</h1>
          <p>查看已结束的模拟面试记录，梳理优势、短板与下一步训练重点。</p>
        </div>
        <el-button :icon="RefreshCw" :loading="loading" @click="loadReviews">刷新</el-button>
      </header>

      <p v-if="error" class="error-message" role="alert">{{ error }}</p>
      <section v-if="loading && !reviews.length" class="state-section">正在加载面试记录...</section>
      <section v-else-if="!reviews.length" class="state-section">
        <ClipboardCheck :size="26" />
        <p>完成或提前结束一轮模拟面试后，复盘记录会出现在这里。</p>
      </section>
      <section v-else class="review-list" aria-label="面试复盘列表">
        <article v-for="item in reviews" :key="item.sessionId" class="review-item">
          <div class="review-item__main">
            <div class="review-item__title">
              <h2>{{ item.targetPosition }}</h2>
              <span class="status-tag" :class="{ 'status-tag--early': item.status === 'ENDED_EARLY' }">
                {{ statusLabel(item.status) }}
              </span>
            </div>
            <p>{{ roundLabels[item.interviewRound] }}<span v-if="item.targetCompany"> · {{ item.targetCompany }}</span></p>
            <dl>
              <div><dt>回答</dt><dd>{{ item.answeredTurnCount }} 题</dd></div>
              <div><dt>追问</dt><dd>{{ item.followUpCount }} 次</dd></div>
              <div><dt>均分</dt><dd>{{ item.averageScore ?? '-' }}</dd></div>
              <div><dt>结束时间</dt><dd>{{ formatTime(item.finishedTime) }}</dd></div>
            </dl>
          </div>
          <div class="review-item__actions">
            <el-button v-if="item.reviewGenerated" type="primary" :icon="FileText" @click="openReview(item)">
              查看复盘
            </el-button>
            <template v-else-if="item.reviewAvailable">
              <span>本轮已结束，可生成复盘</span>
              <el-button type="primary" :loading="generating" @click="generateReview(item)">生成复盘</el-button>
            </template>
            <span v-else>未回答题目，无法生成复盘</span>
          </div>
        </article>
      </section>
    </template>

    <template v-else>
      <header class="page-heading">
        <div>
          <button class="back-button" type="button" @click="closeDetail"><ArrowLeft :size="17" /> 返回列表</button>
          <h1>{{ selectedReview.targetPosition }}面试复盘</h1>
          <p>{{ roundLabels[selectedReview.interviewRound] }} · {{ statusLabel(selectedReview.status) }} · {{ formatTime(selectedReview.finishedTime) }}</p>
        </div>
      </header>

      <section class="overview-section">
        <div><span>平均分</span><strong>{{ selectedReview.averageScore ?? '-' }}</strong></div>
        <div><span>已答题</span><strong>{{ selectedReview.answeredTurnCount }}</strong></div>
        <div><span>主问题</span><strong>{{ selectedReview.mainQuestionCount }}</strong></div>
        <div><span>追问</span><strong>{{ selectedReview.followUpCount }}</strong></div>
      </section>

      <section class="feedback-section">
        <article><h2>整体反馈</h2><p>{{ selectedReview.overallFeedback }}</p></article>
        <article><h2>优势</h2><p>{{ selectedReview.strengths }}</p></article>
        <article><h2>优先改进项</h2><p>{{ selectedReview.improvementAreas }}</p></article>
        <article><h2>下一步训练</h2><p>{{ selectedReview.actionItems }}</p></article>
      </section>

      <section class="turn-section">
        <h2>逐题复盘</h2>
        <el-collapse>
          <el-collapse-item v-for="turn in selectedTurns" :key="turn.id" :name="turn.id">
            <template #title>
              <span class="turn-title">{{ turnLabel(turn) }} · {{ turn.score === null ? '未作答' : `${turn.score}/10` }}</span>
            </template>
            <div class="turn-detail">
              <h3>问题</h3><p>{{ turn.question }}</p>
              <template v-if="turn.userAnswer">
                <h3>我的回答</h3><p>{{ turn.userAnswer }}</p>
                <h3>改进建议</h3><p>{{ turn.suggestion }}</p>
                <h3>参考答案</h3><p>{{ turn.correctAnswer }}</p>
              </template>
              <p v-else class="unanswered">本题未作答</p>
            </div>
          </el-collapse-item>
        </el-collapse>
      </section>
    </template>
  </section>
</template>

<style scoped>
.review-page { display: grid; gap: 24px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; }
.page-heading h1 { color: var(--ink-strong); font-size: 28px; }
.page-heading p { margin-top: 6px; color: var(--ink-muted); font-size: 14px; }
.error-message, .state-section, .review-item, .overview-section, .feedback-section article, .turn-section { border: 1px solid var(--border); background: var(--surface); border-radius: var(--radius-md); }
.error-message { padding: 12px 14px; color: var(--danger); background: var(--danger-subtle); }
.state-section { display: grid; min-height: 180px; place-items: center; align-content: center; gap: 10px; color: var(--ink-muted); font-size: 14px; }
.review-list { display: grid; gap: 12px; }
.review-item { display: flex; justify-content: space-between; gap: 20px; padding: 20px; }
.review-item__main { min-width: 0; }
.review-item__title { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.review-item h2, .feedback-section h2, .turn-section > h2 { color: var(--ink-strong); font-size: 17px; }
.review-item__main > p { margin-top: 6px; color: var(--ink-muted); font-size: 13px; }
.status-tag { padding: 3px 7px; border-radius: var(--radius-sm); color: var(--primary); background: var(--primary-subtle); font-size: 12px; font-weight: 700; }
.status-tag--early { color: #9b5c00; background: #fff3d9; }
.review-item dl { display: flex; flex-wrap: wrap; gap: 8px 20px; margin: 14px 0 0; }
.review-item dl div { display: flex; gap: 6px; color: var(--ink-muted); font-size: 12px; }
.review-item dd { margin: 0; color: var(--ink-strong); font-weight: 700; }
.review-item__actions { display: grid; align-content: center; justify-items: end; gap: 8px; flex: 0 0 auto; color: var(--ink-muted); font-size: 12px; }
.back-button { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 10px; padding: 0; border: 0; background: transparent; color: var(--primary); font: inherit; cursor: pointer; }
.overview-section { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); padding: 20px; }
.overview-section div { display: grid; gap: 7px; padding: 0 18px; border-left: 1px solid var(--border); }
.overview-section div:first-child { padding-left: 0; border-left: 0; }
.overview-section span { color: var(--ink-muted); font-size: 13px; }
.overview-section strong { color: var(--primary); font-size: 27px; }
.feedback-section { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.feedback-section article, .turn-section { padding: 20px; }
.feedback-section p, .turn-detail p { margin-top: 10px; color: var(--ink); font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
.turn-section { display: grid; gap: 16px; }
.turn-title { color: var(--ink-strong); font-size: 14px; font-weight: 700; }
.turn-detail { padding: 4px 4px 12px; }
.turn-detail h3 { margin-top: 15px; color: var(--ink-strong); font-size: 13px; }
.unanswered { color: var(--ink-muted) !important; }
@media (max-width: 760px) { .page-heading, .review-item { align-items: flex-start; flex-direction: column; } .review-item__actions { justify-items: start; } .overview-section, .feedback-section { grid-template-columns: repeat(2, minmax(0, 1fr)); } .overview-section div:nth-child(3) { padding-left: 0; border-left: 0; margin-top: 16px; } .overview-section div:nth-child(4) { margin-top: 16px; } }
</style>
