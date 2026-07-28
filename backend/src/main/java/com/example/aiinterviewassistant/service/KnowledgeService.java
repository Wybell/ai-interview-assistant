package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.KnowledgeTopicResponse;

import java.util.List;

public interface KnowledgeService {
    List<KnowledgeTopicResponse> getTopics(String direction, String language);

    KnowledgeTopicResponse getTopic(Long topicId);
}
