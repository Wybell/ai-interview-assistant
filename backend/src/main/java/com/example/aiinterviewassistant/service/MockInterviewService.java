package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.MockInterviewSessionResponse;
import com.example.aiinterviewassistant.dto.MockInterviewTurnResponse;
import com.example.aiinterviewassistant.dto.ActiveMockInterviewResponse;

import java.util.List;

public interface MockInterviewService {

    MockInterviewSessionResponse createSession(
            Long userId,
            Long resumeId,
            String targetPosition,
            String targetCompany,
            String interviewRound);

    MockInterviewSessionResponse getSession(Long userId, Long sessionId);

    List<ActiveMockInterviewResponse> getActiveSessions(Long userId);

    MockInterviewTurnResponse answerTurn(Long userId, Long sessionId, Long turnId, String answer);

    MockInterviewTurnResponse generateNextQuestion(Long userId, Long sessionId);

    MockInterviewTurnResponse generateFollowUpQuestion(Long userId, Long sessionId, Long turnId);

    MockInterviewSessionResponse finishSession(Long userId, Long sessionId);

    MockInterviewSessionResponse endSessionEarly(Long userId, Long sessionId);
}
