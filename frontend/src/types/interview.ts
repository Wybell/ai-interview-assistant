export interface AiModel {
  id: number;
  provider: string;
  modelCode: string;
  displayName: string;
  defaultModel: boolean;
  selected: boolean;
}

export interface AiModelPreference {
  modelId: number;
  provider: string;
  modelCode: string;
  displayName: string;
  defaultSelection: boolean;
}

export interface AiScoreResult {
  score: number;
  correctAnswer: string;
  suggestion: string;
}

export interface MistakeRecord {
  id: number;
  tag: string;
  question: string;
  userAnswer: string;
  score: number;
  correctAnswer: string;
  suggestion: string;
  createTime: string;
}

export interface StudyProgress {
  tag: string;
  totalCount: number;
  avgScore: number;
}

export interface QuestionRequest {
  tag: string;
  refresh: boolean;
}

export interface ScoreRequest {
  tag: string;
  question: string;
  answer: string;
}

export type ScoreStatus = 'idle' | 'streaming' | 'complete' | 'error' | 'cancelled';
