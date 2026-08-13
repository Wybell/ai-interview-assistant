package com.example.aiinterviewassistant.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MockInterviewSessionResponse(
        Long id,
        String targetPosition,
        String targetCompany,
        String interviewRound,
        String status,
        int questionCount,
        int questionLimit,
        Long aiModelId,
        String summary,
        LocalDateTime createTime,
        LocalDateTime finishedTime,
        List<MockInterviewTurnResponse> turns) {
}
