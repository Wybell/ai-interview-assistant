import { request } from '@/api/client';
import type { KnowledgeTopic } from '@/types/knowledge';

export function getKnowledgeTopics(direction: string, language: string): Promise<KnowledgeTopic[]> {
  return request<KnowledgeTopic[]>({ url: '/knowledge/topics', method: 'get', params: { direction, language } });
}

export function getKnowledgeTopic(topicId: number): Promise<KnowledgeTopic> {
  return request<KnowledgeTopic>({ url: `/knowledge/topics/${topicId}`, method: 'get' });
}
