import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/api/client';
import {
  createMockInterview,
  endMockInterviewEarly,
  getActiveMockInterviews,
  generateMockInterviewReview,
  getMockInterviewReview,
  getMockInterviewReviews,
  getMockInterviewSession,
} from '@/api/mock-interview-api';

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

  it('uses the protected active, detail, and early-end session endpoints', async () => {
    vi.spyOn(apiClient, 'request').mockResolvedValue({
      data: { code: 200, message: 'ok', data: [] },
    });

    await getActiveMockInterviews();
    await getMockInterviewSession(7);
    await endMockInterviewEarly(7);

    expect(apiClient.request).toHaveBeenNthCalledWith(1, {
      url: '/mock-interviews/active',
      method: 'get',
    });
    expect(apiClient.request).toHaveBeenNthCalledWith(2, {
      url: '/mock-interviews/7',
      method: 'get',
    });
    expect(apiClient.request).toHaveBeenNthCalledWith(3, {
      url: '/mock-interviews/7/end',
      method: 'post',
    });
  });

  it('uses the review list, detail, and generation endpoints', async () => {
    vi.spyOn(apiClient, 'request').mockResolvedValue({
      data: { code: 200, message: 'ok', data: [] },
    });

    await getMockInterviewReviews();
    await getMockInterviewReview(7);
    await generateMockInterviewReview(7);

    expect(apiClient.request).toHaveBeenNthCalledWith(1, {
      url: '/mock-interviews/reviews',
      method: 'get',
    });
    expect(apiClient.request).toHaveBeenNthCalledWith(2, {
      url: '/mock-interviews/7/review',
      method: 'get',
    });
    expect(apiClient.request).toHaveBeenNthCalledWith(3, {
      url: '/mock-interviews/7/review',
      method: 'post',
    });
  });
});
