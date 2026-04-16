package com.project.shift.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// 단일 필드 체크용 DTO(loginId, phone, password 중복/규칙 확인을 위함)
public record CheckRequest(
        @NotBlank
        String value
) {
}
