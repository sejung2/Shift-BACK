package com.project.shift.chat.dto;

import java.time.LocalDateTime;

public interface MessageSearchResultProjection {

    Long getChatroomUserId();

    Long getChatroomId();

    String getChatroomName();

    LocalDateTime getLastConnectionTime();

    LocalDateTime getCreatedTime();

    String getConnectionStatus();

    String getIsDarkMode();

    String getMessage();

    LocalDateTime getSendDate();

    Long getReceiverId();
}
