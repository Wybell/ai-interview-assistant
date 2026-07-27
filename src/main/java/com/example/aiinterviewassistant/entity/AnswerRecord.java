package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("answer_record")
public class AnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tag;

    private Long userId;

    private String question;

    private String userAnswer;

    private Integer score;

    private String correctAnswer;

    private String suggestion;

    private Long scoreAiModelId;

    private LocalDateTime createTime;
}
