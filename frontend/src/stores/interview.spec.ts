import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

import { generateQuestion } from '@/api/interview-api';
import { useInterviewStore } from '@/stores/interview';

vi.mock('@/api/interview-api', () => ({
  generateQuestion: vi.fn(),
  streamScore: vi.fn(),
}));

describe('练习题历史导航', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it.each([
    ['CUSTOM_TOPIC', '自定义知识点', 'Redis'],
    ['TECHNICAL_TOPIC', '技术知识点', 'HashMap'],
  ] as const)('自定义和技术知识点模式支持上一题和下一题', async (mode, tag, firstTag) => {
    const store = useInterviewStore();
    store.questionMode = mode;
    store.tag = firstTag;
    vi.mocked(generateQuestion)
      .mockResolvedValueOnce(`${tag} 第一题`)
      .mockResolvedValueOnce(`${tag} 第二题`)
      .mockResolvedValueOnce(`${tag} 第三题`);

    await store.generate(true);
    await store.generate(true);
    expect(store.question).toBe(`${tag} 第二题`);
    expect(store.questionHistoryIndex).toBe(1);

    store.goToPreviousQuestion();
    expect(store.question).toBe(`${tag} 第一题`);
    expect(store.questionHistoryIndex).toBe(0);
    expect(generateQuestion).toHaveBeenCalledTimes(2);

    await store.generate(true);
    expect(store.question).toBe(`${tag} 第三题`);
    expect(store.questionHistory).toHaveLength(2);
    expect(store.questionHistoryIndex).toBe(1);
    expect(generateQuestion).toHaveBeenCalledTimes(3);
  });
});
