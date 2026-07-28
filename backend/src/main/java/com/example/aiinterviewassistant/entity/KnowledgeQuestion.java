package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_question")
public class KnowledgeQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private String question;
    private String answer;
    private String difficulty;
    private Integer sortOrder;
}
