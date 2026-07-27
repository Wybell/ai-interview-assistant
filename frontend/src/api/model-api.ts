import { request } from '@/api/client';
import type { AiModel, AiModelPreference } from '@/types/interview';

export function getAvailableModels(): Promise<AiModel[]> {
  return request<AiModel[]>({ url: '/ai/models', method: 'get' });
}

export function getCurrentModelPreference(): Promise<AiModelPreference> {
  return request<AiModelPreference>({ url: '/users/me/ai-preference', method: 'get' });
}

export function updateModelPreference(modelId: number): Promise<AiModelPreference> {
  return request<AiModelPreference>({
    url: '/users/me/ai-preference',
    method: 'put',
    data: { modelId },
  });
}
