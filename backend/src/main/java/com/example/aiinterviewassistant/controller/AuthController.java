package com.example.aiinterviewassistant.controller;
import com.example.aiinterviewassistant.dto.AuthRequest;

import javax.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AuthTokenResponse;
import com.example.aiinterviewassistant.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
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
