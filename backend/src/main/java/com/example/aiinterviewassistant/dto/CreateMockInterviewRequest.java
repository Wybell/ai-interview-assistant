package com.example.aiinterviewassistant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@Data
public class CreateMockInterviewRequest {

    @Positive(message = "Resume ID must be positive")
    private Long resumeId;

    @NotBlank(message = "Target position must not be blank")
    @Size(max = 100, message = "Target position is too long")
    private String targetPosition;

    @Size(max = 100, message = "Target company is too long")
    private String targetCompany;

    @NotBlank(message = "Interview round must not be blank")
    @Pattern(regexp = "FIRST|SECOND|THIRD", message = "Interview round is invalid")
    private String interviewRound;
}
