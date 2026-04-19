package com.project.shift.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRequest(
        @NotBlank(message = "이름을 입력해야 합니다.")
        @Size(min = 2, max = 6, message = "이름은 2자 이상 6자 이하로 입력해야 합니다.")
        @Pattern(regexp = "^[가-힣\\s]+$", message = "이름은 한글만 사용할 수 있습니다.")
        String name,

        @NotBlank(message = "연락처를 입력해야 합니다.")
        @Pattern(regexp = "^[0-9]{11}$", message = "연락처는 11자리 숫자만 입력 가능합니다.")
        String phone,

        @Size(max = 200)
        String address
) {
}
