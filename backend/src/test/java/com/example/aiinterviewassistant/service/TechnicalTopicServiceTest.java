package com.example.aiinterviewassistant.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalTopicServiceTest {

    private final TechnicalTopicService technicalTopicService = new TechnicalTopicService();

    @Test
    void shouldReturnTopicsForSupportedDirectionAndLanguage() {
        assertThat(technicalTopicService.getTopics("backend", "Java"))
                .containsExactly("集合框架", "并发编程", "JVM", "Spring", "MySQL", "Redis");
    }

    @Test
    void shouldRejectUnknownScopeOrTopic() {
        assertThat(technicalTopicService.getTopics("frontend", "Java")).isEmpty();
        assertThat(technicalTopicService.isSupported("backend", "Java", "JVM")).isTrue();
        assertThat(technicalTopicService.isSupported("frontend", "Java", "JVM")).isFalse();
        assertThat(technicalTopicService.isSupported("backend", "Java", "不存在的板块")).isFalse();
    }
}
