package com.project.shift.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
