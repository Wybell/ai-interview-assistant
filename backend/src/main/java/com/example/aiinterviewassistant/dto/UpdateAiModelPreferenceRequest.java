package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Schema(name = "UpdateAiModelPreferenceRequest", description = "更新当前用户 AI 模型偏好的 JSON 请求体。")
public record UpdateAiModelPreferenceRequest(
        @Schema(description = "来自可选 AI 模型目录的正整数 ID。", example = "2")
        @NotNull @Positive Long modelId) {
}
