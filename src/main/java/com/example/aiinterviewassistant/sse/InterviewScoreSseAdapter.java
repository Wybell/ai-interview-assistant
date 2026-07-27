package com.example.aiinterviewassistant.sse;

import com.example.aiinterviewassistant.client.AiStreamCancelledException;
import com.example.aiinterviewassistant.config.SseProperties;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.service.InterviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InterviewScoreSseAdapter {

    private final AsyncTaskExecutor sseTaskExecutor;
    private final InterviewService interviewService;
    private final ObjectMapper objectMapper;
    private final SseProperties sseProperties;

    public InterviewScoreSseAdapter(
            @Qualifier("sseTaskExecutor") AsyncTaskExecutor sseTaskExecutor,
            InterviewService interviewService,
            ObjectMapper objectMapper,
            SseProperties sseProperties) {
        this.sseTaskExecutor = sseTaskExecutor;
        this.interviewService = interviewService;
        this.objectMapper = objectMapper;
        this.sseProperties = sseProperties;
    }

    public SseEmitter streamScore(Long userId, String question, String answer, String tag) {
        if (userId == null) {
            return loginRequiredEmitter();
        }

        SseEmitter emitter = new SseEmitter(sseProperties.getScoreTimeoutMillis());
        StreamSession session = new StreamSession();
        registerCancellationCallbacks(emitter, session);

        try {
            Future<?> task = sseTaskExecutor.submit(
                    () -> sendScore(emitter, session, userId, question, answer, tag)
            );
            session.setTask(task);
        } catch (RuntimeException exception) {
            sendFailure(emitter, session);
        }
        return emitter;
    }

    private SseEmitter loginRequiredEmitter() {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send("请先登录");
            emitter.complete();
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    private void sendScore(
            SseEmitter emitter,
            StreamSession session,
            Long userId,
            String question,
            String answer,
            String tag) {
        try {
            AiScoreResult scoreResult = interviewService.streamScoreAnswer(
                    userId,
                    question,
                    answer,
                    tag,
                    delta -> sendDelta(emitter, session, delta)
            );
            if (session.isCancelled()) {
                return;
            }

            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(objectMapper.writeValueAsString(scoreResult)));
            session.finish();
            emitter.complete();
        } catch (AiStreamCancelledException exception) {
            if (!session.isCancelled()) {
                sendFailure(emitter, session);
            }
        } catch (Exception exception) {
            if (!session.isCancelled()) {
                sendFailure(emitter, session);
            }
        }
    }

    private void registerCancellationCallbacks(SseEmitter emitter, StreamSession session) {
        emitter.onCompletion(session::cancelIfActive);
        emitter.onTimeout(session::cancel);
        emitter.onError(throwable -> session.cancel());
    }

    private void sendDelta(SseEmitter emitter, StreamSession session, String delta) {
        if (session.isCancelled()) {
            throw new AiStreamCancelledException();
        }

        try {
            emitter.send(delta);
        } catch (IOException exception) {
            session.cancel();
            throw new AiStreamCancelledException(exception);
        }
    }

    private void sendFailure(SseEmitter emitter, StreamSession session) {
        session.finish();
        try {
            emitter.send(SseEmitter.event().name("error").data("评分失败，请稍后重试"));
            emitter.complete();
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private static final class StreamSession {

        private final AtomicBoolean finished = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Future<?>> taskReference = new AtomicReference<>();

        void setTask(Future<?> task) {
            taskReference.set(task);
            if (cancelled.get()) {
                task.cancel(true);
            }
        }

        void cancelIfActive() {
            if (!finished.get()) {
                cancel();
            }
        }

        void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                Future<?> task = taskReference.get();
                if (task != null) {
                    task.cancel(true);
                }
            }
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        void finish() {
            finished.set(true);
        }
    }
}
