import { API_BASE_URL, request } from '@/api/client';
import { ApiError, type ApiResponse } from '@/types/api';
import type { AiScoreResult, QuestionRequest, ScoreRequest } from '@/types/interview';
import { readSession } from '@/utils/session';
import { parseScoreResult, readSseStream } from '@/utils/sse';

export interface ScoreStreamCallbacks {
  onDelta: (text: string) => void;
  onDone: (result: AiScoreResult) => void;
  onError: (message: string) => void;
}

export function generateQuestion(payload: QuestionRequest): Promise<string> {
  return request<string>({
    url: '/question/ask',
    method: 'post',
    data: payload,
  });
}

export function scoreAnswer(payload: ScoreRequest): Promise<AiScoreResult> {
  return request<AiScoreResult>({
    url: '/question/score',
    method: 'post',
    data: payload,
  });
}

export async function streamScore(
  payload: ScoreRequest,
  callbacks: ScoreStreamCallbacks,
  signal: AbortSignal,
): Promise<void> {
  const session = readSession();
  if (!session) {
    throw new ApiError('登录状态已失效，请重新登录', { status: 401, code: 401 });
  }

  const response = await fetch(`${API_BASE_URL}/question/score/stream`, {
    method: 'post',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.token}`,
    },
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok) {
    const contentType = response.headers.get('content-type') ?? '';
    if (contentType.includes('application/json')) {
      const body = (await response.json()) as unknown;
      if (typeof body === 'object' && body !== null) {
        const apiBody = body as Partial<ApiResponse<unknown>>;
        throw new ApiError(apiBody.message ?? '评分请求失败，请稍后重试', {
          status: response.status,
          code: apiBody.code,
        });
      }
    }
    throw new ApiError('评分请求失败，请稍后重试', { status: response.status });
  }

  if (!response.body) {
    throw new ApiError('评分服务未返回可读取的数据流');
  }

  let completed = false;
  await readSseStream(
    response.body,
    {
      onEvent: ({ event, data }) => {
        if (event === 'done') {
          completed = true;
          callbacks.onDone(parseScoreResult(data));
          return;
        }

        if (event === 'error') {
          callbacks.onError(data || '评分失败，请稍后重试');
          return;
        }

        callbacks.onDelta(data);
      },
    },
    signal,
  );

  if (!completed && !signal.aborted) {
    throw new ApiError('评分流意外结束，请重试');
  }
}
