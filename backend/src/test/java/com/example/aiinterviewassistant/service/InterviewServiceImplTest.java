package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.QuestionMode;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.entity.AnswerRecord;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.model.KnowledgeContext;
import com.example.aiinterviewassistant.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    private static final EffectiveAiModel DEEPSEEK_MODEL = new EffectiveAiModel(
            1L,
            "deepseek",
            "deepseek-v4-flash",
            "DeepSeek V4 Flash",
            true
    );

    private static final EffectiveAiModel CHANGE2PRO_MODEL = new EffectiveAiModel(
            2L,
            "change2proapi",
            "gpt-5.6-luna",
            "GPT-5.6 Luna",
            false
    );

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private AiService aiService;

    @Mock
    private AnswerRecordMapper answerRecordMapper;

    @Mock
    private UserAiPreferenceService userAiPreferenceService;

    @Mock
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Mock
    private TechnicalTopicService technicalTopicService;

    @Mock
    private KnowledgeQuestionHistoryService knowledgeQuestionHistoryService;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @Test
    void shouldReturnCachedQuestionWithoutCallingAi() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(DEEPSEEK_MODEL);
        when(valueOperations.get("question:1:model:1:custom_topic:backend:Java:Java")).thenReturn("cached question");

        String question = interviewService.askQuestion(1L, "Java", false);

        assertThat(question).isEqualTo("cached question");
        verifyNoInteractions(aiService, answerRecordMapper);
    }

    @Test
    void shouldRefreshQuestionAndStoreItInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(DEEPSEEK_MODEL);
        when(aiService.generateQuestion(DEEPSEEK_MODEL, "backend", "Java", "Java", null)).thenReturn("new question");

        String question = interviewService.askQuestion(1L, "Java", true);

        assertThat(question).isEqualTo("new question");
        verify(aiService).generateQuestion(DEEPSEEK_MODEL, "backend", "Java", "Java", null);
        verify(valueOperations).set(
                "question:1:model:1:custom_topic:backend:Java:Java",
                "new question",
                Duration.ofHours(1)
        );
    }

    @Test
    void shouldSaveAnswerRecordWhenScoringAnswer() {
        AiScoreResult scoreResult = new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        );
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(CHANGE2PRO_MODEL);
        when(aiService.scoreAnswer(CHANGE2PRO_MODEL, "What is JVM?", "My answer"))
                .thenReturn(scoreResult);

        AiScoreResult actual = interviewService.scoreAnswer(
                1L,
                "What is JVM?",
                "My answer",
                "Java"
        );

        assertThat(actual).isEqualTo(scoreResult);

        ArgumentCaptor<AnswerRecord> answerRecordCaptor =
                ArgumentCaptor.forClass(AnswerRecord.class);
        verify(answerRecordMapper).insert(answerRecordCaptor.capture());

        AnswerRecord savedAnswerRecord = answerRecordCaptor.getValue();
        assertThat(savedAnswerRecord.getUserId()).isEqualTo(1L);
        assertThat(savedAnswerRecord.getTag()).isEqualTo("Java");
        assertThat(savedAnswerRecord.getQuestion()).isEqualTo("What is JVM?");
        assertThat(savedAnswerRecord.getUserAnswer()).isEqualTo("My answer");
        assertThat(savedAnswerRecord.getScore()).isEqualTo(8);
        assertThat(savedAnswerRecord.getCorrectAnswer()).isEqualTo("standard answer");
        assertThat(savedAnswerRecord.getSuggestion()).isEqualTo("add more details");
        assertThat(savedAnswerRecord.getScoreAiModelId()).isEqualTo(2L);
        assertThat(savedAnswerRecord.getCreateTime()).isNotNull();
    }

    @Test
    void shouldSaveAnswerRecordAfterStreamingScoreAnswerCompletes() {
        AiScoreResult scoreResult = new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        );
        List<String> deltas = new ArrayList<>();
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(CHANGE2PRO_MODEL);
        when(aiService.streamScoreAnswer(
                eq(CHANGE2PRO_MODEL),
                eq("What is JVM?"),
                eq("My answer"),
                any(AiTextDeltaConsumer.class)
        )).thenAnswer(invocation -> {
            AiTextDeltaConsumer deltaConsumer = invocation.getArgument(3);
            deltaConsumer.onDelta("{\"score\":8}");
            return scoreResult;
        });

        AiScoreResult actual = interviewService.streamScoreAnswer(
                1L,
                "What is JVM?",
                "My answer",
                "Java",
                deltas::add
        );

        assertThat(actual).isEqualTo(scoreResult);
        assertThat(deltas).containsExactly("{\"score\":8}");

        ArgumentCaptor<AnswerRecord> answerRecordCaptor =
                ArgumentCaptor.forClass(AnswerRecord.class);
        verify(answerRecordMapper).insert(answerRecordCaptor.capture());
        assertThat(answerRecordCaptor.getValue().getScoreAiModelId()).isEqualTo(2L);
        assertThat(answerRecordCaptor.getValue().getScore()).isEqualTo(8);
    }

    @Test
    void shouldIsolateQuestionCacheAfterUserChangesModel() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L))
                .thenReturn(DEEPSEEK_MODEL, CHANGE2PRO_MODEL);
        when(valueOperations.get("question:1:model:1:custom_topic:backend:Java:Java")).thenReturn("DeepSeek question");
        when(aiService.generateQuestion(CHANGE2PRO_MODEL, "backend", "Java", "Java", null))
                .thenReturn("GPT question");

        String initialQuestion = interviewService.askQuestion(1L, "Java", false);
        String questionAfterSwitch = interviewService.askQuestion(1L, "Java", false);

        assertThat(initialQuestion).isEqualTo("DeepSeek question");
        assertThat(questionAfterSwitch).isEqualTo("GPT question");
        verify(valueOperations).set(
                "question:1:model:2:custom_topic:backend:Java:Java",
                "GPT question",
                Duration.ofHours(1)
        );
    }

    @Test
    void shouldRouteTwoUsersToTheirOwnModelsWithoutRestart() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(DEEPSEEK_MODEL);
        when(userAiPreferenceService.resolveEffectiveModel(2L)).thenReturn(CHANGE2PRO_MODEL);
        when(aiService.generateQuestion(DEEPSEEK_MODEL, "backend", "Java", "Java", null)).thenReturn("DeepSeek question");
        when(aiService.generateQuestion(CHANGE2PRO_MODEL, "backend", "Java", "Java", null)).thenReturn("GPT question");

        String firstUserQuestion = interviewService.askQuestion(1L, "Java", true);
        String secondUserQuestion = interviewService.askQuestion(2L, "Java", true);

        assertThat(firstUserQuestion).isEqualTo("DeepSeek question");
        assertThat(secondUserQuestion).isEqualTo("GPT question");
        verify(valueOperations).set(
                "question:1:model:1:custom_topic:backend:Java:Java",
                "DeepSeek question",
                Duration.ofHours(1)
        );
        verify(valueOperations).set(
                "question:2:model:2:custom_topic:backend:Java:Java",
                "GPT question",
                Duration.ofHours(1)
        );
    }

    @Test
    void shouldRejectRequestWhenUserIsNotLoggedIn() {
        assertThatThrownBy(() -> interviewService.askQuestion(null, "Java", false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先登录");

        verifyNoInteractions(redisTemplate, aiService, answerRecordMapper, userAiPreferenceService);
    }

    @Test
    void shouldGenerateTechnicalTopicQuestionWithTechnicalModeCacheKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(DEEPSEEK_MODEL);
        when(technicalTopicService.isSupported("backend", "Java", "JVM")).thenReturn(true);
        when(aiService.generateQuestion(DEEPSEEK_MODEL, "backend", "Java", "JVM", null))
                .thenReturn("JVM question");

        String question = interviewService.askQuestion(
                1L, "backend", "Java", "JVM", null, QuestionMode.TECHNICAL_TOPIC, true);

        assertThat(question).isEqualTo("JVM question");
        verify(valueOperations).set(
                "question:1:model:1:technical_topic:backend:Java:JVM",
                "JVM question",
                Duration.ofHours(1)
        );
    }

    @Test
    void shouldRejectKnowledgeModeWithoutTopic() {
        assertThatThrownBy(() -> interviewService.askQuestion(
                1L, "backend", "Java", "", null, QuestionMode.KNOWLEDGE_BASE, true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请选择有效的知识库专题");

        verifyNoInteractions(redisTemplate, aiService, userAiPreferenceService, knowledgeRetrievalService);
    }

    @Test
    void shouldRejectKnowledgeTopicInCustomMode() {
        assertThatThrownBy(() -> interviewService.askQuestion(
                1L, "backend", "Java", "JVM", 7L, QuestionMode.CUSTOM_TOPIC, true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前出题模式不能使用知识库专题");

        verifyNoInteractions(redisTemplate, aiService, userAiPreferenceService, knowledgeRetrievalService);
    }

    @Test
    void shouldUseAndRecordHistoryOnlyForKnowledgeBaseQuestions() {
        KnowledgeContext knowledgeContext = new KnowledgeContext(
                7L,
                "Java 基础专题",
                "HashMap 和 JVM 的文档内容"
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(DEEPSEEK_MODEL);
        when(knowledgeRetrievalService.getPublishedContext(7L, "backend", "Java"))
                .thenReturn(knowledgeContext);
        when(knowledgeQuestionHistoryService.getPreviousQuestions(1L, "backend", "Java", 7L))
                .thenReturn(List.of("已出过的问题？"));
        when(aiService.generateQuestion(
                DEEPSEEK_MODEL,
                "backend",
                "Java",
                "Java 基础专题",
                knowledgeContext,
                List.of("已出过的问题？")
        )).thenReturn("新的专题问题？");

        String question = interviewService.askQuestion(
                1L, "backend", "Java", "", 7L, QuestionMode.KNOWLEDGE_BASE, true);

        assertThat(question).isEqualTo("新的专题问题？");
        verify(knowledgeQuestionHistoryService).getPreviousQuestions(1L, "backend", "Java", 7L);
        verify(knowledgeQuestionHistoryService).recordQuestion(
                1L, "backend", "Java", 7L, "新的专题问题？");
        verify(valueOperations).set(
                "question:1:model:1:knowledge_base:backend:Java:topic:v2:7",
                "新的专题问题？",
                Duration.ofHours(1)
        );
    }

    @Test
    void shouldRetryWhenKnowledgeBaseModelRepeatsAQuestion() {
        KnowledgeContext knowledgeContext = new KnowledgeContext(
                7L,
                "Java 基础专题",
                "HashMap 和 JVM 的文档内容"
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userAiPreferenceService.resolveEffectiveModel(1L)).thenReturn(DEEPSEEK_MODEL);
        when(knowledgeRetrievalService.getPublishedContext(7L, "backend", "Java"))
                .thenReturn(knowledgeContext);
        when(knowledgeQuestionHistoryService.getPreviousQuestions(1L, "backend", "Java", 7L))
                .thenReturn(List.of("已出过的问题？"));
        when(aiService.generateQuestion(
                DEEPSEEK_MODEL,
                "backend",
                "Java",
                "Java 基础专题",
                knowledgeContext,
                List.of("已出过的问题？")
        )).thenReturn("已出过的问题？", "新的专题问题？");

        String question = interviewService.askQuestion(
                1L, "backend", "Java", "", 7L, QuestionMode.KNOWLEDGE_BASE, true);

        assertThat(question).isEqualTo("新的专题问题？");
        verify(aiService, org.mockito.Mockito.times(2)).generateQuestion(
                DEEPSEEK_MODEL,
                "backend",
                "Java",
                "Java 基础专题",
                knowledgeContext,
                List.of("已出过的问题？")
        );
    }
}
