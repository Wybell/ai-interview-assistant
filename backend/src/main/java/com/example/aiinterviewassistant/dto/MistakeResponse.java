package com.example.aiinterviewassistant.dto;

import com.example.aiinterviewassistant.entity.AnswerRecord;

import java.time.LocalDateTime;

public record MistakeResponse(
        Long id,
        String tag,
        String question,
        String userAnswer,
        Integer score,
        String correctAnswer,
        String suggestion,
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
