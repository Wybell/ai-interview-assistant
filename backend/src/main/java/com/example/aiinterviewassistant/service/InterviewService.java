package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.QuestionMode;

public interface InterviewService {

    String askQuestion(Long userId, String direction, String language, String tag, boolean refresh);

    String askQuestion(Long userId, String direction, String language, String tag, Long knowledgeTopicId, boolean refresh);

    String askQuestion(Long userId, String direction, String language, String tag, Long knowledgeTopicId,
                       QuestionMode mode, boolean refresh);

    default String askQuestion(Long userId, String tag, boolean refresh) {
        return askQuestion(userId, "backend", "Java", tag, refresh);
    }

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
