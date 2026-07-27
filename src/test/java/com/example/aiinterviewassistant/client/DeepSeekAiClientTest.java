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
class DeepSeekAiClientTest {

    private static final String MODEL_CODE = "deepseek-v4-flash";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpResponse<InputStream> streamHttpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeepSeekAiClient aiClient;

    @BeforeEach
    void setUp() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setDeepseekApiKey("test-api-key");
        aiProperties.setDeepseekEndpoint("https://api.deepseek.com/chat/completions");

        aiClient = new DeepSeekAiClient(aiProperties, objectMapper, httpClient);
    }

    @Test
    void shouldCallChatCompletionsApiAndReturnGeneratedText() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Generated interview question"
                      }
                    }
                  ]
                }
                """);
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        String result = aiClient.generate(
                MODEL_CODE,
                "You are an interviewer.",
                "Ask about JVM."
        );

        assertThat(result).isEqualTo("Generated interview question");
        assertThat(aiClient.isConfigured()).isTrue();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).<String>send(requestCaptor.capture(), any());

        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri()).isEqualTo(URI.create("https://api.deepseek.com/chat/completions"));
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer test-api-key");
        assertThat(request.headers().firstValue("Content-Type")).hasValue("application/json");

        JsonNode requestBody = objectMapper.readTree(readRequestBody(request));
        assertThat(requestBody.path("model").asText()).isEqualTo(MODEL_CODE);
        assertThat(requestBody.path("stream").asBoolean()).isFalse();
        assertThat(requestBody.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(requestBody.path("messages").get(0).path("content").asText()).isEqualTo("You are an interviewer.");
        assertThat(requestBody.path("messages").get(1).path("role").asText()).isEqualTo("user");
        assertThat(requestBody.path("messages").get(1).path("content").asText()).isEqualTo("Ask about JVM.");
    }

    @Test
    void shouldRejectNonSuccessfulChatCompletionsCall() throws Exception {
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        assertThatThrownBy(() -> aiClient.generate(MODEL_CODE, "System prompt", "User prompt"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(502);
    }

    @Test
    void shouldRejectNonSuccessfulStreamingChatCompletionsCall() throws Exception {
        when(streamHttpResponse.statusCode()).thenReturn(429);
        when(httpClient.<InputStream>send(any(HttpRequest.class), any()))
                .thenReturn(streamHttpResponse);

        assertThatThrownBy(() -> aiClient.generateStream(
                MODEL_CODE,
                "System prompt",
                "User prompt",
                delta -> { }
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(502);
    }

    @Test
    void shouldStreamChatCompletionDeltasAsTheyArrive() throws Exception {
        String ssePayload = """
                data: {"choices":[{"delta":{"content":"Generated "}}]}

                data: {"choices":[{"delta":{"content":"question"}}]}

                data: [DONE]

                """;
        List<String> deltas = new ArrayList<>();
        when(streamHttpResponse.statusCode()).thenReturn(200);
        when(streamHttpResponse.body()).thenReturn(new ByteArrayInputStream(
                ssePayload.getBytes(StandardCharsets.UTF_8)
        ));
        when(httpClient.<InputStream>send(any(HttpRequest.class), any()))
                .thenReturn(streamHttpResponse);

        String result = aiClient.generateStream(
                MODEL_CODE,
                "You are an interviewer.",
                "Ask about JVM.",
                deltas::add
        );

        assertThat(result).isEqualTo("Generated question");
        assertThat(deltas).containsExactly("Generated ", "question");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).<InputStream>send(requestCaptor.capture(), any());
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.headers().firstValue("Accept")).hasValue("text/event-stream");
        JsonNode requestBody = objectMapper.readTree(readRequestBody(request));
        assertThat(requestBody.path("stream").asBoolean()).isTrue();
    }

    @Test
    void shouldRejectIncompleteConfigurationBeforeSendingRequest() {
        AiProperties incompleteProperties = new AiProperties();
        incompleteProperties.setDeepseekApiKey("test-api-key");
        DeepSeekAiClient incompleteClient = new DeepSeekAiClient(
                incompleteProperties,
                objectMapper,
                httpClient
        );

        assertThat(incompleteClient.isConfigured()).isFalse();

        assertThatThrownBy(() -> incompleteClient.generate(MODEL_CODE, "System prompt", "User prompt"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(500);
        verifyNoInteractions(httpClient);
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
