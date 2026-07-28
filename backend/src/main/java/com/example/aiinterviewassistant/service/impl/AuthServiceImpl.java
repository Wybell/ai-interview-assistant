package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.dto.AuthTokenResponse;
import com.example.aiinterviewassistant.entity.User;
import com.example.aiinterviewassistant.service.AuthService;
import com.example.aiinterviewassistant.service.UserService;
import com.example.aiinterviewassistant.utils.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            UserService userService,
            JwtUtil jwtUtil
    ) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthTokenResponse register(String username, String password) {
        User user = userService.register(username, password);
        return createTokenResponse(user);
    }

    @Override
    public AuthTokenResponse login(String username, String password) {
        User user = userService.login(username, password);
        return createTokenResponse(user);
    }

    private AuthTokenResponse createTokenResponse(User user) {
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername()
        );

        return new AuthTokenResponse(token, user.getUsername(), user.getRole());
    }
}
