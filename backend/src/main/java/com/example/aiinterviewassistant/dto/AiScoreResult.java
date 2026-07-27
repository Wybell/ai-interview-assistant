package com.example.aiinterviewassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "AiScoreResult", description = "AI 对一次面试答案的评分结果。")
public class AiScoreResult {

    @Schema(description = "评分，范围为 0 到 10。", minimum = "0", maximum = "10", example = "8")
    private int score;

    @JsonProperty("correct_answer")
    @Schema(description = "用于复盘的参考答案。", example = "先计算 key 的 hash，再定位桶；发生冲突时比较 key，必要时处理链表或红黑树，最后按阈值决定是否扩容。")
    private String correctAnswer;

    @Schema(description = "基于本次回答生成的改进建议。", example = "可补充树化阈值、扩容条件和 key 相等判断的细节。")
    private String suggestion;
}
