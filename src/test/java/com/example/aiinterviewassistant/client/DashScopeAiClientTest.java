package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.config.AiProperties;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashScopeAiClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpResponse<InputStream> streamHttpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DashScopeAiClient aiClient;

    @BeforeEach
    void setUp() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setDashscopeApiKey("test-api-key");
        aiProperties.setDashscopeEndpoint(
                "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
        );

        aiClient = new DashScopeAiClient(aiProperties, objectMapper, httpClient);
    }

    @Test
    void shouldUseRuntimeModelCodeForDashScopeRequest() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"output\":{\"text\":\"Generated question\"}}");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        String result = aiClient.generate("qwen-plus", "System prompt", "User prompt");

        assertThat(result).isEqualTo("Generated question");
        assertThat(aiClient.provider()).isEqualTo("dashscope");
        assertThat(aiClient.isConfigured()).isTrue();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).<String>send(requestCaptor.capture(), any());

        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri()).isEqualTo(URI.create(
                "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
        ));
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer test-api-key");

        JsonNode requestBody = objectMapper.readTree(readRequestBody(request));
        assertThat(requestBody.path("model").asText()).isEqualTo("qwen-plus");
        assertThat(requestBody.path("input").path("messages").get(0).path("content").asText())
                .isEqualTo("System prompt");
        assertThat(requestBody.path("input").path("messages").get(1).path("content").asText())
                .isEqualTo("User prompt");
    }

    @Test
    void shouldRejectIncompleteConfigurationBeforeSendingRequest() {
        AiProperties incompleteProperties = new AiProperties();
        incompleteProperties.setDashscopeApiKey("test-api-key");
        DashScopeAiClient incompleteClient = new DashScopeAiClient(
                incompleteProperties,
                objectMapper,
                httpClient
        );

        assertThat(incompleteClient.isConfigured()).isFalse();

        assertThatThrownBy(() -> incompleteClient.generate(
                "qwen-plus",
                "System prompt",
                "User prompt"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);
        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldStreamDashScopeIncrementalText() throws Exception {
        String ssePayload = """
                data: {"output":{"text":"Generated "}}

                data: {"output":{"text":"question"}}

                """;
        List<String> deltas = new ArrayList<>();
        when(streamHttpResponse.statusCode()).thenReturn(200);
        when(streamHttpResponse.body()).thenReturn(new ByteArrayInputStream(
                ssePayload.getBytes(StandardCharsets.UTF_8)
        ));
        when(httpClient.<InputStream>send(any(HttpRequest.class), any()))
                .thenReturn(streamHttpResponse);

        String result = aiClient.generateStream(
                "qwen-plus",
                "System prompt",
                "User prompt",
                deltas::add
        );

        assertThat(result).isEqualTo("Generated question");
        assertThat(deltas).containsExactly("Generated ", "question");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).<InputStream>send(requestCaptor.capture(), any());
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.headers().firstValue("Accept")).hasValue("text/event-stream");
        assertThat(request.headers().firstValue("X-DashScope-SSE")).hasValue("enable");
        JsonNode requestBody = objectMapper.readTree(readRequestBody(request));
        assertThat(requestBody.path("parameters").path("incremental_output").asBoolean()).isTrue();
    }

    private String readRequestBody(HttpRequest request) throws Exception {
        HttpRequest.BodyPublisher bodyPublisher = request.bodyPublisher().orElseThrow();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CountDownLatch completion = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        bodyPublisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                output.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                completion.countDown();
            }

            @Override
            public void onComplete() {
                completion.countDown();
            }
        });

        assertThat(completion.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
