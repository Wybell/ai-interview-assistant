package com.example.aiinterviewassistant.sse;

import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.config.SseProperties;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.service.InterviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewScoreSseAdapterTest {

    @Mock
    private AsyncTaskExecutor sseTaskExecutor;

    @Mock
    private InterviewService interviewService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SseProperties sseProperties;

    @InjectMocks
    private InterviewScoreSseAdapter sseAdapter;

    @Test
    void shouldReturnCompletedEmitterWhenUserIsNotLoggedIn() {
        SseEmitter emitter = sseAdapter.streamScore(
                null,
                "What is JVM?",
                "My answer",
                "Java"
        );

        assertThat(emitter).isNotNull();
        verifyNoInteractions(sseTaskExecutor, interviewService, objectMapper, sseProperties);
    }

    @Test
    void shouldForwardRealScoreDeltasAndSendCompletionEvent() throws Exception {
        AiScoreResult scoreResult = new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        );
        AtomicReference<Runnable> submittedTask = new AtomicReference<>();
        when(sseProperties.getScoreTimeoutMillis()).thenReturn(45_000L);
        when(interviewService.streamScoreAnswer(
                eq(1L),
                eq("What is JVM?"),
                eq("My answer"),
                eq("Java"),
                any(AiTextDeltaConsumer.class)
        )).thenAnswer(invocation -> {
            AiTextDeltaConsumer deltaConsumer = invocation.getArgument(4);
            deltaConsumer.onDelta("{\"score\":");
            deltaConsumer.onDelta("8}");
            return scoreResult;
        });
        when(objectMapper.writeValueAsString(scoreResult)).thenReturn("{}");
        doAnswer(invocation -> {
            submittedTask.set(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        }).when(sseTaskExecutor).submit(any(Runnable.class));

        SseEmitter emitter = sseAdapter.streamScore(
                1L,
                "What is JVM?",
                "My answer",
                "Java"
        );

        assertThat(emitter).isNotNull();
        assertThat(submittedTask.get()).isNotNull();
        assertThatCode(() -> submittedTask.get().run()).doesNotThrowAnyException();

        verify(interviewService).streamScoreAnswer(
                eq(1L),
                eq("What is JVM?"),
                eq("My answer"),
                eq("Java"),
                any(AiTextDeltaConsumer.class)
        );
        verify(objectMapper).writeValueAsString(scoreResult);
    }

    @Test
    void shouldHandleStreamingFailureWithoutThrowing() {
        AtomicReference<Runnable> submittedTask = new AtomicReference<>();
        when(sseProperties.getScoreTimeoutMillis()).thenReturn(45_000L);
        when(interviewService.streamScoreAnswer(
                eq(1L),
                eq("What is JVM?"),
                eq("My answer"),
                eq("Java"),
                any(AiTextDeltaConsumer.class)
        )).thenThrow(new IllegalStateException("score service unavailable"));
        doAnswer(invocation -> {
            submittedTask.set(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        }).when(sseTaskExecutor).submit(any(Runnable.class));

        SseEmitter emitter = sseAdapter.streamScore(
                1L,
                "What is JVM?",
                "My answer",
                "Java"
        );

        assertThat(emitter).isNotNull();
        assertThat(submittedTask.get()).isNotNull();
        assertThatCode(() -> submittedTask.get().run()).doesNotThrowAnyException();

        verify(interviewService).streamScoreAnswer(
                eq(1L),
                eq("What is JVM?"),
                eq("My answer"),
                eq("Java"),
                any(AiTextDeltaConsumer.class)
        );
        verifyNoInteractions(objectMapper);
    }
}
