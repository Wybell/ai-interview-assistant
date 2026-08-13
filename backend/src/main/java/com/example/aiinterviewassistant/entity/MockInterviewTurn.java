package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mock_interview_turn")
public class MockInterviewTurn {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer sequenceNo;
    private String turnType;
    private Long parentTurnId;
    private Integer followUpNo;
    private String question;
    private String userAnswer;
    private Integer score;
    private String correctAnswer;
    private String suggestion;
    private String focusTag;
    private LocalDateTime createTime;
}
