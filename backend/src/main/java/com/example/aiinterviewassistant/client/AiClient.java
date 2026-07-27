package com.example.aiinterviewassistant.client;

public interface AiClient {

    String provider();

    boolean isConfigured();

    String generate(String modelCode, String systemPrompt, String userContent);

    String generateStream(
            String modelCode,
            String systemPrompt,
            String userContent,
            AiTextDeltaConsumer deltaConsumer);
}
