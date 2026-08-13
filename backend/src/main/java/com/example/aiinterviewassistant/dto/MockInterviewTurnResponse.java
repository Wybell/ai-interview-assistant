package com.example.aiinterviewassistant.dto;

import java.time.LocalDateTime;

public record MockInterviewTurnResponse(
        Long id,
        int sequenceNo,
        String turnType,
        Long parentTurnId,
        Integer followUpNo,
        String question,
        String userAnswer,
        Integer score,
        String correctAnswer,
        String suggestion,
        LocalDateTime createTime) {
}
