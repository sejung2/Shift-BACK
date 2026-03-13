package com.project.shift.global.oauth2;

import com.project.shift.global.jwt.JwtService;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .loginId(email)
                            .email(email)
                            .provider("GOOGLE")
                            .name(name)
                            .password(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString())) // 랜덤 비밀번호 설정
                            .adminFlag("N")
                            .points(0)
                            .build();
                    return userRepository.save(newUser);
                });

        String accessToken = jwtService.createAccessToken(user.getUserId(), user.getName());
        String refreshToken = jwtService.createRefreshToken(user.getUserId());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        System.out.println("구글 로그인 성공 발급된 액세스 토큰: " + accessToken);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
