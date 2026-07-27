package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AuthTokenResponse;
import com.example.aiinterviewassistant.entity.User;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.service.impl.AuthServiceImpl;
import com.example.aiinterviewassistant.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldRegisterUserAndReturnTokenResponse() {
        User user = user(1L, "alice");
        when(userService.register("alice", "password123")).thenReturn(user);
        when(jwtUtil.generateToken(1L, "alice")).thenReturn("register-token");

        AuthTokenResponse response = authService.register("alice", "password123");

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getToken()).isEqualTo("register-token");
        verify(userService).register("alice", "password123");
        verify(jwtUtil).generateToken(1L, "alice");
    }

    @Test
    void shouldLoginUserAndReturnTokenResponse() {
        User user = user(2L, "bob");
        when(userService.login("bob", "password123")).thenReturn(user);
        when(jwtUtil.generateToken(2L, "bob")).thenReturn("login-token");

        AuthTokenResponse response = authService.login("bob", "password123");

        assertThat(response.getUsername()).isEqualTo("bob");
        assertThat(response.getToken()).isEqualTo("login-token");
        verify(userService).login("bob", "password123");
        verify(jwtUtil).generateToken(2L, "bob");
    }

    @Test
    void shouldNotGenerateTokenWhenLoginFails() {
        when(userService.login("alice", "wrong-password"))
                .thenThrow(new BusinessException(401, "invalid credentials"));

        assertThatThrownBy(() -> authService.login("alice", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("invalid credentials");

        verify(userService).login("alice", "wrong-password");
        verifyNoInteractions(jwtUtil);
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
