import type { AiScoreResult } from '@/types/interview';

export interface SseEvent {
  event: string;
  data: string;
}

export interface SseCallbacks {
  onEvent: (event: SseEvent) => void;
}

export function parseSseEvent(block: string): SseEvent | null {
  const lines = block.replace(/\r/g, '').split('\n');
  const data: string[] = [];
  let event = 'message';

  for (const line of lines) {
    if (!line || line.startsWith(':')) {
      continue;
    }

    if (line.startsWith('event:')) {
      event = line.slice(6).trim() || 'message';
      continue;
    }

    if (line.startsWith('data:')) {
      data.push(line.slice(5).replace(/^\s/, ''));
    }
  }

  if (data.length === 0) {
    return null;
  }

  return { event, data: data.join('\n') };
}

export async function readSseStream(
  stream: ReadableStream<Uint8Array>,
  callbacks: SseCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  const consumeBlock = (block: string): void => {
    const event = parseSseEvent(block);
    if (event) {
      callbacks.onEvent(event);
    }
  };

  try {
    while (true) {
      if (signal?.aborted) {
        throw new DOMException('评分已取消', 'AbortError');
      }

      const { done, value } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      let separatorIndex = buffer.indexOf('\n\n');

      while (separatorIndex >= 0) {
        consumeBlock(buffer.slice(0, separatorIndex));
        buffer = buffer.slice(separatorIndex + 2);
        separatorIndex = buffer.indexOf('\n\n');
      }
    }

    buffer += decoder.decode();
    if (buffer.trim()) {
      consumeBlock(buffer);
    }
  } finally {
    reader.releaseLock();
  }
}

export function parseScoreResult(raw: string): AiScoreResult {
  const parsed: unknown = JSON.parse(raw);
  if (typeof parsed !== 'object' || parsed === null) {
    throw new Error('评分结果格式不正确');
  }

  const record = parsed as Record<string, unknown>;
  const score = Number(record.score);
  const correctAnswer = record.correct_answer ?? record.correctAnswer;
  const suggestion = record.suggestion;

  if (
    !Number.isFinite(score) ||
    score < 0 ||
    score > 10 ||
    typeof correctAnswer !== 'string' ||
    typeof suggestion !== 'string'
  ) {
    throw new Error('评分结果缺少必要字段');
  }

  return { score, correctAnswer, suggestion };
}
