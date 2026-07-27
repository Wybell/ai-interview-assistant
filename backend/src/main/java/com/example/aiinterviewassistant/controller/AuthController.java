package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AuthRequest;
import com.example.aiinterviewassistant.dto.AuthTokenResponse;
import com.example.aiinterviewassistant.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "用户注册和登录接口，不需要 JWT Bearer Token。")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "用户注册",
            description = "创建用户并返回 JWT。请求体使用 application/x-www-form-urlencoded。",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                            schema = @Schema(implementation = AuthRequest.class)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "注册成功，返回 JWT"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "用户名或密码未通过参数校验"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "用户名已存在"
            )
    })
    public ApiResponse<AuthTokenResponse> register(
            @Valid @ModelAttribute AuthRequest request) {
        AuthTokenResponse response = authService.register(
                request.getUsername(),
                request.getPassword()
        );

        return ApiResponse.success(
                "注册成功",
                response
        );
    }

    @PostMapping("/login")
    @Operation(
            summary = "用户登录",
            description = "校验用户名和密码并返回 JWT。请求体使用 application/x-www-form-urlencoded。",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                            schema = @Schema(implementation = AuthRequest.class)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "登录成功，返回 JWT"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "用户名或密码未通过参数校验"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "用户名或密码错误"
            )
    })
    public ApiResponse<AuthTokenResponse> login(
            @Valid @ModelAttribute AuthRequest request) {
        AuthTokenResponse response = authService.login(
                request.getUsername(),
                request.getPassword()
        );

        return ApiResponse.success(
                "登录成功",
                response
        );
    }
}
