<script setup lang="ts">
import { ArrowRight, FileText, Play, Send, Trash2, Upload } from '@lucide/vue';
import { computed, onMounted, ref } from 'vue';

import {
  answerMockInterviewTurn,
  createMockInterview,
  finishMockInterview,
  getNextMockInterviewQuestion,
} from '@/api/mock-interview-api';
import { deleteResume, getResumes, uploadResume } from '@/api/resume-api';
import type {
  InterviewRound,
  MockInterviewSession,
  MockInterviewTurn,
  ResumeDocument,
} from '@/types/interview';

const resumes = ref<ResumeDocument[]>([]);
const selectedResumeId = ref<number | null>(null);
const targetPosition = ref('Java 后端实习生');
const interviewRound = ref<InterviewRound>('FIRST');
const session = ref<MockInterviewSession | null>(null);
const answerDraft = ref('');
const loading = ref(false);
const uploadLoading = ref(false);
const error = ref('');
const fileInput = ref<HTMLInputElement | null>(null);

const currentTurn = computed(() => session.value?.turns.at(-1) ?? null);
const currentTurnAnswered = computed(() => Boolean(currentTurn.value?.userAnswer));
const roundOptions: Array<{ value: InterviewRound; label: string; description: string }> = [
  { value: 'FIRST', label: '一面', description: '经历、基础与沟通' },
  { value: 'SECOND', label: '二面', description: '项目深度与技术取舍' },
  { value: 'THIRD', label: '三面', description: '架构、协作与业务判断' },
];

function getErrorMessage(requestError: unknown, fallback: string): string {
  return requestError instanceof Error ? requestError.message : fallback;
}

async function loadResumes(): Promise<void> {
  try {
    resumes.value = await getResumes();
    if (!resumes.value.some((resume) => resume.id === selectedResumeId.value)) {
      selectedResumeId.value = resumes.value[0]?.id ?? null;
    }
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '简历列表加载失败');
  }
}

function chooseFile(): void {
  fileInput.value?.click();
}

async function handleFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (file.size > 2 * 1024 * 1024) {
    error.value = '简历文件不能超过 2 MB';
    return;
  }
  uploadLoading.value = true;
  error.value = '';
  try {
    const resume = await uploadResume(file);
    resumes.value = [resume, ...resumes.value];
    selectedResumeId.value = resume.id;
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '简历上传失败');
  } finally {
    uploadLoading.value = false;
  }
}

async function removeResume(resume: ResumeDocument): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    await deleteResume(resume.id);
    resumes.value = resumes.value.filter((item) => item.id !== resume.id);
    if (selectedResumeId.value === resume.id) selectedResumeId.value = resumes.value[0]?.id ?? null;
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '简历删除失败');
  } finally {
    loading.value = false;
  }
}

async function startInterview(): Promise<void> {
  if (!selectedResumeId.value || !targetPosition.value.trim()) {
    error.value = '请选择简历并填写目标岗位';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    session.value = await createMockInterview({
      resumeId: selectedResumeId.value,
      targetPosition: targetPosition.value.trim(),
      interviewRound: interviewRound.value,
    });
    answerDraft.value = '';
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '模拟面试启动失败');
  } finally {
    loading.value = false;
  }
}

function replaceTurn(updatedTurn: MockInterviewTurn): void {
  if (!session.value) return;
  session.value = {
    ...session.value,
    turns: session.value.turns.map((turn) => (turn.id === updatedTurn.id ? updatedTurn : turn)),
  };
}

async function scoreAnswer(): Promise<void> {
  if (!session.value || !currentTurn.value || !answerDraft.value.trim()) {
    error.value = '请完成当前问题的回答';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const updatedTurn = await answerMockInterviewTurn(
      session.value.id,
      currentTurn.value.id,
      answerDraft.value.trim(),
    );
    replaceTurn(updatedTurn);
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '评分失败');
  } finally {
    loading.value = false;
  }
}

async function continueInterview(): Promise<void> {
  if (!session.value) return;
  loading.value = true;
  error.value = '';
  try {
    const nextTurn = await getNextMockInterviewQuestion(session.value.id);
    session.value = {
      ...session.value,
      questionCount: nextTurn.sequenceNo,
      turns: [...session.value.turns, nextTurn],
    };
    answerDraft.value = '';
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '下一题生成失败');
  } finally {
    loading.value = false;
  }
}

async function finishInterview(): Promise<void> {
  if (!session.value) return;
  loading.value = true;
  error.value = '';
  try {
    session.value = await finishMockInterview(session.value.id);
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '面试总结生成失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => void loadResumes());
</script>

<template>
  <section class="mock-interview-page">
    <header class="page-heading">
      <div>
        <h1>模拟面试</h1>
        <p>基于个人简历，以不同轮次完成连续的岗位面试训练。</p>
      </div>
      <span v-if="session" class="session-status">{{
        session.status === 'ACTIVE' ? `进行中 · ${session.questionCount}/8 题` : '已完成'
      }}</span>
    </header>

    <p v-if="error" class="error-message" role="alert">{{ error }}</p>

    <template v-if="!session">
      <section class="setup-section" aria-labelledby="resume-heading">
        <div class="section-heading">
          <div>
            <p class="section-label">第一步</p>
            <h2 id="resume-heading">选择简历</h2>
          </div>
          <el-button :icon="Upload" :loading="uploadLoading" @click="chooseFile"
            >上传简历</el-button
          >
        </div>
        <input
          ref="fileInput"
          class="file-input"
          type="file"
          accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
          @change="handleFileChange"
        />
        <div v-if="resumes.length" class="resume-list">
          <label
            v-for="resume in resumes"
            :key="resume.id"
            class="resume-item"
            :class="{ 'resume-item--selected': selectedResumeId === resume.id }"
          >
            <input v-model="selectedResumeId" type="radio" :value="resume.id" name="resume" />
            <FileText :size="19" /><span
              ><strong>{{ resume.fileName }}</strong
              ><small>{{ new Date(resume.createTime).toLocaleDateString('zh-CN') }}</small></span
            >
            <el-tooltip content="删除简历"
              ><button
                type="button"
                class="icon-button"
                :disabled="loading"
                aria-label="删除简历"
                @click.prevent="removeResume(resume)"
              >
                <Trash2 :size="17" /></button
            ></el-tooltip>
          </label>
        </div>
        <div v-else class="empty-message">
          上传 PDF、DOCX 或 TXT 简历后开始模拟面试，单个文件最大 2 MB。
        </div>
      </section>

      <section class="setup-section" aria-labelledby="round-heading">
        <div class="section-heading">
          <div>
            <p class="section-label">第二步</p>
            <h2 id="round-heading">设置面试场景</h2>
          </div>
        </div>
        <div class="setup-form">
          <el-form-item label="目标岗位"
            ><el-input
              v-model="targetPosition"
              maxlength="100"
              show-word-limit
              placeholder="例如：Java 后端实习生" /></el-form-item
          ><el-form-item label="面试轮次"
            ><el-radio-group v-model="interviewRound"
              ><el-radio-button
                v-for="round in roundOptions"
                :key="round.value"
                :label="round.value"
                >{{ round.label }}</el-radio-button
              ></el-radio-group
            >
            <p class="round-description">
              {{ roundOptions.find((round) => round.value === interviewRound)?.description }}
            </p></el-form-item
          >
        </div>
        <el-button
          type="primary"
          :icon="Play"
          :loading="loading"
          :disabled="!resumes.length"
          @click="startInterview"
          >开始模拟面试</el-button
        >
      </section>
    </template>

    <template v-else>
      <div class="interview-layout">
        <main class="interview-workspace">
          <section v-if="currentTurn" class="question-section">
            <p class="section-label">第 {{ currentTurn.sequenceNo }} 题</p>
            <h2>{{ currentTurn.question }}</h2>
          </section>
          <section v-if="currentTurn" class="answer-section">
            <template v-if="!currentTurnAnswered">
              <label class="answer-label" for="mock-answer">我的回答</label>
              <el-input
                id="mock-answer"
                v-model="answerDraft"
                type="textarea"
                :rows="10"
                maxlength="5000"
                show-word-limit
                :disabled="loading"
                placeholder="按真实面试表达方式组织你的回答"
              />
              <div class="answer-actions">
                <el-button type="primary" :icon="Send" :loading="loading" @click="scoreAnswer"
                  >提交回答</el-button
                ><el-button :disabled="loading" @click="finishInterview">结束面试</el-button>
              </div>
            </template>
            <template v-else>
              <div class="score-summary">
                <strong>{{ currentTurn.score }}/10</strong><span>本题评分</span>
              </div>
              <h3>改进建议</h3>
              <p>{{ currentTurn.suggestion }}</p>
              <h3>参考答案</h3>
              <p>{{ currentTurn.correctAnswer }}</p>
              <div v-if="session.status === 'ACTIVE'" class="answer-actions">
                <el-button
                  type="primary"
                  :icon="ArrowRight"
                  :loading="loading"
                  @click="continueInterview"
                  >继续面试</el-button
                ><el-button :disabled="loading" @click="finishInterview">结束并生成总结</el-button>
              </div>
            </template>
          </section>
        </main>
        <aside class="session-side">
          <p class="section-label">面试信息</p>
          <dl>
            <div>
              <dt>岗位</dt>
              <dd>{{ session.targetPosition }}</dd>
            </div>
            <div>
              <dt>轮次</dt>
              <dd>
                {{ roundOptions.find((round) => round.value === session?.interviewRound)?.label }}
              </dd>
            </div>
            <div>
              <dt>题数</dt>
              <dd>{{ session.questionCount }}/8</dd>
            </div>
          </dl>
          <section v-if="session.summary" class="report">
            <h2>面试总结</h2>
            <p>{{ session.summary }}</p>
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped>
.mock-interview-page {
  display: grid;
  gap: 24px;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}
.page-heading h1 {
  font-size: 28px;
}
.page-heading p {
  margin-top: 6px;
  color: var(--ink-muted);
  font-size: 14px;
}
.session-status {
  color: var(--primary);
  font-size: 14px;
  font-weight: 700;
}
.setup-section,
.question-section,
.answer-section,
.session-side {
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}
.setup-section {
  display: grid;
  gap: 20px;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.section-label {
  margin-bottom: 4px;
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 700;
}
.section-heading h2,
.question-section h2,
.report h2 {
  color: var(--ink-strong);
  font-size: 19px;
  line-height: 1.55;
}
.file-input {
  display: none;
}
.resume-list {
  display: grid;
  gap: 8px;
}
.resume-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.resume-item--selected {
  border-color: var(--primary);
  background: var(--primary-subtle);
}
.resume-item span {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 3px;
}
.resume-item strong {
  overflow: hidden;
  color: var(--ink-strong);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.resume-item small,
.round-description {
  color: var(--ink-muted);
  font-size: 12px;
}
.icon-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--ink-muted);
}
.icon-button:hover {
  background: var(--danger-subtle);
  color: var(--danger);
}
.empty-message {
  padding: 18px 0;
  color: var(--ink-muted);
  font-size: 14px;
}
.setup-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}
.round-description {
  margin-top: 8px;
}
.interview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
  align-items: start;
}
.interview-workspace {
  display: grid;
  gap: 20px;
}
.question-section h2 {
  white-space: pre-wrap;
}
.answer-section {
  display: grid;
  gap: 14px;
}
.answer-label,
.answer-section h3 {
  color: var(--ink-strong);
  font-size: 14px;
  font-weight: 700;
}
.answer-section h3 {
  margin-top: 4px;
}
.answer-section p,
.report p {
  color: var(--ink);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}
.answer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}
.score-summary {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}
.score-summary strong {
  color: var(--primary);
  font-size: 32px;
}
.score-summary span {
  color: var(--ink-muted);
  font-size: 13px;
}
.session-side {
  display: grid;
  gap: 18px;
}
.session-side dl {
  display: grid;
  gap: 12px;
  margin: 0;
}
.session-side dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.session-side dt {
  color: var(--ink-muted);
  font-size: 13px;
}
.session-side dd {
  margin: 0;
  color: var(--ink-strong);
  font-size: 13px;
  font-weight: 700;
  text-align: right;
}
.report {
  padding-top: 18px;
  border-top: 1px solid var(--border);
}
.report h2 {
  margin-bottom: 10px;
  font-size: 16px;
}
.error-message {
  padding: 12px 14px;
  border: 1px solid #f4c7d0;
  border-radius: var(--radius-sm);
  background: var(--danger-subtle);
  color: var(--danger);
  font-size: 14px;
}
@media (max-width: 900px) {
  .interview-layout,
  .setup-form {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 600px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .setup-section,
  .question-section,
  .answer-section,
  .session-side {
    padding: 18px;
  }
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
