package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.dto.AiScoreResult;

public interface InterviewService {

    String askQuestion(Long userId, String tag, boolean refresh);

    AiScoreResult scoreAnswer(
            Long userId,
            String question,
            String userAnswer,
            String tag
    );

    AiScoreResult streamScoreAnswer(
            Long userId,
            String question,
            String userAnswer,
            String tag,
            AiTextDeltaConsumer deltaConsumer
    );
}
