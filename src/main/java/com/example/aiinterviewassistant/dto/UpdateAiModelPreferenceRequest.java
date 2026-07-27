package com.example.aiinterviewassistant.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public record UpdateAiModelPreferenceRequest(
        @NotNull @Positive Long modelId) {
}
