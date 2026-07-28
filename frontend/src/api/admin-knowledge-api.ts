import { request } from '@/api/client';

export interface KnowledgeDocument { id: number; title: string; sourceFileName?: string; direction: string; language: string; summary: string; }

export function getKnowledgeDocuments(direction: string, language: string): Promise<KnowledgeDocument[]> {
  return request<KnowledgeDocument[]>({ url: '/admin/knowledge/documents', method: 'get', params: { direction, language } });
}

export function uploadKnowledgeDocument(direction: string, language: string, file: File): Promise<KnowledgeDocument> {
  const data = new FormData(); data.append('direction', direction); data.append('language', language); data.append('file', file);
  return request<KnowledgeDocument>({ url: '/admin/knowledge/documents', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } });
}

export function deleteKnowledgeDocument(id: number): Promise<void> {
  return request<void>({ url: `/admin/knowledge/documents/${id}`, method: 'delete' });
}
