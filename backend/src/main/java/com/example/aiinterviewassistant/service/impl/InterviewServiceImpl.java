package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.entity.AnswerRecord;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.AiService;
import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.KnowledgeRetrievalService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import com.example.aiinterviewassistant.model.KnowledgeContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final RedisTemplate<String, String> redisTemplate;

    private final AiService aiService;

    private final AnswerRecordMapper answerRecordMapper;

    private final UserAiPreferenceService userAiPreferenceService;

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public InterviewServiceImpl(
            RedisTemplate<String, String> redisTemplate,
            AiService aiService,
            AnswerRecordMapper answerRecordMapper,
            UserAiPreferenceService userAiPreferenceService,
            KnowledgeRetrievalService knowledgeRetrievalService) {
        this.redisTemplate = redisTemplate;
        this.aiService = aiService;
        this.answerRecordMapper = answerRecordMapper;
        this.userAiPreferenceService = userAiPreferenceService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public String askQuestion(Long userId, String direction, String language, String tag, boolean refresh) {
        return askQuestion(userId, direction, language, tag, null, refresh);
    }

    @Override
    public String askQuestion(
            Long userId,
            String direction,
            String language,
            String tag,
            Long knowledgeTopicId,
            boolean refresh) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        validateScope(direction, language);
        EffectiveAiModel aiModel = userAiPreferenceService.resolveEffectiveModel(userId);
        KnowledgeContext knowledgeContext = knowledgeTopicId == null
                ? null
                : knowledgeRetrievalService.getPublishedContext(knowledgeTopicId, direction, language);
        String effectiveTag = knowledgeContext == null ? tag : knowledgeContext.title();
        String cacheKey = "question:" + userId + ":model:" + aiModel.id()
                + ":" + direction + ":" + language + ":"
                + (knowledgeContext == null ? "custom:" + tag : "knowledge:" + knowledgeContext.topicId());

        if (!refresh) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        String questionText = aiService.generateQuestion(aiModel, direction, language, effectiveTag, knowledgeContext);

        redisTemplate.opsForValue().set(
                cacheKey,
                questionText,
                Duration.ofHours(1)
        );

        return questionText;
    }

    @Override
    public String askQuestion(Long userId, String tag, boolean refresh) {
        return askQuestion(userId, "backend", "Java", tag, refresh);
    }

    private void validateScope(String direction, String language) {
        if ("frontend".equals(direction)
                && java.util.Set.of("JavaScript", "TypeScript", "Vue", "React").contains(language)) {
            return;
        }
        if ("backend".equals(direction)
                && java.util.Set.of("Java", "Python", "Go", "C#", "Node.js", "TypeScript").contains(language)) {
            return;
        }
        throw new BusinessException(400, "面试方向和语言组合不受支持");
    }

    @Override
    public AiScoreResult scoreAnswer(
            Long userId,
            String question,
            String userAnswer,
            String tag) {
        EffectiveAiModel aiModel = resolveAiModel(userId);
        AiScoreResult scoreResult = aiService.scoreAnswer(
                aiModel,
                question,
                userAnswer
        );

        saveAnswerRecord(userId, question, userAnswer, tag, aiModel, scoreResult);
        return scoreResult;
    }

    @Override
    public AiScoreResult streamScoreAnswer(
            Long userId,
            String question,
            String userAnswer,
            String tag,
            AiTextDeltaConsumer deltaConsumer) {
        EffectiveAiModel aiModel = resolveAiModel(userId);
        AiScoreResult scoreResult = aiService.streamScoreAnswer(
                aiModel,
                question,
                userAnswer,
                deltaConsumer
        );

        saveAnswerRecord(userId, question, userAnswer, tag, aiModel, scoreResult);
        return scoreResult;
    }

    private EffectiveAiModel resolveAiModel(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        return userAiPreferenceService.resolveEffectiveModel(userId);
    }

    private void saveAnswerRecord(
            Long userId,
            String question,
            String userAnswer,
            String tag,
            EffectiveAiModel aiModel,
            AiScoreResult scoreResult) {
        AnswerRecord answerRecord = new AnswerRecord();
        answerRecord.setTag(tag);
        answerRecord.setQuestion(question);
        answerRecord.setUserAnswer(userAnswer);
        answerRecord.setScore(scoreResult.getScore());
        answerRecord.setCorrectAnswer(scoreResult.getCorrectAnswer());
        answerRecord.setSuggestion(scoreResult.getSuggestion());
        answerRecord.setScoreAiModelId(aiModel.id());
        answerRecord.setCreateTime(LocalDateTime.now());
        answerRecord.setUserId(userId);

        answerRecordMapper.insert(answerRecord);
    }
}
