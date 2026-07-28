package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiinterviewassistant.dto.KnowledgeTopicResponse;
import com.example.aiinterviewassistant.entity.KnowledgeQuestion;
import com.example.aiinterviewassistant.entity.KnowledgeTopic;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.KnowledgeQuestionMapper;
import com.example.aiinterviewassistant.mapper.KnowledgeTopicMapper;
import com.example.aiinterviewassistant.service.KnowledgeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeTopicMapper topicMapper;
    private final KnowledgeQuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    public KnowledgeServiceImpl(KnowledgeTopicMapper topicMapper, KnowledgeQuestionMapper questionMapper,
                                ObjectMapper objectMapper) {
        this.topicMapper = topicMapper;
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<KnowledgeTopicResponse> getTopics(String direction, String language) {
        List<KnowledgeTopic> topics = topicMapper.selectList(new LambdaQueryWrapper<KnowledgeTopic>()
                .eq(KnowledgeTopic::getDirection, direction).eq(KnowledgeTopic::getLanguage, language)
                .eq(KnowledgeTopic::getPublished, 1).orderByAsc(KnowledgeTopic::getSortOrder));
        return topics.stream().map(this::toResponse).toList();
    }

    @Override
    public KnowledgeTopicResponse getTopic(Long topicId) {
        KnowledgeTopic topic = topicMapper.selectById(topicId);
        if (topic == null || !Integer.valueOf(1).equals(topic.getPublished())) {
            throw new BusinessException(404, "知识专题不存在");
        }
        return toResponse(topic);
    }

    private KnowledgeTopicResponse toResponse(KnowledgeTopic topic) {
        List<String> keyPoints;
        try {
            keyPoints = objectMapper.readValue(topic.getKeyPoints(), new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "知识专题内容格式错误");
        }
        List<KnowledgeQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<KnowledgeQuestion>()
                .eq(KnowledgeQuestion::getTopicId, topic.getId()).orderByAsc(KnowledgeQuestion::getSortOrder));
        return new KnowledgeTopicResponse(topic.getId(), topic.getDirection(), topic.getLanguage(), topic.getCategory(),
                topic.getTitle(), topic.getSummary(), keyPoints,
                questions.stream().map(item -> new KnowledgeTopicResponse.KnowledgeQuestion(item.getQuestion(), item.getAnswer())).toList());
    }
}
