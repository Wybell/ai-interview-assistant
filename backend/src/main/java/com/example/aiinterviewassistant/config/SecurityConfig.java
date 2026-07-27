package com.example.aiinterviewassistant.config;

import com.example.aiinterviewassistant.security.JwtAuthenticationFilter;
import com.example.aiinterviewassistant.security.RestSecurityExceptionHandler;
import com.example.aiinterviewassistant.utils.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final RestSecurityExceptionHandler restSecurityExceptionHandler;

    public SecurityConfig(
            JwtUtil jwtUtil,
            RestSecurityExceptionHandler restSecurityExceptionHandler
    ) {
        this.jwtUtil = jwtUtil;
        this.restSecurityExceptionHandler = restSecurityExceptionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/login.html"),
                                new AntPathRequestMatcher("/register.html"),
                                new AntPathRequestMatcher("/index.html"),
                                new AntPathRequestMatcher("/test.html"),
                                new AntPathRequestMatcher("/stream_test.html"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/webjars/**")
                        ).permitAll()
                        .requestMatchers(
                                new AntPathRequestMatcher("/swagger-ui.html"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs"),
                                new AntPathRequestMatcher("/v3/api-docs/**")
                        ).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restSecurityExceptionHandler)
                        .accessDeniedHandler(restSecurityExceptionHandler)
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class
                )
                .httpBasic().disable()
                .formLogin().disable();
        return http.build();
    }
}
