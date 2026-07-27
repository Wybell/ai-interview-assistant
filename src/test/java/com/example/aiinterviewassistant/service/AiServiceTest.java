package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    private static final EffectiveAiModel SELECTED_MODEL = new EffectiveAiModel(
            2L,
            "change2proapi",
            "gpt-5.6-luna",
            "GPT-5.6 Luna",
            false
    );

    @Mock
    private AiClientRegistry aiClientRegistry;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(aiClientRegistry, new ObjectMapper());
    }

    @Test
    void shouldGenerateQuestionThroughSelectedRuntimeModel() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                eq("知识点：JVM")
        ))
                .thenReturn("Explain JVM memory areas.");

        String question = aiService.generateQuestion(SELECTED_MODEL, "JVM");

        assertThat(question).isEqualTo("Explain JVM memory areas.");
        verify(aiClientRegistry).generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                eq("知识点：JVM")
        );
    }

    @Test
    void shouldParseValidScoreResultThroughSelectedRuntimeModel() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString()
        ))
                .thenReturn(
                        "{\"score\":8,\"correct_answer\":\"standard answer\",\"suggestion\":\"add more details\"}"
                );

        AiScoreResult result = aiService.scoreAnswer(SELECTED_MODEL, "What is JVM?", "My answer");

        assertThat(result).isEqualTo(new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        ));
    }

    @Test
    void shouldParseScoreResultWrappedInMarkdownCodeFence() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString()
        )).thenReturn("""
                ```json
                {"score":8,"correct_answer":"standard {answer}","suggestion":"add more details"}
                ```
                """);

        AiScoreResult result = aiService.scoreAnswer(SELECTED_MODEL, "What is JVM?", "My answer");

        assertThat(result).isEqualTo(new AiScoreResult(
                8,
                "standard {answer}",
                "add more details"
        ));
    }

    @Test
    void shouldParseScoreResultWithLeadingAndTrailingProse() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString()
        )).thenReturn("Score result:\n"
                + "{\"score\":8,\"correct_answer\":\"standard answer\",\"suggestion\":\"add more details\"}\n"
                + "Use this feedback for practice.");

        AiScoreResult result = aiService.scoreAnswer(SELECTED_MODEL, "What is JVM?", "My answer");

        assertThat(result).isEqualTo(new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        ));
    }

    @Test
    void shouldParseAndForwardStreamingScoreResultThroughSelectedRuntimeModel() {
        List<String> deltas = new ArrayList<>();
        when(aiClientRegistry.generateStream(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString(),
                any(AiTextDeltaConsumer.class)
        )).thenAnswer(invocation -> {
            AiTextDeltaConsumer deltaConsumer = invocation.getArgument(4);
            deltaConsumer.onDelta("{\"score\":8,");
            deltaConsumer.onDelta("\"correct_answer\":\"standard answer\",\"suggestion\":\"add more details\"}");
            return "{\"score\":8,\"correct_answer\":\"standard answer\",\"suggestion\":\"add more details\"}";
        });

        AiScoreResult result = aiService.streamScoreAnswer(
                SELECTED_MODEL,
                "What is JVM?",
                "My answer",
                deltas::add
        );

        assertThat(result).isEqualTo(new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        ));
        assertThat(deltas).containsExactly(
                "{\"score\":8,",
                "\"correct_answer\":\"standard answer\",\"suggestion\":\"add more details\"}"
        );
    }

    @Test
    void shouldRejectAiScoreOutsideAllowedRange() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString()
        ))
                .thenReturn(
                        "{\"score\":11,\"correct_answer\":\"standard answer\",\"suggestion\":\"add more details\"}"
                );

        assertThatThrownBy(() -> aiService.scoreAnswer(SELECTED_MODEL, "What is JVM?", "My answer"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 评分结果超出范围");
    }

    @Test
    void shouldRejectAiScoreWithMissingFields() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString()
        ))
                .thenReturn("{\"score\":8}");

        assertThatThrownBy(() -> aiService.scoreAnswer(SELECTED_MODEL, "What is JVM?", "My answer"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 评分结果格式错误");
    }

    @Test
    void shouldRejectMalformedAiScoreResult() {
        when(aiClientRegistry.generate(
                eq("change2proapi"),
                eq("gpt-5.6-luna"),
                anyString(),
                anyString()
        )).thenReturn("Score result: {\"score\":8");

        assertThatThrownBy(() -> aiService.scoreAnswer(SELECTED_MODEL, "What is JVM?", "My answer"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 评分结果解析失败");
    }
}
