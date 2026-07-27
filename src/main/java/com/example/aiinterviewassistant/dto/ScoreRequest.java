package com.example.aiinterviewassistant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ScoreRequest {

    @NotBlank(message = "知识点不能为空")
    @Size(max = 50, message = "知识点长度不能超过50个字符")
    private String tag;

    @NotBlank(message = "面试题不能为空")
    @Size(max = 2000, message = "面试题长度不能超过2000个字符")
    private String question;

    @NotBlank(message = "回答不能为空")
    @Size(max = 5000, message = "回答长度不能超过5000个字符")
    private String answer;
}
