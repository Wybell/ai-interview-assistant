package com.example.aiinterviewassistant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MockInterviewReviewSummaryResponse(
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
        boolean reviewGenerated,
        boolean reviewAvailable,
        LocalDateTime finishedTime,
        LocalDateTime createTime) {
}
