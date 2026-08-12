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
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/** OpenAI Responses-compatible client for the externally configured custom provider. */
@Component
public class CustomResponsesAiClient implements AiClient {

    private static final String PROVIDER = "custom";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CustomResponsesAiClient(
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
        return StringUtils.hasText(aiProperties.getCustomApiKey())
                && StringUtils.hasText(aiProperties.getCustomEndpoint());
    }

    @Override
    public String generate(String modelCode, String systemPrompt, String userContent) {
        try {
            HttpResponse<String> response = httpClient.send(
                    createRequest(modelCode, systemPrompt, userContent, false),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(502, "AI service request failed");
            }
            String text = extractText(objectMapper.readTree(response.body()));
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(502, "AI service returned no text");
            }
            return text;
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "AI service request failed");
        } catch (HttpTimeoutException exception) {
            throw new BusinessException(504, "AI服务响应超时，请稍后重试");
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(502, "AI service request failed");
        }
    }

    @Override
    public String generateStream(
            String modelCode,
            String systemPrompt,
            String userContent,
            AiTextDeltaConsumer deltaConsumer) {
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    createRequest(modelCode, systemPrompt, userContent, true),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(502, "AI service request failed");
            }

            StringBuilder generatedText = new StringBuilder();
            AiSseEventReader.read(response.body(), (eventName, data) -> {
                if ("[DONE]".equals(data)) {
                    return;
                }
                JsonNode event = objectMapper.readTree(data);
                String eventType = StringUtils.hasText(eventName) ? eventName : event.path("type").asText();
                if ("response.output_text.delta".equals(eventType)) {
                    appendDelta(generatedText, event.path("delta").asText(), deltaConsumer);
                } else if ("response.output_text.done".equals(eventType)) {
                    appendCumulativeText(generatedText, event.path("text").asText(), deltaConsumer);
                } else if ("response.completed".equals(eventType)) {
                    appendCumulativeText(generatedText, extractText(event.path("response")), deltaConsumer);
                }
            });
            if (generatedText.length() == 0) {
                throw new BusinessException(502, "AI service returned no text");
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
            throw new BusinessException(504, "AI服务响应超时，请稍后重试");
        } catch (IOException | IllegalArgumentException exception) {
            if (Thread.currentThread().isInterrupted()) {
                throw new AiStreamCancelledException(exception);
            }
            throw new BusinessException(502, "AI service request failed");
        }
    }

    private HttpRequest createRequest(
            String modelCode,
            String systemPrompt,
            String userContent,
            boolean stream) throws IOException {
        String apiKey = requireConfiguredValue(aiProperties.getCustomApiKey());
        String endpoint = requireConfiguredValue(aiProperties.getCustomEndpoint());
        String requestedModel = requireConfiguredValue(modelCode);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", requestedModel);
        body.put("instructions", systemPrompt);
        body.put("input", userContent);
        body.put("store", !aiProperties.isCustomDisableResponseStorage());
        body.put("stream", stream);
        if (StringUtils.hasText(aiProperties.getCustomReasoningEffort())) {
            body.putObject("reasoning").put("effort", aiProperties.getCustomReasoningEffort());
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT);
        if (stream) {
            builder.header("Accept", "text/event-stream");
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
    }

    private String extractText(JsonNode response) {
        if (response == null || response.isMissingNode()) {
            return "";
        }
        String topLevelOutputText = response.path("output_text").asText();
        if (StringUtils.hasText(topLevelOutputText)) {
            return topLevelOutputText;
        }
        for (JsonNode outputItem : response.path("output")) {
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

    private void appendCumulativeText(StringBuilder generatedText, String fullText, AiTextDeltaConsumer consumer) {
        if (!StringUtils.hasText(fullText)) {
            return;
        }
        String current = generatedText.toString();
        appendDelta(generatedText, fullText.startsWith(current) ? fullText.substring(current.length()) : fullText, consumer);
    }

    private void appendDelta(StringBuilder generatedText, String delta, AiTextDeltaConsumer consumer) {
        if (StringUtils.hasText(delta)) {
            generatedText.append(delta);
            consumer.onDelta(delta);
        }
    }

    private String requireConfiguredValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(500, "AI provider configuration is incomplete");
        }
        return value;
    }
}
