package com.project.shift.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService {

    static final String PREFIX = "Bearer ";
    static final String TOKEN_TYPE_ACCESS = "access";
    static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final SignatureAlgorithm signatureAlgorithm;

    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtService(
            SecretKey secretKey,
            SignatureAlgorithm signatureAlgorithm,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs
    ) {
        this.secretKey = secretKey;
        this.signatureAlgorithm = signatureAlgorithm;
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String name) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", TOKEN_TYPE_ACCESS)
                .claim("name", name)
                .setIssuedAt(new Date()) // 토큰 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidityMs))
                .signWith(secretKey, signatureAlgorithm)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", TOKEN_TYPE_REFRESH)
                .setIssuedAt(new Date()) // 토큰 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidityMs))
                .signWith(secretKey, signatureAlgorithm)
                .compact();
    }

    // Token 만료 시간 계산
    public LocalDateTime getRefreshTokenExpiration() {
        return LocalDateTime.now().plus(refreshTokenValidityMs, ChronoUnit.MILLIS);
    }

    // 헤더에서 토큰 추출
    public String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }

    // 유효한 토큰에서만 userId 추출
    public Long extractUserIdFromValidToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    // 만료된 토큰 포함 모든 토큰에서 userId 추출 (재발급 시 사용)
    public Long extractUserIdFromExpiredValidToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            return Long.parseLong(e.getClaims().getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    // 토큰 유효성 체크
    public boolean isValidToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰 재발급 시 Refresh Token 인지 체크
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return TOKEN_TYPE_REFRESH.equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }
}
