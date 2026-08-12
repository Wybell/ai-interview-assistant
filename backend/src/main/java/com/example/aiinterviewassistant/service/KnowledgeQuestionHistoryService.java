package com.example.aiinterviewassistant.service;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Keeps a bounded per-user history for knowledge-base practice questions.
 */
@Service
public class KnowledgeQuestionHistoryService {

    private static final long MAX_HISTORY_SIZE = 50;
    private static final Duration HISTORY_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redisTemplate;

    public KnowledgeQuestionHistoryService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<String> getPreviousQuestions(
            Long userId,
            String direction,
            String language,
            Long topicId) {
        List<String> questions = redisTemplate.opsForList().range(
                buildKey(userId, direction, language, topicId),
                -MAX_HISTORY_SIZE,
                -1
        );
        return questions == null ? List.of() : List.copyOf(questions);
    }

    public void recordQuestion(
            Long userId,
            String direction,
            String language,
            Long topicId,
            String question) {
        if (question == null || question.isBlank()) {
            return;
        }

        String normalizedQuestion = question.trim();
        String key = buildKey(userId, direction, language, topicId);
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        List<String> previousQuestions = listOperations.range(key, -MAX_HISTORY_SIZE, -1);
        if (previousQuestions == null || !previousQuestions.contains(normalizedQuestion)) {
            listOperations.rightPush(key, normalizedQuestion);
            listOperations.trim(key, -MAX_HISTORY_SIZE, -1);
        }
        redisTemplate.expire(key, HISTORY_TTL);
    }

    private String buildKey(Long userId, String direction, String language, Long topicId) {
        return "question-history:" + userId + ":" + direction + ":" + language + ":" + topicId;
    }
}
