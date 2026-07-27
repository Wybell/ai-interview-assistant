package com.example.aiinterviewassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_ai_preference")
public class UserAiPreference {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private Long aiModelId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
