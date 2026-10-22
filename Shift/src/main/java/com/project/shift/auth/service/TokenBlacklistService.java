package com.project.shift.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String TOKEN_PREFIX = "blacklist:token:";
    private static final String USER_PREFIX = "blacklist:user:";

    private final StringRedisTemplate redisTemplate;

    // 로그아웃한 토큰을 남은 시간만큼 막음
    public void blacklistToken(String token, Duration remaining) {
        if (remaining.isZero() || remaining.isNegative()) {
            return; // 이미 만료된 토큰은 넣을 필요 없음
        }
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, "logout", remaining);
    }

    // 탈퇴한 사용자의 토큰을 전부 막음
    public void blacklistUser(Long userId, Duration duration) {
        redisTemplate.opsForValue().set(USER_PREFIX + userId, "withdrawn", duration);
    }

    public boolean isBlacklisted(String token, Long userId) {
        return redisTemplate.hasKey(TOKEN_PREFIX + token)
                || redisTemplate.hasKey(USER_PREFIX + userId);
    }
}