package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(name = "ScoreRequest", description = "同步评分面试答案的 JSON 请求体。")
public class ScoreRequest {

    @Schema(description = "面试知识点，不能为空且最多 50 个字符。", example = "HashMap")
    @NotBlank(message = "知识点不能为空")
    @Size(max = 50, message = "知识点长度不能超过50个字符")
    private String tag;

    @Schema(description = "待评分的面试题，不能为空且最多 2000 个字符。", example = "请说明 Java 8 中 HashMap 的 put 流程。")
    @NotBlank(message = "面试题不能为空")
    @Size(max = 2000, message = "面试题长度不能超过2000个字符")
    private String question;

    @Schema(description = "用户回答，不能为空且最多 5000 个字符。", example = "先计算 hash 并定位桶；发生冲突时比较 key，必要时遍历链表或红黑树；超过阈值则扩容。")
    @NotBlank(message = "回答不能为空")
    @Size(max = 5000, message = "回答长度不能超过5000个字符")
    private String answer;
}
