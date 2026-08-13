package com.example.aiinterviewassistant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MockInterviewReviewResponse(
        Long sessionId,
        String targetPosition,
        String targetCompany,
        String interviewRound,
        String status,
        String resumeFileName,
        int answeredTurnCount,
        int mainQuestionCount,
        int followUpCount,
        BigDecimal averageScore,
        String overallFeedback,
        String strengths,
        String improvementAreas,
        String actionItems,
        LocalDateTime finishedTime,
        LocalDateTime generatedTime,
        List<MockInterviewTurnResponse> turns) {
}
