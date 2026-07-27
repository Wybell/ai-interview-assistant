package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AiClientRegistry {

    private static final String MODEL_UNAVAILABLE_MESSAGE = "所选 AI 模型暂不可用";

    private final Map<String, AiClient> clientsByProvider;

    public AiClientRegistry(List<AiClient> aiClients) {
        Map<String, AiClient> registeredClients = new HashMap<>();
        for (AiClient aiClient : aiClients) {
            String provider = normalizeClientProvider(aiClient.provider());
            if (registeredClients.putIfAbsent(provider, aiClient) != null) {
                throw new IllegalStateException("Duplicate AI client provider: " + provider);
            }
        }
        this.clientsByProvider = Map.copyOf(registeredClients);
    }

    public String generate(
            String provider,
            String modelCode,
            String systemPrompt,
            String userContent) {
        AiClient aiClient = requireAvailableClient(provider, modelCode);

        try {
            return aiClient.generate(modelCode.trim(), systemPrompt, userContent);
        } catch (BusinessException exception) {
            if (exception.getCode() == 500) {
                throw modelUnavailable();
            }
            throw exception;
        }
    }

    public String generateStream(
            String provider,
            String modelCode,
            String systemPrompt,
            String userContent,
            AiTextDeltaConsumer deltaConsumer) {
        AiClient aiClient = requireAvailableClient(provider, modelCode);

        try {
            return aiClient.generateStream(
                    modelCode.trim(),
                    systemPrompt,
                    userContent,
                    deltaConsumer
            );
        } catch (BusinessException exception) {
            if (exception.getCode() == 500) {
                throw modelUnavailable();
            }
            throw exception;
        }
    }

    public boolean isModelAvailable(String provider, String modelCode) {
        AiClient aiClient = findClient(provider);
        return StringUtils.hasText(modelCode) && aiClient != null && aiClient.isConfigured();
    }

    private String normalizeClientProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalStateException("AI client provider must not be blank");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private AiClient findClient(String provider) {
        if (!StringUtils.hasText(provider)) {
            return null;
        }
        return clientsByProvider.get(provider.trim().toLowerCase(Locale.ROOT));
    }

    private AiClient requireAvailableClient(String provider, String modelCode) {
        AiClient aiClient = findClient(provider);
        if (!StringUtils.hasText(modelCode) || aiClient == null || !aiClient.isConfigured()) {
            throw modelUnavailable();
        }
        return aiClient;
    }

    private BusinessException modelUnavailable() {
        return new BusinessException(503, MODEL_UNAVAILABLE_MESSAGE);
    }
}
