package com.project.shift.auth;

import com.project.shift.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TokenBlacklistServiceTest {

    @Autowired
    TokenBlacklistService blacklistService;
    @Autowired
    StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.delete(redisTemplate.keys("blacklist:*"));
    }

    @Test
    @DisplayName("블랙리스트에 넣은 토큰만 막히고 다른 토큰은 통과한다")
    void blacklistedTokenOnly() {
        blacklistService.blacklistToken("token-a", Duration.ofMinutes(10));

        assertThat(blacklistService.isBlacklisted("token-a", 1L)).isTrue();
        assertThat(blacklistService.isBlacklisted("token-b", 1L)).isFalse();
    }

    @Test
    @DisplayName("사용자를 블랙리스트에 넣으면 그 사용자의 모든 토큰이 막힌다")
    void blacklistedUserBlocksAllTokens() {
        blacklistService.blacklistUser(1L, Duration.ofMinutes(30));

        assertThat(blacklistService.isBlacklisted("any-token", 1L)).isTrue();
        assertThat(blacklistService.isBlacklisted("any-token", 2L)).isFalse();
    }

    @Test
    @DisplayName("토큰은 남은 시간만큼만 저장된다")
    void tokenExpiresAfterRemainingTime() {
        blacklistService.blacklistToken("token-a", Duration.ofMinutes(10));

        Long ttlSeconds = redisTemplate.getExpire("blacklist:token:token-a");
        assertThat(ttlSeconds).isPositive().isLessThanOrEqualTo(600);
    }

    @Test
    @DisplayName("이미 만료된 토큰은 저장하지 않는다")
    void expiredTokenIsNotStored() {
        blacklistService.blacklistToken("token-a", Duration.ZERO);

        assertThat(redisTemplate.hasKey("blacklist:token:token-a")).isFalse();
    }
}