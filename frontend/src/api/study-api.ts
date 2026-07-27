import { request } from '@/api/client';
import type { MistakeRecord, StudyProgress } from '@/types/interview';

export function getMistakes(): Promise<MistakeRecord[]> {
  return request<MistakeRecord[]>({ url: '/mistakes', method: 'get' });
}

export function getStudyProgress(): Promise<StudyProgress[]> {
  return request<StudyProgress[]>({ url: '/progress', method: 'get' });
}
