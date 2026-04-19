package com.project.shift.user.dto.response;

public record UserInfoResponse(
        Long userId,
        String loginId,
        String name,
        String phone,
        String address,
        Integer points

) {
}
