package com.project.shift.auth.controller;

import com.project.shift.auth.dto.request.LoginRequest;
import com.project.shift.auth.dto.response.LoginResponse;
import com.project.shift.auth.service.AuthService;
import com.project.shift.global.exception.BadRequestException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final String HEADER = "Authorization";
    private final String TOKEN_HEADER = "Bearer ";

    private final AuthService authService;

    // 로그인 기능
    @PostMapping("/login")
    public ResponseEntity<?> userLogin(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        log.info("[AUTH] 로그인 시도 User ID: {}", request.loginId());

        LoginResponse tokens = authService.login(request);
        log.info("[AUTH] 로그인 성공 User ID: {}", request.loginId());

        ResponseCookie cookie = createRefreshTokenCookie(tokens.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        authService.logout();

        // 로그아웃 시 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .path("/auth/refresh")
                .maxAge(0) // 즉시 만료
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .header("Clear-Site-Data", "\"cookies\", \"storage\", \"cache\"") // 좀비 쿠키 방지를 위한 추가 헤더
                .body(Map.of("message", "로그아웃이 정상적으로 처리되었습니다."));
    }

    // Access 토큰 재발급 기능
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = HEADER, required = false) String authorizationHeader,
                                          @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        // 쿠키 유효성 검사
        if (refreshToken == null) {
            throw new BadRequestException("리프레시 토큰이 존재하지 않습니다.");
        }

        // 헤더 유효성 검사
        if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_HEADER)) {
            throw new BadRequestException("Access Token이 올바르지 않습니다.");
        }

        // 헤더에서 토큰 추출
        String accessToken = authorizationHeader.replace(TOKEN_HEADER, "");

        // 토큰 재발급 서비스 호출
        LoginResponse tokens = authService.refresh(accessToken, refreshToken);

        // 새로운 리프레시 토큰 쿠키 생성
        ResponseCookie newRefreshCookie = createRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(Map.of("accessToken", tokens.accessToken()));
    }

    // 리프레시 토큰 쿠키 생성
    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // JavaScript에서 접근 불가
                .secure(false)
                .path("/auth/refresh") // 특정 경로에서만 전송
                .maxAge(7 * 24 * 60 * 60) // 7일
                .sameSite("Lax") // CSRF 방어
                .build();
    }
}
