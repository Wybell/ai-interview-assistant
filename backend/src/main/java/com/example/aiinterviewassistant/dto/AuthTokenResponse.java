package com.example.aiinterviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokenResponse {

    private String token;

    private String username;
}