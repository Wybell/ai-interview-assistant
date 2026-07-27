package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.config.AiProperties;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class DashScopeAiClient implements AiClient {

    private static final String PROVIDER = "dashscope";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String MODEL_UNAVAILABLE_MESSAGE = "所选 AI 模型暂不可用";

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DashScopeAiClient(
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
        return StringUtils.hasText(aiProperties.getDashscopeApiKey())
                && StringUtils.hasText(aiProperties.getDashscopeEndpoint());
    }

    @Override
    public String generate(String modelCode, String systemPrompt, String userContent) {
        try {
            HttpRequest request = createRequest(modelCode, systemPrompt, userContent, false);

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(502, "AI服务调用失败");
            }

            String text = objectMapper.readTree(response.body())
                    .path("output")
                    .path("text")
                    .asText();
            if (text.isBlank()) {
                throw new BusinessException(502, "AI服务返回内容为空");
            }

            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
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

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(502, "AI服务调用失败");
            }

            StringBuilder generatedText = new StringBuilder();
            AiSseEventReader.read(response.body(), (eventName, data) -> {
                if ("[DONE]".equals(data)) {
                    return;
                }

                JsonNode eventNode = objectMapper.readTree(data);
                appendDelta(generatedText, extractStreamText(eventNode), deltaConsumer);
            });

            if (generatedText.length() == 0) {
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
        } catch (Exception exception) {
            if (Thread.currentThread().isInterrupted()) {
                throw new AiStreamCancelledException(exception);
            }
            throw new BusinessException(502, "AI服务调用失败");
        }
    }

    private HttpRequest createRequest(
            String modelCode,
            String systemPrompt,
            String userContent,
            boolean stream) throws IOException {
        String apiKey = requireConfiguredValue(aiProperties.getDashscopeApiKey());
        String endpoint = requireConfiguredValue(aiProperties.getDashscopeEndpoint());
        String requestedModel = requireConfiguredValue(modelCode);
        String requestBody = createRequestBody(requestedModel, systemPrompt, userContent, stream);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT);
        if (stream) {
            requestBuilder
                    .header("Accept", "text/event-stream")
                    .header("X-DashScope-SSE", "enable");
        }
        return requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String createRequestBody(
            String modelCode,
            String systemPrompt,
            String userContent,
            boolean stream) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelCode);

        ObjectNode input = root.putObject("input");
        ArrayNode messages = input.putArray("messages");

        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);

        if (stream) {
            root.putObject("parameters")
                    .put("incremental_output", true);
        }

        return objectMapper.writeValueAsString(root);
    }

    private String extractStreamText(JsonNode eventNode) {
        String outputText = eventNode.path("output").path("text").asText();
        if (StringUtils.hasText(outputText)) {
            return outputText;
        }

        return eventNode.path("output")
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
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

    private String requireConfiguredValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(503, MODEL_UNAVAILABLE_MESSAGE);
        }
        return value;
    }
}
