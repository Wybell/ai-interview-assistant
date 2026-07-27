package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AiModelResponse", description = "当前用户可选择的 AI 模型目录项。")
public record AiModelResponse(
        @Schema(description = "模型目录 ID；更新模型偏好时传入此值。", example = "2")
        Long id,
        @Schema(description = "后端使用的非敏感 Provider 标识。", example = "change2proapi")
        String provider,
        @Schema(description = "Provider 下的模型代码。", example = "gpt-5.6-luna")
        String modelCode,
        @Schema(description = "供界面展示的模型名称。", example = "GPT-5.6 Luna")
        String displayName,
        @Schema(description = "是否为系统策略中的默认模型。")
        boolean defaultModel,
        @Schema(description = "是否为当前用户当前选中的模型。")
        boolean selected) {
}
