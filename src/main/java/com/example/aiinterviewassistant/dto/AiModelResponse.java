package com.example.aiinterviewassistant.dto;

public record AiModelResponse(
        Long id,
        String provider,
        String modelCode,
        String displayName,
        boolean defaultModel,
        boolean selected) {
}
