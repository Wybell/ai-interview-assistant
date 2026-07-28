export interface KnowledgeQuestion {
  question: string;
  answer: string;
}

export interface KnowledgeTopic {
  id: number;
  direction: 'frontend' | 'backend';
  language: string;
  category: string;
  title: string;
  summary: string;
  keyPoints: string[];
  questions: KnowledgeQuestion[];
}
