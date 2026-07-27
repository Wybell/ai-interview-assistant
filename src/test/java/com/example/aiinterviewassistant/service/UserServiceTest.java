package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.entity.User;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, new BCryptPasswordEncoder());
    }

    @Test
    void shouldEncodePasswordAndInsertNewUser() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        userService.register("alice", "password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("alice");
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(new BCryptPasswordEncoder().matches(
                "password123",
                savedUser.getPassword()
        )).isTrue();
    }

    @Test
    void shouldRejectRegistrationWhenUsernameAlreadyExists() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.register("alice", "password123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void shouldReturnUserWhenCredentialsAreValid() {
        User user = user("alice", "password123");
        when(userMapper.selectOne(any())).thenReturn(user);

        User actual = userService.login("alice", "password123");

        assertThat(actual).isSameAs(user);
        verify(userMapper).selectOne(any());
    }

    @Test
    void shouldRejectLoginWhenUserDoesNotExist() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> userService.login("alice", "password123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        User user = user("alice", "correct-password");
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login("alice", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    private User user(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        return user;
    }
}
