package com.example.aiinterviewassistant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class QuestionRequest {

    @NotBlank(message = "知识点不能为空")
    @Size(max = 50, message = "知识点长度不能超过50个字符")
    private String tag;

    private boolean refresh;
}
