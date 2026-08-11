package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@Data
@Schema(name = "QuestionRequest", description = "生成面试题的 JSON 请求体。")
public class QuestionRequest {

    @Schema(description = "面试方向", example = "backend")
    @NotBlank(message = "面试方向不能为空")
    @Pattern(regexp = "frontend|backend", message = "面试方向只能是 frontend 或 backend")
    private String direction;

    @Schema(description = "面试语言或技术栈", example = "Java")
    @NotBlank(message = "面试语言不能为空")
    @Size(max = 30, message = "面试语言长度不能超过30个字符")
    private String language;

    @Schema(description = "面试知识点，不能为空且最多 50 个字符。", example = "HashMap")
    @NotBlank(message = "知识点不能为空")
    @Size(max = 50, message = "知识点长度不能超过50个字符")
    private String tag;

    @Schema(description = "Optional published knowledge topic ID used to ground the generated question.", example = "1")
    @Positive(message = "Knowledge topic ID must be positive")
    private Long knowledgeTopicId;

    @Schema(description = "true 时忽略当前用户、模型和知识点对应的题目缓存，强制重新生成。", example = "false")
    private boolean refresh;
}
