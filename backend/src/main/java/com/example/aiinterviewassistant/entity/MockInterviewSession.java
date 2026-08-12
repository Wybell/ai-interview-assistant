package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mock_interview_session")
public class MockInterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String targetPosition;
    private String targetCompany;
    private String interviewRound;
    private String status;
    private Integer questionCount;
    private Long aiModelId;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime finishedTime;
}
