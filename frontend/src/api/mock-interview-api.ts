import { request } from '@/api/client';
import type {
  ActiveMockInterview,
  InterviewRound,
  MockInterviewReview,
  MockInterviewReviewSummary,
  MockInterviewSession,
  MockInterviewTurn,
} from '@/types/interview';

export function createMockInterview(payload: {
  resumeId: number;
  targetPosition: string;
  targetCompany?: string;
  interviewRound: InterviewRound;
}): Promise<MockInterviewSession> {
  return request<MockInterviewSession>({ url: '/mock-interviews', method: 'post', data: payload });
}

export function answerMockInterviewTurn(
  sessionId: number,
  turnId: number,
  answer: string,
): Promise<MockInterviewTurn> {
  return request<MockInterviewTurn>({
    url: `/mock-interviews/${sessionId}/turns/${turnId}/answer`,
    method: 'post',
    data: { answer },
  });
}

export function getNextMockInterviewQuestion(sessionId: number): Promise<MockInterviewTurn> {
  return request<MockInterviewTurn>({
    url: `/mock-interviews/${sessionId}/questions`,
    method: 'post',
  });
}

export function getFollowUpMockInterviewQuestion(
  sessionId: number,
  turnId: number,
): Promise<MockInterviewTurn> {
  return request<MockInterviewTurn>({
    url: `/mock-interviews/${sessionId}/turns/${turnId}/follow-up`,
    method: 'post',
  });
}

export function finishMockInterview(sessionId: number): Promise<MockInterviewSession> {
  return request<MockInterviewSession>({
    url: `/mock-interviews/${sessionId}/finish`,
    method: 'post',
  });
}

export function getMockInterviewSession(sessionId: number): Promise<MockInterviewSession> {
  return request<MockInterviewSession>({ url: `/mock-interviews/${sessionId}`, method: 'get' });
}

export function getActiveMockInterviews(): Promise<ActiveMockInterview[]> {
  return request<ActiveMockInterview[]>({ url: '/mock-interviews/active', method: 'get' });
}

export function endMockInterviewEarly(sessionId: number): Promise<MockInterviewSession> {
  return request<MockInterviewSession>({
    url: `/mock-interviews/${sessionId}/end`,
    method: 'post',
  });
}

export function getMockInterviewReviews(): Promise<MockInterviewReviewSummary[]> {
  return request<MockInterviewReviewSummary[]>({ url: '/mock-interviews/reviews', method: 'get' });
}

export function getMockInterviewReview(sessionId: number): Promise<MockInterviewReview> {
  return request<MockInterviewReview>({ url: `/mock-interviews/${sessionId}/review`, method: 'get' });
}

export function generateMockInterviewReview(sessionId: number): Promise<MockInterviewReview> {
  return request<MockInterviewReview>({ url: `/mock-interviews/${sessionId}/review`, method: 'post' });
}
