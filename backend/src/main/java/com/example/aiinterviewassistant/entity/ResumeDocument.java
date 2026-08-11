package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_document")
public class ResumeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalFileName;
    private String contentType;
    private String storagePath;
    private String extractedContent;
    private LocalDateTime createTime;
}
