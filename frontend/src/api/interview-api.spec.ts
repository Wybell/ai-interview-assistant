import { afterEach, describe, expect, it, vi } from 'vitest';

import { streamScore } from '@/api/interview-api';
import { writeSession } from '@/utils/session';

describe('streamScore', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    sessionStorage.clear();
  });

  it('uses POST JSON with a bearer token', async () => {
    writeSession({ token: 'test-token', username: 'tester' });
    const body = 'event: done\ndata: {"score":8,"correct_answer":"ok","suggestion":"keep going"}\n\n';
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(body, { status: 200, headers: { 'content-type': 'text/event-stream' } }),
    );
    const onDone = vi.fn();

    await streamScore(
      { tag: 'Java', question: 'Question', answer: 'Answer' },
      { onDelta: vi.fn(), onDone, onError: vi.fn() },
      new AbortController().signal,
    );

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/question/score/stream',
      expect.objectContaining({
        method: 'post',
        body: JSON.stringify({ tag: 'Java', question: 'Question', answer: 'Answer' }),
        headers: expect.objectContaining({ Authorization: 'Bearer test-token' }),
      }),
    );
    expect(onDone).toHaveBeenCalledWith({ score: 8, correctAnswer: 'ok', suggestion: 'keep going' });
  });
});
