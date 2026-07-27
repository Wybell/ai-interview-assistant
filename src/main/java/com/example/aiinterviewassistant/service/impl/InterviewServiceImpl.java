package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.entity.AnswerRecord;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.AiService;
import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
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

    public InterviewServiceImpl(
            RedisTemplate<String, String> redisTemplate,
            AiService aiService,
            AnswerRecordMapper answerRecordMapper,
            UserAiPreferenceService userAiPreferenceService) {
        this.redisTemplate = redisTemplate;
        this.aiService = aiService;
        this.answerRecordMapper = answerRecordMapper;
        this.userAiPreferenceService = userAiPreferenceService;
    }

    @Override
    public String askQuestion(Long userId, String tag, boolean refresh) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        EffectiveAiModel aiModel = userAiPreferenceService.resolveEffectiveModel(userId);
        String cacheKey = "question:" + userId + ":model:" + aiModel.id() + ":" + tag;

        if (!refresh) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        String questionText = aiService.generateQuestion(aiModel, tag);

        redisTemplate.opsForValue().set(
                cacheKey,
                questionText,
                Duration.ofHours(1)
        );

        return questionText;
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
