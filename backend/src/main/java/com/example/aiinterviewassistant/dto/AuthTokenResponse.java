package com.example.aiinterviewassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "AuthTokenResponse", description = "认证成功后返回的 JWT 和用户名。")
public class AuthTokenResponse {

    @Schema(description = "用于调用受保护接口的 JWT。通过 Authorization: Bearer <token> 发送。")
    private String token;

    @Schema(description = "认证成功的用户名。", example = "candidate01")
    private String username;
}
