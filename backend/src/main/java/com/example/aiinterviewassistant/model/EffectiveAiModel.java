package com.example.aiinterviewassistant.model;

/**
 * Immutable model-selection result used by business services and later runtime routing.
 */
public record EffectiveAiModel(
        Long id,
        String provider,
        String modelCode,
        String displayName,
        boolean defaultSelection) {
}
