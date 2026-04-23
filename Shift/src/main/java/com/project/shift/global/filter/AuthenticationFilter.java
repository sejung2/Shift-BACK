package com.project.shift.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.shift.global.jwt.JwtService;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtService.extractTokenFromRequest(request);
        Long userId = null;

        if (token != null) {
            // 토큰이 유효한지 검사
            if (jwtService.isValidToken(token)) {
                userId = jwtService.extractUserIdFromValidToken(token);
            }
        }

        if (userId != null) {
            UserEntity userEntity = userRepository.findById(userId).orElse(null);

            if (userEntity != null) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } else {
                log.warn("[AUTH] 탈퇴했거나 존재하지 않는 사용자 userId: {}", userId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                Map<String, Object> body = new HashMap<>();
                body.put("status", 401);
                body.put("error", "Unauthorized");
                body.put("message", "탈퇴했거나 존재하지 않는 회원입니다.");
                body.put("path", request.getServletPath());

                new ObjectMapper().writeValue(response.getOutputStream(), body);
            }
        } else {
            // 토큰이 없거나 만료된 경우 -> 다음 필터로 넘김
            filterChain.doFilter(request, response);
        }
    }
}
