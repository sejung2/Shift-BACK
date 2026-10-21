package com.project.shift.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.shift.auth.dto.request.LoginRequest;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogoutTokenTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;

    static final String LOGIN_ID = "logout_user";
    static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        userRepository.save(UserEntity.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .name("테스터")
                .build());
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
}