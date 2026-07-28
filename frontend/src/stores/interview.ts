import { defineStore } from 'pinia';

import { generateQuestion, streamScore } from '@/api/interview-api';
import { isApiError } from '@/types/api';
import type { AiScoreResult, ScoreStatus } from '@/types/interview';

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
    question: '',
    answer: '',
    questionLoading: false,
    questionError: '',
    scoreStatus: 'idle' as ScoreStatus,
    scoreResult: null as AiScoreResult | null,
    scoreError: '',
    streamText: '',
    abortController: null as AbortController | null,
  }),
  actions: {
    resetScore() {
      this.scoreStatus = 'idle';
      this.scoreResult = null;
      this.scoreError = '';
      this.streamText = '';
    },
    async generate(refresh = true) {
      const tag = this.tag.trim();
      if (!tag) {
        this.questionError = '请输入知识点';
        return;
      }

      this.questionLoading = true;
      this.questionError = '';
      try {
        const question = await generateQuestion({
          direction: this.direction,
          language: this.language,
          tag,
          refresh,
        });
        this.question = question;
        this.answer = '';
        this.resetScore();
      } catch (error) {
        this.questionError = getErrorMessage(error);
      } finally {
        this.questionLoading = false;
      }
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
            },
            onDone: (result) => {
              this.scoreResult = result;
              this.scoreStatus = 'complete';
            },
            onError: (message) => {
              this.scoreError = message;
              this.scoreStatus = 'error';
            },
          },
          controller.signal,
        );
      } catch (error) {
        if (controller.signal.aborted) {
          this.scoreStatus = 'cancelled';
          this.scoreError = '评分已取消';
        } else if (!this.scoreError) {
          this.scoreStatus = 'error';
          this.scoreError = getErrorMessage(error);
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
