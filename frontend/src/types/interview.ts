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
  direction: 'frontend' | 'backend';
  language: string;
  tag: string;
  knowledgeTopicId?: number;
  mode: 'KNOWLEDGE_BASE' | 'CUSTOM_TOPIC' | 'TECHNICAL_TOPIC';
  refresh: boolean;
}

export interface ScoreRequest {
  tag: string;
  question: string;
  answer: string;
}

export type ScoreStatus = 'idle' | 'streaming' | 'complete' | 'error' | 'cancelled';

export interface QuestionHistoryEntry {
  question: string;
  answer: string;
  scoreStatus: ScoreStatus;
  scoreResult: AiScoreResult | null;
  scoreError: string;
  streamText: string;
}

export interface ResumeDocument {
  id: number;
  fileName: string;
  contentType: string;
  createTime: string;
}

export interface ResumePreview {
  id: number;
  fileName: string;
  contentType: string;
  content: string;
}

export type InterviewRound = 'FIRST' | 'SECOND' | 'THIRD';

export interface MockInterviewTurn {
  id: number;
  sequenceNo: number;
  question: string;
  userAnswer: string | null;
  score: number | null;
  correctAnswer: string | null;
  suggestion: string | null;
  createTime: string;
}

export interface MockInterviewSession {
  id: number;
  targetPosition: string;
  targetCompany: string | null;
  interviewRound: InterviewRound;
  status: 'ACTIVE' | 'COMPLETED';
  questionCount: number;
  aiModelId: number;
  summary: string | null;
  createTime: string;
  finishedTime: string | null;
  turns: MockInterviewTurn[];
}
