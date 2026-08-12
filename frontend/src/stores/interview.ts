import { defineStore } from 'pinia';

import { generateQuestion, streamScore } from '@/api/interview-api';
import { isApiError } from '@/types/api';
import type {
  AiScoreResult,
  QuestionHistoryEntry,
  QuestionRequest,
  ScoreStatus,
} from '@/types/interview';

function getErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '操作失败，请稍后重试';
}

export const useInterviewStore = defineStore('interview', {
  state: () => ({
    direction: 'backend' as 'frontend' | 'backend',
    language: 'Java',
    tag: 'HashMap',
    knowledgeTopicId: null as number | null,
    questionMode: 'CUSTOM_TOPIC' as QuestionRequest['mode'],
    question: '',
    answer: '',
    questionLoading: false,
    questionError: '',
    scoreStatus: 'idle' as ScoreStatus,
    scoreResult: null as AiScoreResult | null,
    scoreError: '',
    streamText: '',
    abortController: null as AbortController | null,
    questionHistory: [] as QuestionHistoryEntry[],
    questionHistoryIndex: -1,
  }),
  actions: {
    resetQuestionHistory() {
      this.questionHistory = [];
      this.questionHistoryIndex = -1;
      this.question = '';
      this.answer = '';
      this.questionError = '';
      this.resetScore();
    },
    resetScore() {
      this.scoreStatus = 'idle';
      this.scoreResult = null;
      this.scoreError = '';
      this.streamText = '';
    },
    saveCurrentQuestionState() {
      const current = this.questionHistory[this.questionHistoryIndex];
      if (!current) {
        return;
      }
      current.answer = this.answer;
      current.scoreStatus = this.scoreStatus;
      current.scoreResult = this.scoreResult;
      current.scoreError = this.scoreError;
      current.streamText = this.streamText;
    },
    restoreQuestion(entry: QuestionHistoryEntry) {
      this.question = entry.question;
      this.answer = entry.answer;
      this.questionError = '';
      this.scoreStatus = entry.scoreStatus;
      this.scoreResult = entry.scoreResult;
      this.scoreError = entry.scoreError;
      this.streamText = entry.streamText;
    },
    async generate(refresh = true) {
      const tag = this.tag.trim();
      if (!tag) {
        this.questionError = '请输入知识点';
        return;
      }

      this.saveCurrentQuestionState();
      this.questionLoading = true;
      this.questionError = '';
      try {
        const question = await generateQuestion({
          direction: this.direction,
          language: this.language,
          tag: this.questionMode === 'KNOWLEDGE_BASE' ? '' : tag,
          knowledgeTopicId: this.knowledgeTopicId ?? undefined,
          mode: this.questionMode,
          refresh,
        });
        this.question = question;
        this.answer = '';
        this.resetScore();
        this.questionHistory = this.questionHistory.slice(0, this.questionHistoryIndex + 1);
        this.questionHistory.push({
          question,
          answer: '',
          scoreStatus: 'idle',
          scoreResult: null,
          scoreError: '',
          streamText: '',
        });
        this.questionHistoryIndex = this.questionHistory.length - 1;
      } catch (error) {
        this.questionError = getErrorMessage(error);
      } finally {
        this.questionLoading = false;
      }
    },
    goToPreviousQuestion() {
      if (this.questionLoading || this.scoreStatus === 'streaming' || this.questionHistoryIndex <= 0) {
        return;
      }
      this.saveCurrentQuestionState();
      this.questionHistoryIndex -= 1;
      this.restoreQuestion(this.questionHistory[this.questionHistoryIndex]);
    },
    async score() {
      if (!this.question.trim() || !this.answer.trim()) {
        this.scoreError = '请先生成题目并完成回答';
        return;
      }

      this.scoreStatus = 'streaming';
      this.scoreResult = null;
      this.scoreError = '';
      this.streamText = '';
      this.saveCurrentQuestionState();
      const controller = new AbortController();
      this.abortController = controller;

      try {
        await streamScore(
          {
            tag: this.tag.trim(),
            question: this.question,
            answer: this.answer,
          },
          {
            onDelta: (text) => {
              this.streamText += text;
              this.saveCurrentQuestionState();
            },
            onDone: (result) => {
              this.scoreResult = result;
              this.scoreStatus = 'complete';
              this.saveCurrentQuestionState();
            },
            onError: (message) => {
              this.scoreError = message;
              this.scoreStatus = 'error';
              this.saveCurrentQuestionState();
            },
          },
          controller.signal,
        );
      } catch (error) {
        if (controller.signal.aborted) {
          this.scoreStatus = 'cancelled';
          this.scoreError = '评分已取消';
          this.saveCurrentQuestionState();
        } else if (!this.scoreError) {
          this.scoreStatus = 'error';
          this.scoreError = getErrorMessage(error);
          this.saveCurrentQuestionState();
        }
      } finally {
        if (this.abortController === controller) {
          this.abortController = null;
        }
      }
    },
    cancelScore() {
      this.abortController?.abort();
    },
  },
});
