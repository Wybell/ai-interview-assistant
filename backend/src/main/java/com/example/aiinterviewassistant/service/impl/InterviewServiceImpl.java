package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.entity.AnswerRecord;
import com.example.aiinterviewassistant.dto.QuestionMode;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.AiService;
import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.KnowledgeRetrievalService;
import com.example.aiinterviewassistant.service.KnowledgeQuestionHistoryService;
import com.example.aiinterviewassistant.service.TechnicalTopicService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import com.example.aiinterviewassistant.model.KnowledgeContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final int MAX_KNOWLEDGE_QUESTION_ATTEMPTS = 2;

    private final RedisTemplate<String, String> redisTemplate;

    private final AiService aiService;

    private final AnswerRecordMapper answerRecordMapper;

    private final UserAiPreferenceService userAiPreferenceService;

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final KnowledgeQuestionHistoryService knowledgeQuestionHistoryService;

    private final TechnicalTopicService technicalTopicService;

    public InterviewServiceImpl(
            RedisTemplate<String, String> redisTemplate,
            AiService aiService,
            AnswerRecordMapper answerRecordMapper,
            UserAiPreferenceService userAiPreferenceService,
            KnowledgeRetrievalService knowledgeRetrievalService,
            TechnicalTopicService technicalTopicService,
            KnowledgeQuestionHistoryService knowledgeQuestionHistoryService) {
        this.redisTemplate = redisTemplate;
        this.aiService = aiService;
        this.answerRecordMapper = answerRecordMapper;
        this.userAiPreferenceService = userAiPreferenceService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.technicalTopicService = technicalTopicService;
        this.knowledgeQuestionHistoryService = knowledgeQuestionHistoryService;
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
        return askQuestion(userId, direction, language, tag, knowledgeTopicId,
                knowledgeTopicId == null ? QuestionMode.CUSTOM_TOPIC : QuestionMode.KNOWLEDGE_BASE,
                refresh);
    }

    @Override
    public String askQuestion(
            Long userId,
            String direction,
            String language,
            String tag,
            Long knowledgeTopicId,
            QuestionMode mode,
            boolean refresh) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        validateScope(direction, language);
        QuestionMode effectiveMode = mode == null ? QuestionMode.CUSTOM_TOPIC : mode;
        validateQuestionSource(direction, language, tag, knowledgeTopicId, effectiveMode);
        EffectiveAiModel aiModel = userAiPreferenceService.resolveEffectiveModel(userId);
        KnowledgeContext knowledgeContext = effectiveMode == QuestionMode.KNOWLEDGE_BASE
                ? knowledgeRetrievalService.getPublishedContext(knowledgeTopicId, direction, language)
                : null;
        String effectiveTag = knowledgeContext == null ? tag.trim() : knowledgeContext.title();
        String cacheKey = "question:" + userId + ":model:" + aiModel.id()
                + ":" + effectiveMode.name().toLowerCase() + ":" + direction + ":" + language + ":"
                + (knowledgeContext == null ? effectiveTag : "topic:v2:" + knowledgeContext.topicId());

        if (!refresh) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (effectiveMode == QuestionMode.KNOWLEDGE_BASE) {
                    knowledgeQuestionHistoryService.recordQuestion(
                            userId,
                            direction,
                            language,
                            knowledgeContext.topicId(),
                            cached
                    );
                }
                return cached;
            }
        }

        String questionText;
        if (effectiveMode == QuestionMode.KNOWLEDGE_BASE) {
            List<String> previousQuestions = knowledgeQuestionHistoryService.getPreviousQuestions(
                    userId, direction, language, knowledgeContext.topicId());
            questionText = generateKnowledgeQuestion(
                    aiModel, direction, language, effectiveTag, knowledgeContext, previousQuestions);
            knowledgeQuestionHistoryService.recordQuestion(
                    userId,
                    direction,
                    language,
                    knowledgeContext.topicId(),
                    questionText
            );
        } else {
            questionText = aiService.generateQuestion(
                    aiModel,
                    direction,
                    language,
                    effectiveTag,
                    knowledgeContext
            );
        }

        redisTemplate.opsForValue().set(
                cacheKey,
                questionText,
                Duration.ofHours(1)
        );

        return questionText;
    }

    private String generateKnowledgeQuestion(
            EffectiveAiModel aiModel,
            String direction,
            String language,
            String tag,
            KnowledgeContext knowledgeContext,
            List<String> previousQuestions) {
        String questionText = null;
        for (int attempt = 0; attempt < MAX_KNOWLEDGE_QUESTION_ATTEMPTS; attempt++) {
            questionText = aiService.generateQuestion(
                    aiModel,
                    direction,
                    language,
                    tag,
                    knowledgeContext,
                    previousQuestions
            );
            if (!containsQuestion(previousQuestions, questionText)) {
                return questionText;
            }
        }
        return questionText;
    }

    private boolean containsQuestion(List<String> previousQuestions, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String normalizedCandidate = normalizeQuestion(candidate);
        return previousQuestions.stream()
                .filter(question -> question != null && !question.isBlank())
                .map(this::normalizeQuestion)
                .anyMatch(normalizedCandidate::equals);
    }

    private String normalizeQuestion(String question) {
        return question.trim().replaceAll("\\s+", " ");
    }

    private void validateQuestionSource(
            String direction,
            String language,
            String tag,
            Long knowledgeTopicId,
            QuestionMode mode) {
        boolean hasTag = tag != null && !tag.isBlank();
        if (mode == QuestionMode.KNOWLEDGE_BASE) {
            if (knowledgeTopicId == null) {
                throw new BusinessException(400, "请选择有效的知识库专题");
            }
            return;
        }
        if (knowledgeTopicId != null) {
            throw new BusinessException(400, "当前出题模式不能使用知识库专题");
        }
        if (!hasTag || tag.trim().length() > 50) {
            throw new BusinessException(400, "知识点不能为空且不能超过50个字符");
        }
        if (mode == QuestionMode.TECHNICAL_TOPIC
                && !technicalTopicService.isSupported(direction, language, tag)) {
            throw new BusinessException(400, "当前方向和语言不支持该技术知识点");
        }
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
