package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AiModelPreferenceResponse;
import com.example.aiinterviewassistant.dto.AiModelResponse;
import com.example.aiinterviewassistant.dto.UpdateAiModelPreferenceRequest;
import com.example.aiinterviewassistant.service.AiModelCatalogService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import com.example.aiinterviewassistant.utils.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
public class AiModelController {

    private final UserContext userContext;
    private final AiModelCatalogService aiModelCatalogService;
    private final UserAiPreferenceService userAiPreferenceService;

    public AiModelController(
            UserContext userContext,
            AiModelCatalogService aiModelCatalogService,
            UserAiPreferenceService userAiPreferenceService) {
        this.userContext = userContext;
        this.aiModelCatalogService = aiModelCatalogService;
        this.userAiPreferenceService = userAiPreferenceService;
    }

    @GetMapping("/api/ai/models")
    public ApiResponse<List<AiModelResponse>> getAvailableModels() {
        Long userId = userContext.getCurrentUserId();
        List<AiModelResponse> models = aiModelCatalogService.getAvailableModels(userId);
        return ApiResponse.success(models);
    }

    @GetMapping("/api/users/me/ai-preference")
    public ApiResponse<AiModelPreferenceResponse> getCurrentPreference() {
        Long userId = userContext.getCurrentUserId();
        AiModelPreferenceResponse preference = userAiPreferenceService
                .getEffectivePreference(userId);
        return ApiResponse.success(preference);
    }

    @PutMapping("/api/users/me/ai-preference")
    public ApiResponse<AiModelPreferenceResponse> updateCurrentPreference(
            @Valid @RequestBody UpdateAiModelPreferenceRequest request) {
        Long userId = userContext.getCurrentUserId();
        AiModelPreferenceResponse preference = userAiPreferenceService
                .updatePreference(userId, request.modelId());
        return ApiResponse.success(preference);
    }
}
