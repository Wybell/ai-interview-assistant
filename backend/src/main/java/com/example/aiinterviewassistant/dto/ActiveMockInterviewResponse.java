package com.example.aiinterviewassistant.dto;

import java.time.LocalDateTime;

public record ActiveMockInterviewResponse(
        Long id,
        Long resumeId,
        String resumeFileName,
        String targetPosition,
        String interviewRound,
        int questionCount,
        int questionLimit,
        LocalDateTime createTime) {
}
