package com.project.shift.auth;

import com.project.shift.global.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenMisuseTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;

    @Test
    @DisplayName("Access Token으로는 요청할 수 있다")
    void accessTokenIsAccepted() throws Exception {
        String accessToken = jwtService.createAccessToken(1L, "테스터");

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Refresh Token을 Access Token 자리에 넣으면 요청할 수 없다")
    void refreshTokenIsRejected() throws Exception {
        String refreshToken = jwtService.createRefreshToken(1L);

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }
}