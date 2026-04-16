package com.project.shift.user.dto.request;

import jakarta.validation.constraints.*;

// 회원가입
public record SignUpRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 설정해야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해야 합니다.")
        @Size(min = 8, max = 24, message = "비밀번호는 8자 이상 24자 이하로 설정해야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()])[A-Za-z0-9!@#$%^&*()]+$",
                message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 이상 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "이름을 입력해야 합니다.")
        @Size(min = 2, max = 6, message = "이름은 2자 이상 6자 이하로 입력해야 합니다.")
        @Pattern(regexp = "^[가-힣\\s]+$", message = "이름은 한글만 사용할 수 있습니다.")
        String name,

        @Pattern(regexp = "^[0-9]{11}$", message = "연락처는 11자리 숫자만 입력 가능합니다.")
        String phone,

        @Size(max = 200)
        String address,

        @NotNull(message = "이용약관에 동의해야 합니다.")
        @AssertTrue(message = "이용약관에 동의해야 합니다.")
        Boolean termsAgreed
) {}
