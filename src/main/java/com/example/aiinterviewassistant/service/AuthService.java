package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AuthTokenResponse;

public interface AuthService {

    AuthTokenResponse register(String username, String password);

    AuthTokenResponse login(String username, String password);
}
