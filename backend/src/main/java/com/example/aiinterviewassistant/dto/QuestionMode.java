package com.example.aiinterviewassistant.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum QuestionMode {
    KNOWLEDGE_BASE,
    CUSTOM_TOPIC,
    TECHNICAL_TOPIC;

    @JsonCreator
    public static QuestionMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOM_TOPIC;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported question mode");
        }
    }
}
