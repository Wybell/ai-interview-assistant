package com.example.aiinterviewassistant.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public record KnowledgeTopicRequest(
        @NotBlank String direction,
        @NotBlank String language,
        @NotBlank String category,
        @NotBlank String title,
        @NotBlank String summary,
        @NotEmpty List<String> keyPoints,
        @NotEmpty List<@Valid KnowledgeQuestionRequest> questions,
        Boolean published
) {
    public record KnowledgeQuestionRequest(@NotBlank String question, @NotBlank String answer, String difficulty) { }
}
