package com.project.shift.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.shift.auth.dto.request.LoginRequest;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogoutTokenTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;

    static final String LOGIN_ID = "logout_user";
    static final String PASSWORD = "pass1234";

    Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(UserEntity.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .name("테스터")
                .build()).getUserId();
    }

    @AfterEach
    void tearDown() {
        // 탈퇴하면 @SQLRestriction 때문에 JPA로는 안 보여서 SQL로 직접 지움
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(LOGIN_ID, PASSWORD));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    @DisplayName("로그아웃한 Access Token으로는 요청할 수 없다")
    void cannotUseAccessTokenAfterLogout() throws Exception {
        String accessToken = login();

        // 로그아웃 전에는 정상적으로 쓸 수 있어야 함
        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 로그아웃 후 같은 토큰으로 요청하면 막혀야 함
        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴한 사용자의 Access Token으로는 요청할 수 없다")
    void cannotUseAccessTokenAfterWithdrawal() throws Exception {
        String accessToken = login();

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }
}