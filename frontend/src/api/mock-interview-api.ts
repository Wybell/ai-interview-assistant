import { request } from '@/api/client';
import type { InterviewRound, MockInterviewSession, MockInterviewTurn } from '@/types/interview';

export function createMockInterview(payload: {
  resumeId: number;
  targetPosition: string;
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

export function finishMockInterview(sessionId: number): Promise<MockInterviewSession> {
  return request<MockInterviewSession>({
    url: `/mock-interviews/${sessionId}/finish`,
    method: 'post',
  });
}
