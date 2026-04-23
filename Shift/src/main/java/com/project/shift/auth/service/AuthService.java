package com.project.shift.auth.service;

import com.project.shift.auth.dto.request.LoginRequest;
import com.project.shift.auth.dto.response.LoginResponse;
import com.project.shift.auth.entity.RefreshTokenEntity;
import com.project.shift.auth.repository.RefreshTokenRepository;
import com.project.shift.global.exception.BadRequestException;
import com.project.shift.global.exception.NotFoundException;
import com.project.shift.global.jwt.JwtService;
import com.project.shift.global.security.CurrentUser;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 로그인
    @Transactional
    public LoginResponse login(LoginRequest loginInfo) {
        // 입력값 검증 수행
        UsernamePasswordAuthenticationToken cred = new UsernamePasswordAuthenticationToken(
                loginInfo.loginId(),
                loginInfo.password()
        );

        Authentication authentication = authenticationManager.authenticate(cred);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // dto -> entity로 변환
        UserEntity foundUser = userRepository.findByLoginId(loginInfo.loginId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Long userId = foundUser.getUserId();
        String name = foundUser.getName();

        log.info("[AUTH] 인증 성공, 토큰 발급 및 리프레시 토큰 갱신 시작 UserId: {}", userId);

        String accessToken = jwtService.createAccessToken(userId, name);
        String refreshToken = jwtService.createRefreshToken(userId);
        LocalDateTime expiredAt = jwtService.getRefreshTokenExpiration();

        refreshTokenRepository.findByUser_UserId(userId)
                .ifPresentOrElse(
                        existingToken -> existingToken.refreshToken(refreshToken, expiredAt),
                        () -> refreshTokenRepository.save(
                                RefreshTokenEntity.builder()
                                        .userEntity(foundUser)
                                        .tokenValue(refreshToken)
                                        .expiredAt(expiredAt)
                                        .build()
                        )
                );

        log.info("[AUTH] 리프레시 토큰 갱신 완료 UserId: {}", userId);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout() {
        Long userId = CurrentUser.getUserId();

        log.info("[AUTH] 로그아웃 시작 UserId: {}", userId);

        // DB의 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUser_UserId(userId);

        SecurityContextHolder.clearContext();

        log.info("[AUTH] 로그아웃 완료 UserId: {}", userId);
    }

    // 토큰 재발급
    @Transactional
    public LoginResponse refresh(String accessToken, String refreshToken) {
        // refresh token 검증
        validateRefreshToken(refreshToken);

        // 토큰의 값(userId)이 서로 일치하는지 체크
        Long userId = validateTokenPair(accessToken, refreshToken);

        // RefreshTokenEntity에서 검증
        RefreshTokenEntity storedToken = refreshTokenRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new NotFoundException("저장된 리프레시 토큰이 없습니다."));

        if (!storedToken.getTokenValue().equals(refreshToken)) {
            throw new BadRequestException("리프레시 토큰이 일치하지 않습니다.");
        }

        UserEntity foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtService.createAccessToken(foundUser.getUserId(), foundUser.getName());
        String newRefreshToken = jwtService.createRefreshToken(foundUser.getUserId());
        LocalDateTime newExpiredAt = jwtService.getRefreshTokenExpiration();

        // 토큰 갱신
        storedToken.refreshToken(newRefreshToken, newExpiredAt);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    private void validateRefreshToken(String refreshToken) {
        // 토큰 유효성 체크
        if (!jwtService.isValidToken(refreshToken)) {
            throw new BadRequestException("유효하지 않은 리프레시 토큰입니다.");
        }
        // 토큰 타입이 refresh 인지 체크
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("토큰 타입이 리프레시 토큰이 아닙니다.");
        }
    }

    private Long validateTokenPair(String accessToken, String refreshToken) {
        Long userIdFromAccess = jwtService.extractUserIdFromExpiredValidToken(accessToken);
        if (userIdFromAccess == null) {
            throw new BadRequestException("신뢰할 수 없는 엑세스 토큰입니다.");
        }

        Long userIdFromRefresh = jwtService.extractUserIdFromValidToken(refreshToken);

        // 두 토큰의 짝이 맞는지 체크
        if (!userIdFromAccess.equals(userIdFromRefresh)) {
            throw new BadRequestException("토큰이 서로 일치하지 않습니다.");
        }
        return userIdFromRefresh;
    }
}
