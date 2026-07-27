package com.example.aiinterviewassistant.utils;

import com.example.aiinterviewassistant.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
public class UserContext {

    private static final String SCORE_STREAM_PATH = "/api/question/score/stream";

    private final JwtUtil jwtUtil;

    public UserContext(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public Long getCurrentUserId() {
        Long userId = getUserIdFromSecurityContext();
        if (userId != null) {
            return userId;
        }

        return getSseUserIdFromQueryParameter();
    }

    private Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserPrincipal jwtUserPrincipal) {
            return jwtUserPrincipal.userId();
        }

        return null;
    }

    private Long getSseUserIdFromQueryParameter() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String expectedPath = request.getContextPath() + SCORE_STREAM_PATH;
        if (!expectedPath.equals(request.getRequestURI())) {
            return null;
        }

        String token = request.getParameter("token");
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            return null;
        }

        return jwtUtil.getUserIdFromToken(token);
    }
}
