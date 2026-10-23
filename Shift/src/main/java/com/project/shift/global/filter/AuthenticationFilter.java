package com.project.shift.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.shift.auth.service.TokenBlacklistService;
import com.project.shift.global.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter { // 모든 API 요청마다 한 번만 실행됨

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtService.extractTokenFromRequest(request);
        Long userId = null;

        if (token != null && jwtService.isValidToken(token) && jwtService.isAccessToken(token)) {
            userId = jwtService.extractUserIdFromValidToken(token);
        }

        // 토큰이 없거나 만료된 경우 -> 다음 필터로 넘김
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Redis에 못 물어보면 막되, 401이 아니라 503으로 응답
        boolean blacklisted;
        try {
            blacklisted = tokenBlacklistService.isBlacklisted(token, userId);
        } catch (DataAccessException e) {
            log.error("[AUTH] Redis 연결 실패로 블랙리스트 확인 불가 userId: {}", userId, e);
            writeError(response, request, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable",
                    "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        // 로그아웃한 토큰이거나 탈퇴한 사용자면 막음
        if (tokenBlacklistService.isBlacklisted(token, userId)) {
            log.warn("[AUTH] 로그아웃했거나 탈퇴한 사용자의 토큰 userId: {}", userId);
            writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "로그아웃했거나 탈퇴한 회원입니다.");
            return;
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request,
                            int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getServletPath());

        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}