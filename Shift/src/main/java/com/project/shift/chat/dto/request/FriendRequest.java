package com.project.shift.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record FriendRequest(
        @NotNull
        Long userId,       // 사용자 ID
        @NotNull
        Long friendId     // 친구 ID
) {}
