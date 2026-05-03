package com.project.shift.chat.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSearchResultResponse {

    private long chatroomUserId;
    private long chatroomId;
    private LocalDateTime lastConnectionTime;
    private LocalDateTime createdTime;
    private String connectionStatus;
    private String isDarkMode;
    private String chatroomName;
    @Setter
    private int unreadCount;
    private String message;
    private LocalDateTime sendDate;
    private long receiverId;
}
