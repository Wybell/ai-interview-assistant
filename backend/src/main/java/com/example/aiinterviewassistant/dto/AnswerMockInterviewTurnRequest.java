package com.example.aiinterviewassistant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AnswerMockInterviewTurnRequest {

    @NotBlank(message = "Answer must not be blank")
    @Size(max = 5000, message = "Answer is too long")
    private String answer;
}
