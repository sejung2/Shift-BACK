package com.project.shift.global.oauth2;

import com.project.shift.auth.dto.LoginResponseDTO;
import com.project.shift.auth.service.OAuth2UserProcessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2UserProcessService oAuth2UserProcessService;

    // 설정값이 없으면 http://localhost:3000을 기본값으로 사용
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 소셜 정보 및 provider 추출
        OAuth2AuthenticationToken auth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = auth2Token.getAuthorizedClientRegistrationId();

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        log.info("[OAuth2] {} 로그인 성공 - 유저 이메일 : {}", provider, email);

        LoginResponseDTO tokens = oAuth2UserProcessService.processOAuth2User(email, name, provider);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh")
                .maxAge(7 * 24 * 60 * 60) // 7일
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .fragment("accessToken=" + tokens.accessToken())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
