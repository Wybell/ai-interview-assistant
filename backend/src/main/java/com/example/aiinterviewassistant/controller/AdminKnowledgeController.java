package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.KnowledgeTopicRequest;
import com.example.aiinterviewassistant.dto.KnowledgeTopicResponse;
import com.example.aiinterviewassistant.entity.KnowledgeQuestion;
import com.example.aiinterviewassistant.entity.KnowledgeTopic;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.KnowledgeQuestionMapper;
import com.example.aiinterviewassistant.mapper.KnowledgeTopicMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge")
public class AdminKnowledgeController {
    private final KnowledgeTopicMapper topicMapper;
    private final KnowledgeQuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    public AdminKnowledgeController(KnowledgeTopicMapper topicMapper, KnowledgeQuestionMapper questionMapper,
                                    ObjectMapper objectMapper) {
        this.topicMapper = topicMapper;
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/topics")
    public ApiResponse<List<KnowledgeTopic>> getAllTopics(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.success(topicMapper.selectList(null));
    }

    @PostMapping("/topics")
    public ApiResponse<KnowledgeTopic> create(@Valid @RequestBody KnowledgeTopicRequest request,
                                              Authentication authentication) {
        requireAdmin(authentication);
        KnowledgeTopic topic = new KnowledgeTopic();
        copyTopic(topic, request);
        topicMapper.insert(topic);
        saveQuestions(topic.getId(), request.questions());
        return ApiResponse.success(topic);
    }

    @PutMapping("/topics/{id}")
    public ApiResponse<KnowledgeTopic> update(@PathVariable Long id, @Valid @RequestBody KnowledgeTopicRequest request,
                                              Authentication authentication) {
        requireAdmin(authentication);
        KnowledgeTopic topic = topicMapper.selectById(id);
        if (topic == null) throw new BusinessException(404, "知识专题不存在");
        copyTopic(topic, request);
        topicMapper.updateById(topic);
        questionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KnowledgeQuestion>().eq("topic_id", id));
        saveQuestions(id, request.questions());
        return ApiResponse.success(topic);
    }

    @DeleteMapping("/topics/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        topicMapper.deleteById(id);
        return ApiResponse.success(null);
    }

    private void copyTopic(KnowledgeTopic topic, KnowledgeTopicRequest request) {
        topic.setDirection(request.direction()); topic.setLanguage(request.language()); topic.setCategory(request.category());
        topic.setTitle(request.title()); topic.setSummary(request.summary()); topic.setPublished(Boolean.FALSE.equals(request.published()) ? 0 : 1);
        try { topic.setKeyPoints(objectMapper.writeValueAsString(request.keyPoints())); }
        catch (JsonProcessingException exception) { throw new BusinessException(400, "核心要点格式错误"); }
    }

    private void saveQuestions(Long topicId, List<KnowledgeTopicRequest.KnowledgeQuestionRequest> questions) {
        for (int index = 0; index < questions.size(); index++) {
            KnowledgeTopicRequest.KnowledgeQuestionRequest request = questions.get(index);
            KnowledgeQuestion question = new KnowledgeQuestion(); question.setTopicId(topicId); question.setQuestion(request.question());
            question.setAnswer(request.answer()); question.setDifficulty(request.difficulty() == null ? "中级" : request.difficulty()); question.setSortOrder(index);
            questionMapper.insert(question);
        }
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(item -> "ROLE_ADMIN".equals(item.getAuthority()))) {
            throw new BusinessException(403, "只有管理员可以管理知识库");
        }
    }
}
