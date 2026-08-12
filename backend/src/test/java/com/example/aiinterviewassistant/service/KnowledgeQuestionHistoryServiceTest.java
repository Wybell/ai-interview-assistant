package com.example.aiinterviewassistant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeQuestionHistoryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Test
    void shouldReadTopicHistoryFromUserScopedRedisList() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("question-history:1:backend:Java:7", -50, -1))
                .thenReturn(List.of("第一题？", "第二题？"));

        KnowledgeQuestionHistoryService service = new KnowledgeQuestionHistoryService(redisTemplate);

        assertThat(service.getPreviousQuestions(1L, "backend", "Java", 7L))
                .containsExactly("第一题？", "第二题？");
    }

    @Test
    void shouldAppendNewQuestionAndRefreshBoundedHistoryTtl() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("question-history:1:backend:Java:7", -50, -1))
                .thenReturn(List.of("旧题？"));

        KnowledgeQuestionHistoryService service = new KnowledgeQuestionHistoryService(redisTemplate);

        service.recordQuestion(1L, "backend", "Java", 7L, " 新题？ ");

        verify(listOperations).rightPush("question-history:1:backend:Java:7", "新题？");
        verify(listOperations).trim("question-history:1:backend:Java:7", -50, -1);
        verify(redisTemplate).expire("question-history:1:backend:Java:7", Duration.ofDays(30));
    }

    @Test
    void shouldNotDuplicateQuestionInHistory() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("question-history:1:backend:Java:7", -50, -1))
                .thenReturn(List.of("同一道题？"));

        KnowledgeQuestionHistoryService service = new KnowledgeQuestionHistoryService(redisTemplate);

        service.recordQuestion(1L, "backend", "Java", 7L, "同一道题？");

        verify(redisTemplate).expire("question-history:1:backend:Java:7", Duration.ofDays(30));
    }
}
