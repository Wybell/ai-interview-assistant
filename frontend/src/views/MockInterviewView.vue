<script setup lang="ts">
import { ArrowRight, Eye, FileText, Mic, MicOff, Play, Send, Trash2, Upload } from '@lucide/vue';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

import {
  answerMockInterviewTurn,
  createMockInterview,
  finishMockInterview,
  getFollowUpMockInterviewQuestion,
  getNextMockInterviewQuestion,
} from '@/api/mock-interview-api';
import { deleteResume, getResumes, previewResume, uploadResume } from '@/api/resume-api';
import type {
  InterviewRound,
  MockInterviewSession,
  MockInterviewTurn,
  ResumeDocument,
  ResumePreview,
} from '@/types/interview';

const resumes = ref<ResumeDocument[]>([]);
const selectedResumeId = ref<number | null>(null);
const targetPosition = ref('Java 后端实习生');
const targetCompany = ref('');
const interviewRound = ref<InterviewRound>('FIRST');
const session = ref<MockInterviewSession | null>(null);
const answerDraft = ref('');
const loading = ref(false);
const uploadLoading = ref(false);
const error = ref('');
const fileInput = ref<HTMLInputElement | null>(null);
const previewVisible = ref(false);
const previewLoading = ref(false);
const previewError = ref('');
const previewDocument = ref<ResumePreview | null>(null);
const speechError = ref('');
const isListening = ref(false);

interface SpeechRecognitionEventLike extends Event {
  resultIndex: number;
  results: {
    length: number;
    [index: number]: { length: number; [index: number]: { transcript: string } };
  };
}

interface SpeechRecognitionLike {
  lang: string;
  interimResults: boolean;
  continuous: boolean;
  start(): void;
  stop(): void;
  abort(): void;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: Event & { error?: string }) => void) | null;
  onend: (() => void) | null;
}

type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  }
}

let speechRecognition: SpeechRecognitionLike | null = null;

const currentTurn = computed(() => session.value?.turns.at(-1) ?? null);
const currentTurnAnswered = computed(() => Boolean(currentTurn.value?.userAnswer));
const currentMainTurn = computed(() => {
  if (!currentTurn.value || !session.value) return null;
  return currentTurn.value.turnType === 'MAIN'
    ? currentTurn.value
    : session.value.turns.find((turn) => turn.id === currentTurn.value?.parentTurnId) ?? null;
});
const followUpAvailable = computed(() => {
  const mainTurn = currentMainTurn.value;
  return Boolean(
    mainTurn &&
      currentTurnAnswered.value &&
      session.value?.status === 'ACTIVE' &&
      session.value.turns.filter((turn) => turn.parentTurnId === mainTurn.id).length < 2,
  );
});
const roundCompletionHint = computed(() => {
  if (!session.value || !currentTurnAnswered.value || session.value.questionCount < session.value.questionLimit) {
    return '';
  }
  return currentTurn.value?.turnType === 'FOLLOW_UP' && currentTurn.value.followUpNo === 2
    ? '本轮面试已完成，当前问题已完成两次追问。请结束本轮并生成总结。'
    : '本轮主问题已完成，可以继续追问当前问题，或结束本轮并生成总结。';
});
const roundLabels: Record<InterviewRound, string> = {
  FIRST: '初轮技术面',
  SECOND: '深入技术面',
  THIRD: '综合终面',
  HR: 'HR 沟通面',
};
const roundDescriptions: Record<InterviewRound, string> = {
  FIRST: '简历核验、基础知识、项目概述与表达沟通',
  SECOND: '项目深挖、原理、排障、技术取舍与场景追问',
  THIRD: '系统设计、业务理解、协作、责任意识与决策判断',
  HR: '求职动机、岗位匹配、职业规划、沟通协作与到岗安排',
};
const roundOptions: Array<{ value: InterviewRound; label: string; description: string }> = [
  { value: 'FIRST', label: '初轮技术面', description: '简历核验、基础知识、项目概述与表达沟通' },
  { value: 'SECOND', label: '深入技术面', description: '项目深挖、原理、排障、技术取舍与场景追问' },
  { value: 'THIRD', label: '综合终面', description: '系统设计、业务理解、协作、责任意识与决策判断' },
  { value: 'HR', label: 'HR 沟通面', description: '求职动机、岗位匹配、职业规划、沟通协作与到岗安排' },
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
  if (file.size > 10 * 1024 * 1024) {
    error.value = '简历文件不能超过 10 MB';
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

async function viewResume(resume: ResumeDocument): Promise<void> {
  previewVisible.value = true;
  previewLoading.value = true;
  previewError.value = '';
  previewDocument.value = null;
  try {
    previewDocument.value = await previewResume(resume.id);
  } catch (requestError) {
    previewError.value = getErrorMessage(requestError, '简历预览加载失败');
  } finally {
    previewLoading.value = false;
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
      targetCompany: targetCompany.value.trim() || undefined,
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

function appendSpeechText(text: string): void {
  const normalized = text.trim();
  if (!normalized) return;
  answerDraft.value = answerDraft.value.trim()
    ? `${answerDraft.value.trim()} ${normalized}`
    : normalized;
}

function toggleSpeechInput(): void {
  if (isListening.value) {
    speechRecognition?.stop();
    return;
  }
  const Recognition = window.SpeechRecognition ?? window.webkitSpeechRecognition;
  if (!Recognition) {
    speechError.value = '当前浏览器不支持语音转文字，请使用 Chrome 或 Edge';
    return;
  }
  speechError.value = '';
  speechRecognition = new Recognition();
  speechRecognition.lang = 'zh-CN';
  speechRecognition.interimResults = false;
  speechRecognition.continuous = true;
  speechRecognition.onresult = (event) => {
    for (let index = event.resultIndex; index < event.results.length; index += 1) {
      appendSpeechText(event.results[index][0].transcript);
    }
  };
  speechRecognition.onerror = (event) => {
    speechError.value = event.error === 'not-allowed'
      ? '麦克风权限被拒绝，请在浏览器设置中允许使用麦克风'
      : '语音识别失败，请重试或直接输入';
    isListening.value = false;
  };
  speechRecognition.onend = () => {
    isListening.value = false;
  };
  try {
    speechRecognition.start();
    isListening.value = true;
  } catch {
    speechError.value = '语音识别启动失败，请重试或直接输入';
  }
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
      questionCount: session.value.questionCount + 1,
      turns: [...session.value.turns, nextTurn],
    };
    answerDraft.value = '';
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '下一题生成失败');
  } finally {
    loading.value = false;
  }
}

async function followUpInterview(): Promise<void> {
  if (!session.value || !currentTurn.value) return;
  const mainTurn = currentTurn.value.turnType === 'MAIN'
    ? currentTurn.value
    : session.value.turns.find((turn) => turn.id === currentTurn.value?.parentTurnId);
  if (!mainTurn) return;
  loading.value = true;
  error.value = '';
  try {
    const followUp = await getFollowUpMockInterviewQuestion(session.value.id, mainTurn.id);
    session.value = { ...session.value, turns: [...session.value.turns, followUp] };
    answerDraft.value = '';
  } catch (requestError) {
    error.value = getErrorMessage(requestError, '追问生成失败');
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

function returnToSetup(resetScenario: boolean): void {
  if (session.value && !resetScenario) {
    targetPosition.value = session.value.targetPosition;
    targetCompany.value = session.value.targetCompany ?? '';
    interviewRound.value = session.value.interviewRound;
  }
  if (resetScenario) {
    targetPosition.value = '';
    targetCompany.value = '';
    interviewRound.value = 'FIRST';
  }
  session.value = null;
  answerDraft.value = '';
  error.value = '';
}

onMounted(() => void loadResumes());
onBeforeUnmount(() => speechRecognition?.abort());
</script>

<template>
  <section class="mock-interview-page">
    <header class="page-heading">
      <div>
        <h1>模拟面试</h1>
        <p>基于个人简历，以不同轮次完成连续的岗位面试训练。</p>
      </div>
      <span v-if="session" class="session-status">{{
        session.status === 'ACTIVE' ? `进行中 · ${session.questionCount}/${session.questionLimit} 道主问题` : '已完成'
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
            <el-tooltip content="查看简历">
              <button
                type="button"
                class="icon-button"
                :disabled="previewLoading"
                aria-label="查看简历"
                @click.prevent="viewResume(resume)"
              >
                <Eye :size="17" />
              </button>
            </el-tooltip>
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
          上传 PDF、DOCX 或 TXT 简历后开始模拟面试，单个文件最大 10 MB。
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
          <el-form-item label="求职岗位"
            ><el-input
              v-model="targetPosition"
              maxlength="100"
              show-word-limit
              placeholder="例如：Java 后端实习生" /></el-form-item
          ><el-form-item label="面试轮次">
            <div class="round-options" role="radiogroup" aria-label="面试轮次">
              <label
                v-for="round in roundOptions"
                :key="round.value"
                class="round-option"
                :class="{ 'round-option--selected': interviewRound === round.value }"
              >
                <input v-model="interviewRound" type="radio" name="interview-round" :value="round.value" />
                <span class="round-option__content">
                  <strong>{{ round.label }}</strong>
                  <small>{{ round.description }}</small>
                </span>
              </label>
            </div>
            <p class="round-description">{{ roundDescriptions[interviewRound] }}</p>
          </el-form-item>
          <el-form-item label="意向公司（选填）" class="company-field">
            <el-input
              v-model="targetCompany"
              maxlength="100"
              show-word-limit
              placeholder="例如：腾讯、字节跳动、小米"
            />
            <p class="company-description">用于公司风格模拟；未填写或没有可参考信息时按通用岗位面试进行。</p>
          </el-form-item>
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
            <p class="section-label">
              {{ currentTurn.turnType === 'FOLLOW_UP' ? `第 ${currentTurn.sequenceNo} 题 · 追问 ${currentTurn.followUpNo}/2` : `第 ${currentTurn.sequenceNo} 道主问题` }}
            </p>
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
              <div class="speech-actions">
                <el-tooltip :content="isListening ? '停止语音输入' : '语音转文字'">
                  <button
                    type="button"
                    class="speech-button"
                    :class="{ 'speech-button--active': isListening }"
                    :disabled="loading"
                    :aria-label="isListening ? '停止语音输入' : '语音转文字'"
                    @click="toggleSpeechInput"
                  >
                    <MicOff v-if="isListening" :size="17" />
                    <Mic v-else :size="17" />
                  </button>
                </el-tooltip>
                <span v-if="isListening" class="speech-status">正在听，请直接说话</span>
                <span v-if="speechError" class="speech-error">{{ speechError }}</span>
              </div>
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
              <p v-if="roundCompletionHint" class="completion-hint">{{ roundCompletionHint }}</p>
              <div v-if="session.status === 'ACTIVE'" class="answer-actions">
                <el-button
                  v-if="followUpAvailable"
                  :loading="loading"
                  @click="followUpInterview"
                  >追问这一题</el-button
                >
                <el-button
                  v-if="session.questionCount < session.questionLimit"
                  type="primary"
                  :icon="ArrowRight"
                  :loading="loading"
                  @click="continueInterview"
                  >下一题</el-button
                ><el-button :disabled="loading" @click="finishInterview">结束本轮并生成总结</el-button>
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
                {{ roundLabels[session.interviewRound] }}
              </dd>
            </div>
            <div>
              <dt>题数</dt>
              <dd>{{ session.questionCount }}/{{ session.questionLimit }} 道主问题</dd>
            </div>
            <div v-if="session.targetCompany">
              <dt>目标公司</dt>
              <dd>{{ session.targetCompany }}</dd>
            </div>
          </dl>
          <section v-if="session.summary" class="report">
            <h2>面试总结</h2>
            <p>{{ session.summary }}</p>
            <div class="answer-actions">
              <el-button type="primary" :icon="Play" @click="returnToSetup(false)">再来一场</el-button>
              <el-button @click="returnToSetup(true)">重新设置</el-button>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <el-drawer v-model="previewVisible" title="简历预览" size="min(720px, 92vw)">
      <div v-if="previewLoading" class="preview-state">正在加载简历内容...</div>
      <p v-else-if="previewError" class="error-message" role="alert">{{ previewError }}</p>
      <div v-else-if="previewDocument" class="resume-preview">
        <div class="resume-preview__meta">
          <strong>{{ previewDocument.fileName }}</strong>
          <span>{{ previewDocument.contentType }}</span>
        </div>
        <pre>{{ previewDocument.content }}</pre>
      </div>
    </el-drawer>
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
.preview-state {
  padding: 24px 0;
  color: var(--ink-muted);
  font-size: 14px;
}
.resume-preview {
  display: grid;
  gap: 16px;
}
.resume-preview__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}
.resume-preview__meta strong {
  color: var(--ink-strong);
  font-size: 15px;
  overflow-wrap: anywhere;
}
.resume-preview__meta span {
  color: var(--ink-muted);
  font-size: 12px;
}
.resume-preview pre {
  max-height: calc(100vh - 180px);
  margin: 0;
  overflow: auto;
  color: var(--ink);
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
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
.round-options {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.round-option {
  position: relative;
  display: block;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease;
}
.round-option:hover {
  border-color: var(--primary);
}
.round-option:has(input:focus-visible) {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
.round-option--selected {
  border-color: var(--primary);
  background: var(--primary-subtle);
}
.round-option input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}
.round-option__content {
  display: grid;
  gap: 6px;
}
.round-option__content strong {
  color: var(--ink-strong);
  font-size: 14px;
  line-height: 1.4;
}
.round-option__content small {
  color: var(--ink-muted);
  font-size: 12px;
  line-height: 1.55;
}
.round-description {
  margin-top: 8px;
}
.company-field {
  grid-column: 1 / -1;
}
.company-description {
  margin-top: 8px;
  color: var(--ink-muted);
  font-size: 12px;
  line-height: 1.6;
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
.speech-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: -6px;
}
.speech-button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--ink-muted);
  cursor: pointer;
}
.speech-button:hover,
.speech-button--active {
  border-color: var(--primary);
  background: var(--primary-subtle);
  color: var(--primary);
}
.speech-status,
.speech-error {
  color: var(--ink-muted);
  font-size: 12px;
}
.speech-error {
  color: var(--danger);
}
.completion-hint {
  padding: 10px 12px;
  border-left: 3px solid var(--primary);
  background: var(--primary-subtle);
  color: var(--ink-strong) !important;
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
@media (max-width: 720px) {
  .round-options {
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
