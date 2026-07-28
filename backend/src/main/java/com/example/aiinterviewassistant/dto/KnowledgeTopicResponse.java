package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "KnowledgeTopicResponse", description = "知识专题及其解释")
public record KnowledgeTopicResponse(
        Long id,
        String direction,
        String language,
        String category,
        String title,
        String summary,
        List<String> keyPoints,
        List<KnowledgeQuestion> questions
) {
    public record KnowledgeQuestion(String question, String answer) {
    }
}
