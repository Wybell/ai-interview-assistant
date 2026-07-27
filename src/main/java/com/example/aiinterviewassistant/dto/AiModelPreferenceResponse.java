package com.example.aiinterviewassistant.dto;

public record AiModelPreferenceResponse(
        Long modelId,
        String provider,
        String modelCode,
        String displayName,
        boolean defaultSelection) {
}
