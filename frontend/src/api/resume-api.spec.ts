import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/api/client';
import { previewResume } from '@/api/resume-api';

describe('简历预览接口', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('reads preview content through the protected resume route', async () => {
    vi.spyOn(apiClient, 'request').mockResolvedValue({
      data: {
        code: 200,
        message: 'ok',
        data: {
          id: 7,
          fileName: 'resume.pdf',
          contentType: 'application/pdf',
          content: 'Java 后端项目经历',
        },
      },
    });

    await expect(previewResume(7)).resolves.toEqual({
      id: 7,
      fileName: 'resume.pdf',
      contentType: 'application/pdf',
      content: 'Java 后端项目经历',
    });
    expect(apiClient.request).toHaveBeenCalledWith({
      url: '/resumes/7/preview',
      method: 'get',
    });
  });
});
