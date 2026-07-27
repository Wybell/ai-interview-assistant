package com.example.aiinterviewassistant.dto;

import java.util.Map;

public record StudyProgressResponse(String tag, long totalCount, double avgScore) {

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
