package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AiModelPreferenceResponse", description = "当前用户实际生效的 AI 模型偏好。")
public record AiModelPreferenceResponse(
        @Schema(description = "当前生效的模型目录 ID。", example = "1")
        Long modelId,
        @Schema(description = "后端使用的非敏感 Provider 标识。", example = "deepseek")
        String provider,
        @Schema(description = "当前生效的模型代码。", example = "deepseek-v4-flash")
        String modelCode,
        @Schema(description = "供界面展示的模型名称。", example = "DeepSeek V4 Flash")
        String displayName,
        @Schema(description = "true 表示当前结果来自系统默认策略，而非用户显式保存的偏好。")
        boolean defaultSelection) {
}
