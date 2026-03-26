package com.project.shift.auth.service;

import com.project.shift.auth.dao.AuthDAO;
import com.project.shift.auth.dto.LoginResponseDTO;
import com.project.shift.global.jwt.JwtService;
import com.project.shift.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2UserProcessService {

    private final AuthDAO authDao;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponseDTO processOAuth2User(String email, String name, String provider) {
        UserEntity foundUser;

        try {
            foundUser = authDao.getUser(UserEntity.builder().loginId(email).build());
        } catch (RuntimeException e) {
            foundUser = UserEntity.builder()
                    .loginId(email)
                    .email(email)
                    .name(name)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .provider(provider)
                    .build();
            authDao.updateUser(foundUser);
        }
        return createLoginResponse(foundUser);
    }

    private LoginResponseDTO createLoginResponse(UserEntity user) {
        String accessToken = jwtService.createAccessToken(user.getUserId(), user.getName());
        String refreshToken = jwtService.createRefreshToken(user.getUserId());
        user.setRefreshToken(refreshToken);
        authDao.updateUser(user);
        return new LoginResponseDTO(accessToken, refreshToken);
    }
}
