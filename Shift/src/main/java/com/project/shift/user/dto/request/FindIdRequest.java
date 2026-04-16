package com.project.shift.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FindIdRequest(
        @NotBlank(message = "이름은 필수 입력 항목입니다.")
        String name,

        @NotBlank(message = "연락처는 필수 입력 항목입니다.")
        @Pattern(regexp = "^[0-9]{11}$", message = "연락처는 11자리 숫자만 입력 가능합니다.")
        String phone
) {
}
