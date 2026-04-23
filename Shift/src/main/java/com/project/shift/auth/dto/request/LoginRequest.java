package com.project.shift.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(max = 20, message = "아이디 형식이 올바르지 않습니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(max = 24, message = "비밀번호 형식이 올바르지 않습니다.")
        String password
) {}
