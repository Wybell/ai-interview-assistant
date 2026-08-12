package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.config.AiProperties;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
public class DeepSeekAiClient implements AiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiClient.class);
    private static final String PROVIDER = "deepseek";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekAiClient(
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            @Qualifier("aiHttpClient") HttpClient httpClient) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(aiProperties.getDeepseekApiKey())
                && StringUtils.hasText(aiProperties.getDeepseekEndpoint());
    }

    @Override
    public String generate(String modelCode, String systemPrompt, String userContent) {
        try {
            HttpRequest request = createRequest(modelCode, systemPrompt, userContent, false);
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (!isSuccessful(response.statusCode())) {
                logUpstreamHttpFailure("generate", response.statusCode());
                throw new BusinessException(502, "AI服务调用失败");
            }

            String text = extractText(response.body());
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(502, "AI服务返回内容为空");
            }

            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logTechnicalFailure("generate", "interrupted");
            throw new BusinessException(502, "AI服务调用失败");
        } catch (HttpTimeoutException e) {
            logTechnicalFailure("generate", "timeout");
            throw new BusinessException(504, "AI服务响应超时，请稍后重试");
        } catch (IOException | IllegalArgumentException e) {
            logTechnicalFailure("generate", technicalFailureCategory(e));
            throw new BusinessException(502, "AI服务调用失败");
        }
    }

    @Override
    public String generateStream(
            String modelCode,
            String systemPrompt,
            String userContent,
            AiTextDeltaConsumer deltaConsumer) {
        try {
            HttpRequest request = createRequest(modelCode, systemPrompt, userContent, true);
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (!isSuccessful(response.statusCode())) {
                logUpstreamHttpFailure("generate_stream", response.statusCode());
                throw new BusinessException(502, "AI服务调用失败");
            }

            StringBuilder generatedText = new StringBuilder();
            AiSseEventReader.read(response.body(), (eventName, data) -> {
                if ("[DONE]".equals(data)) {
                    return;
                }

                JsonNode eventNode = objectMapper.readTree(data);
                String delta = eventNode.path("choices")
                        .path(0)
                        .path("delta")
                        .path("content")
                        .asText();
                appendDelta(generatedText, delta, deltaConsumer);
            });

            if (generatedText.length() == 0) {
                logTechnicalFailure("generate_stream", "empty_response");
                throw new BusinessException(502, "AI服务返回内容为空");
            }
            return generatedText.toString();
        } catch (AiStreamCancelledException exception) {
            throw exception;
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiStreamCancelledException(exception);
        } catch (HttpTimeoutException exception) {
            logTechnicalFailure("generate_stream", "timeout");
            throw new BusinessException(504, "AI服务响应超时，请稍后重试");
        } catch (IOException | IllegalArgumentException exception) {
            if (Thread.currentThread().isInterrupted()) {
                throw new AiStreamCancelledException(exception);
            }
            logTechnicalFailure("generate_stream", technicalFailureCategory(exception));
            throw new BusinessException(502, "AI服务调用失败");
        }
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private void logUpstreamHttpFailure(String operation, int statusCode) {
        LOGGER.warn("deepseek_upstream_failure operation={} status={}", operation, statusCode);
    }

    private void logTechnicalFailure(String operation, String category) {
        LOGGER.warn("deepseek_upstream_failure operation={} category={}", operation, category);
    }

    private String technicalFailureCategory(Exception exception) {
        return exception instanceof IOException ? "io_failure" : "invalid_request_configuration";
    }

    private HttpRequest createRequest(
            String modelCode,
            String systemPrompt,
            String userContent,
            boolean stream) throws IOException {
        String apiKey = requireConfiguredValue(aiProperties.getDeepseekApiKey());
        String endpoint = requireConfiguredValue(aiProperties.getDeepseekEndpoint());
        String requestedModel = requireConfiguredValue(modelCode);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", requestedModel);
        requestBody.put("stream", stream);

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userContent);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT);
        if (stream) {
            requestBuilder.header("Accept", "text/event-stream");
        }
        return requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
    }

    private void appendDelta(
            StringBuilder generatedText,
            String delta,
            AiTextDeltaConsumer deltaConsumer) {
        if (!StringUtils.hasText(delta)) {
            return;
        }
        generatedText.append(delta);
        deltaConsumer.onDelta(delta);
    }

    private String extractText(String responseBody) throws IOException {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }

        JsonNode responseNode = objectMapper.readTree(responseBody);
        if (responseNode == null) {
            return "";
        }

        return responseNode.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
    }

    private String requireConfiguredValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(500, "AI服务配置不完整");
        }
        return value;
    }
}
