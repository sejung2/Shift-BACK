package com.project.shift.auth;

import com.project.shift.auth.service.TokenBlacklistService;
import com.project.shift.global.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedisDownTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Redis에 연결할 수 없으면 401이 아니라 503으로 응답한다")
    void respondsServiceUnavailableWhenRedisIsDown() throws Exception {
        given(tokenBlacklistService.isBlacklisted(anyString(), anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis down"));

        String accessToken = jwtService.createAccessToken(1L, "테스터");

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isServiceUnavailable());
    }
}