package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_topic")
public class KnowledgeTopic {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String direction;
    private String language;
    private String category;
    private String title;
    private String sourceFileName;
    private String summary;
    private String keyPoints;
    private String documentContent;
    private Integer published;
    private Integer sortOrder;
}
