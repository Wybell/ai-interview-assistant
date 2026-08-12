import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/api/client';
import { createMockInterview } from '@/api/mock-interview-api';

describe('模拟面试接口', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('sends the optional target company when creating a session', async () => {
    const payload = {
      resumeId: 7,
      targetPosition: 'Java 后端实习生',
      targetCompany: '腾讯',
      interviewRound: 'FIRST' as const,
    };
    vi.spyOn(apiClient, 'request').mockResolvedValue({
      data: { code: 200, message: 'ok', data: { id: 1 } },
    });

    await createMockInterview(payload);

    expect(apiClient.request).toHaveBeenCalledWith({
      url: '/mock-interviews',
      method: 'post',
      data: payload,
    });
  });
});
