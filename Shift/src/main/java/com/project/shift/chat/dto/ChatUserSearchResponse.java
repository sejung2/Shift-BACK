package com.project.shift.chat.dto;

public record ChatUserSearchResponse(
        boolean ifFriend,
        long userId,
        String loginId,
        String name,
        String phone
) {
}
