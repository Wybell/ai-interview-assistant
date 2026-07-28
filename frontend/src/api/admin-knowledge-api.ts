import { request } from '@/api/client';

export interface AdminKnowledgeQuestion { question: string; answer: string; difficulty?: string }
export interface AdminKnowledgeTopic {
  id?: number; direction: string; language: string; category: string; title: string; summary: string;
  keyPoints: string[]; questions: AdminKnowledgeQuestion[]; published?: boolean;
}

export function getAdminKnowledgeTopics(): Promise<AdminKnowledgeTopic[]> {
  return request<AdminKnowledgeTopic[]>({ url: '/admin/knowledge/topics', method: 'get' });
}
export function createAdminKnowledgeTopic(data: AdminKnowledgeTopic): Promise<AdminKnowledgeTopic> {
  return request<AdminKnowledgeTopic>({ url: '/admin/knowledge/topics', method: 'post', data });
}
export function updateAdminKnowledgeTopic(id: number, data: AdminKnowledgeTopic): Promise<AdminKnowledgeTopic> {
  return request<AdminKnowledgeTopic>({ url: `/admin/knowledge/topics/${id}`, method: 'put', data });
}
export function deleteAdminKnowledgeTopic(id: number): Promise<void> {
  return request<void>({ url: `/admin/knowledge/topics/${id}`, method: 'delete' });
}
