package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(name = "AuthRequest", description = "注册和登录使用的表单参数。")
public class AuthRequest {

    @Schema(description = "用户名，长度为 3 到 20 个字符。", example = "candidate01")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度应为3到20个字符")
    private String username;

    @Schema(
            description = "密码，长度为 6 到 32 个字符。",
            example = "example-password",
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度应为6到32个字符")
    private String password;
}
