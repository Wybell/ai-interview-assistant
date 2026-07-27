package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.config.AiProperties;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class Change2ProResponsesAiClient implements AiClient {

    private static final String PROVIDER = "change2proapi";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public Change2ProResponsesAiClient(
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
        return StringUtils.hasText(aiProperties.getChange2proApiKey())
                && StringUtils.hasText(aiProperties.getChange2proEndpoint());
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

            String text = extractText(response.body());
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(502, "AI服务返回内容为空");
            }

            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "AI服务调用失败");
        } catch (IOException | IllegalArgumentException e) {
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
                String eventType = StringUtils.hasText(eventName)
                        ? eventName
                        : eventNode.path("type").asText();
                if ("response.output_text.delta".equals(eventType)) {
                    appendDelta(generatedText, eventNode.path("delta").asText(), deltaConsumer);
                } else if ("response.output_text.done".equals(eventType)) {
                    appendCumulativeText(generatedText, eventNode.path("text").asText(), deltaConsumer);
                } else if ("response.completed".equals(eventType)) {
                    appendCumulativeText(
                            generatedText,
                            extractText(eventNode.path("response")),
                            deltaConsumer
                    );
                }
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
        } catch (IOException | IllegalArgumentException exception) {
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
        String apiKey = requireConfiguredValue(aiProperties.getChange2proApiKey());
        String endpoint = requireConfiguredValue(aiProperties.getChange2proEndpoint());
        String requestedModel = requireConfiguredValue(modelCode);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", requestedModel);
        requestBody.put("instructions", systemPrompt);
        requestBody.put("input", userContent);
        requestBody.put("store", !aiProperties.isChange2proDisableResponseStorage());
        requestBody.put("stream", stream);

        if (StringUtils.hasText(aiProperties.getChange2proReasoningEffort())) {
            requestBody.putObject("reasoning")
                    .put("effort", aiProperties.getChange2proReasoningEffort());
        }

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

    private String extractText(String responseBody) throws IOException {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }

        JsonNode responseNode = objectMapper.readTree(responseBody);
        return extractText(responseNode);
    }

    private String extractText(JsonNode responseNode) {
        if (responseNode == null || responseNode.isMissingNode()) {
            return "";
        }

        String topLevelOutputText = responseNode.path("output_text").asText();
        if (StringUtils.hasText(topLevelOutputText)) {
            return topLevelOutputText;
        }

        for (JsonNode outputItem : responseNode.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    String text = contentItem.path("text").asText();
                    if (StringUtils.hasText(text)) {
                        return text;
                    }
                }
            }
        }

        return "";
    }

    private void appendCumulativeText(
            StringBuilder generatedText,
            String fullText,
            AiTextDeltaConsumer deltaConsumer) {
        if (!StringUtils.hasText(fullText)) {
            return;
        }

        String currentText = generatedText.toString();
        String delta = fullText.startsWith(currentText)
                ? fullText.substring(currentText.length())
                : fullText;
        appendDelta(generatedText, delta, deltaConsumer);
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
            throw new BusinessException(500, "AI服务配置不完整");
        }
        return value;
    }
}
