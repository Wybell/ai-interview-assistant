package com.example.aiinterviewassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiinterviewassistant.entity.KnowledgeTopic;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.KnowledgeTopicMapper;
import com.example.aiinterviewassistant.model.KnowledgeContext;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {

    private static final int MAX_CONTEXT_LENGTH = 8_000;

    private final KnowledgeTopicMapper knowledgeTopicMapper;

    public KnowledgeRetrievalService(KnowledgeTopicMapper knowledgeTopicMapper) {
        this.knowledgeTopicMapper = knowledgeTopicMapper;
    }

    public KnowledgeContext getPublishedContext(Long topicId, String direction, String language) {
        KnowledgeTopic topic = knowledgeTopicMapper.selectOne(new LambdaQueryWrapper<KnowledgeTopic>()
                .eq(KnowledgeTopic::getId, topicId)
                .eq(KnowledgeTopic::getDirection, direction)
                .eq(KnowledgeTopic::getLanguage, language)
                .eq(KnowledgeTopic::getPublished, 1));
        if (topic == null) {
            throw new BusinessException(404, "Knowledge topic is unavailable");
        }

        String content = topic.getDocumentContent();
        if (content == null || content.isBlank()) {
            content = topic.getSummary();
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(409, "Knowledge topic has no usable content");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTEXT_LENGTH) {
            normalized = normalized.substring(0, MAX_CONTEXT_LENGTH);
        }
        return new KnowledgeContext(topic.getId(), topic.getTitle(), normalized);
    }
}
