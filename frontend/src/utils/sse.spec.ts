import { describe, expect, it } from 'vitest';

import { parseScoreResult, parseSseEvent, readSseStream } from '@/utils/sse';

describe('SSE 解析', () => {
  it('解析普通增量事件', () => {
    expect(parseSseEvent('data: 正在分析\n\ndata: 不应读取')).toEqual({
      event: 'message',
      data: '正在分析\n不应读取',
    });
  });

  it('解析完成事件中的评分结果', () => {
    expect(
      parseScoreResult(
        JSON.stringify({
          score: 8,
          correct_answer: '参考答案',
          suggestion: '补充扩容条件',
        }),
      ),
    ).toEqual({
      score: 8,
      correctAnswer: '参考答案',
      suggestion: '补充扩容条件',
    });
  });

  it('处理被拆开的 SSE 数据块', async () => {
    const encoder = new TextEncoder();
    const chunks = [
      'data: 正在',
      '评分\n\n',
      'event: done\ndata: {"score":7,"correct_answer":"参考","suggestion":"建议"}\n\n',
    ];
    const received: string[] = [];
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
        controller.close();
      },
    });

    await readSseStream(stream, {
      onEvent: (event) => received.push(`${event.event}:${event.data}`),
    });

    expect(received).toEqual([
      'message:正在评分',
      'done:{"score":7,"correct_answer":"参考","suggestion":"建议"}',
    ]);
  });
});
