import { request } from '@/api/client';
import type { ResumeDocument } from '@/types/interview';

export function getResumes(): Promise<ResumeDocument[]> {
  return request<ResumeDocument[]>({ url: '/resumes', method: 'get' });
}

export function uploadResume(file: File): Promise<ResumeDocument> {
  const formData = new FormData();
  formData.append('file', file);
  return request<ResumeDocument>({ url: '/resumes', method: 'post', data: formData });
}

export function deleteResume(resumeId: number): Promise<void> {
  return request<void>({ url: `/resumes/${resumeId}`, method: 'delete' });
}
