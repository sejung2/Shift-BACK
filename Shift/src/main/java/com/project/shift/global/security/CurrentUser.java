package com.project.shift.global.security;

import com.project.shift.global.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    // null 반환(선택적으로 userId가 필요한 경우)
    public static Long getUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        return null;
    }

    // 예외 던짐(반드시 인증이 필요한 경우)
    public static Long getUserId() {
        Long userId = getUserIdOrNull();
        if (userId == null) throw new BadRequestException("인증 정보가 없습니다.");
        return userId;
    }
}
