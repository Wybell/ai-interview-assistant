package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AiModelPreferenceResponse;
import com.example.aiinterviewassistant.model.EffectiveAiModel;

public interface UserAiPreferenceService {

    EffectiveAiModel resolveEffectiveModel(Long userId);

    EffectiveAiModel resolveDefaultModel();

    EffectiveAiModel resolveAvailableModel(Long modelId);

    AiModelPreferenceResponse getEffectivePreference(Long userId);

    AiModelPreferenceResponse updatePreference(Long userId, Long modelId);
}
