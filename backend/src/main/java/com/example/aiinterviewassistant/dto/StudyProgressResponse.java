package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "StudyProgressResponse", description = "当前用户按知识点汇总的学习进度。")
public record StudyProgressResponse(
        @Schema(description = "面试知识点。", example = "HashMap")
        String tag,
        @Schema(description = "该知识点的累计答题次数。", example = "12")
        long totalCount,
        @Schema(description = "该知识点的平均得分。", example = "7.5")
        double avgScore) {

    public static StudyProgressResponse from(Map<String, Object> row) {
        Object tag = row.get("tag");
        Object totalCount = row.get("totalCount");
        Object avgScore = row.get("avgScore");

        if (!(tag instanceof String) || !(totalCount instanceof Number) || !(avgScore instanceof Number)) {
            throw new IllegalStateException("学习进度聚合结果格式不正确");
        }

        return new StudyProgressResponse(
                (String) tag,
                ((Number) totalCount).longValue(),
                ((Number) avgScore).doubleValue()
        );
    }
}
