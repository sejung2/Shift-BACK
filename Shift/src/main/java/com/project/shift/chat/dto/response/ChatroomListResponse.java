package com.project.shift.chat.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomListResponse {

    private long chatroomUserId;
    private long chatroomId;
    private long lastMsgSender;
    private String lastMsgContent;
    private LocalDateTime lastMsgDate;
    private LocalDateTime lastConnectionTime;
    private LocalDateTime createdTime;
    private String connectionStatus;
    private String isDarkMode;
    private String chatroomName;
    @Setter
    private int unreadCount;
    private long receiverId;
    private String receiverName;

}
