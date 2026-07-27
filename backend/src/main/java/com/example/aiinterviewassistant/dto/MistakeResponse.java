package com.example.aiinterviewassistant.dto;

import com.example.aiinterviewassistant.entity.AnswerRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MistakeResponse", description = "当前用户的一条错题本记录，仅包含评分低于 6 分的答题记录。")
public record MistakeResponse(
        @Schema(description = "答题记录 ID。", example = "42")
        Long id,
        @Schema(description = "面试知识点。", example = "HashMap")
        String tag,
        @Schema(description = "当时的面试题。", example = "请说明 Java 8 中 HashMap 的 put 流程。")
        String question,
        @Schema(description = "用户当时提交的回答。", example = "先计算 hash，再插入桶。")
        String userAnswer,
        @Schema(description = "本次回答得分。", example = "4")
        Integer score,
        @Schema(description = "AI 给出的参考答案。", example = "还需要说明碰撞处理、树化条件和扩容逻辑。")
        String correctAnswer,
        @Schema(description = "AI 给出的改进建议。", example = "补充链表转红黑树与扩容阈值的条件。")
        String suggestion,
        @Schema(description = "答题记录创建时间，采用 ISO-8601 本地日期时间格式。", example = "2026-07-27T10:30:00")
        LocalDateTime createTime) {

    public static MistakeResponse from(AnswerRecord answerRecord) {
        return new MistakeResponse(
                answerRecord.getId(),
                answerRecord.getTag(),
                answerRecord.getQuestion(),
                answerRecord.getUserAnswer(),
                answerRecord.getScore(),
                answerRecord.getCorrectAnswer(),
                answerRecord.getSuggestion(),
                answerRecord.getCreateTime()
        );
    }
}
