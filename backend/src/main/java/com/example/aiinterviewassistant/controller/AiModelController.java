package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AiModelPreferenceResponse;
import com.example.aiinterviewassistant.dto.AiModelResponse;
import com.example.aiinterviewassistant.dto.UpdateAiModelPreferenceRequest;
import com.example.aiinterviewassistant.service.AiModelCatalogService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import com.example.aiinterviewassistant.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@Tag(name = "AI 模型", description = "查询和保存当前登录用户的 AI 模型选择。")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(
            summary = "查询可选 AI 模型",
            description = "返回当前已启用且运行环境可用的模型目录，并标识系统默认模型和当前用户选中的模型。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "可选 AI 模型列表"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "系统默认 AI 模型当前不可用"
            )
    })
    public ApiResponse<List<AiModelResponse>> getAvailableModels() {
        Long userId = userContext.getCurrentUserId();
        List<AiModelResponse> models = aiModelCatalogService.getAvailableModels(userId);
        return ApiResponse.success(models);
    }

    @GetMapping("/api/users/me/ai-preference")
    @Operation(
            summary = "查询当前用户的 AI 模型偏好",
            description = "优先返回用户保存的模型；没有有效偏好时回退到系统默认模型。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "当前生效的 AI 模型偏好"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "当前生效的 AI 模型不可用"
            )
    })
    public ApiResponse<AiModelPreferenceResponse> getCurrentPreference() {
        Long userId = userContext.getCurrentUserId();
        AiModelPreferenceResponse preference = userAiPreferenceService
                .getEffectivePreference(userId);
        return ApiResponse.success(preference);
    }

    @PutMapping("/api/users/me/ai-preference")
    @Operation(
            summary = "更新当前用户的 AI 模型偏好",
            description = "仅接收可选模型目录中的 modelId；不接受 Provider、模型代码、端点或凭证。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "模型偏好已保存并立即生效"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "modelId 非法，或所选模型不存在或已禁用"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "所选模型当前不可用"
            )
    })
    public ApiResponse<AiModelPreferenceResponse> updateCurrentPreference(
            @Valid @RequestBody UpdateAiModelPreferenceRequest request) {
        Long userId = userContext.getCurrentUserId();
        AiModelPreferenceResponse preference = userAiPreferenceService
                .updatePreference(userId, request.modelId());
        return ApiResponse.success(preference);
    }
}
