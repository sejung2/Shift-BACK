package com.project.shift.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeletedChatroomUserInfoRequest(
        @NotNull
        Long chatroomId,
        @NotNull
        Long senderId,
        @NotNull
        Long receiverId,
        @NotBlank
        String senderName,
        @NotBlank
        String receiverName
) {}
