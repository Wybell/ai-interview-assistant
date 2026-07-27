package com.example.aiinterviewassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiScoreResult {

    private int score;

    @JsonProperty("correct_answer")
    private String correctAnswer;

    private String suggestion;
}
