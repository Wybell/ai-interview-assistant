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

export type InterviewRound = 'FIRST' | 'SECOND' | 'THIRD' | 'HR';

export interface MockInterviewTurn {
  id: number;
  sequenceNo: number;
  turnType: 'MAIN' | 'FOLLOW_UP';
  parentTurnId: number | null;
  followUpNo: number | null;
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
  status: 'ACTIVE' | 'COMPLETED' | 'ENDED_EARLY';
  questionCount: number;
  questionLimit: number;
  aiModelId: number;
  summary: string | null;
  createTime: string;
  finishedTime: string | null;
  turns: MockInterviewTurn[];
}

export interface ActiveMockInterview {
  id: number;
  resumeId: number | null;
  resumeFileName: string | null;
  targetPosition: string;
  interviewRound: InterviewRound;
  questionCount: number;
  questionLimit: number;
  createTime: string;
}

export interface MockInterviewReviewSummary {
  sessionId: number;
  targetPosition: string;
  targetCompany: string | null;
  interviewRound: InterviewRound;
  status: 'COMPLETED' | 'ENDED_EARLY';
  resumeFileName: string | null;
  answeredTurnCount: number;
  mainQuestionCount: number;
  followUpCount: number;
  averageScore: number | null;
  reviewGenerated: boolean;
  reviewAvailable: boolean;
  finishedTime: string | null;
  createTime: string;
}

export interface MockInterviewReview extends Omit<MockInterviewReviewSummary, 'reviewGenerated' | 'reviewAvailable'> {
  overallFeedback: string;
  strengths: string;
  improvementAreas: string;
  actionItems: string;
  generatedTime: string;
  turns: MockInterviewTurn[];
}
