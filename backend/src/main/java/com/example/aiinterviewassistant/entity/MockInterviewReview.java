package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mock_interview_review")
public class MockInterviewReview {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer answeredTurnCount;
    private Integer mainQuestionCount;
    private Integer followUpCount;
    private BigDecimal averageScore;
    private String overallFeedback;
    private String strengths;
    private String improvementAreas;
    private String actionItems;
    private Long aiModelId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
