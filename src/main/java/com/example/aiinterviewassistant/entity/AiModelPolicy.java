package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_policy")
public class AiModelPolicy {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private Long defaultAiModelId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
