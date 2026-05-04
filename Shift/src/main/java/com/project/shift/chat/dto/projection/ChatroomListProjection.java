package com.project.shift.chat.dto.projection;

import java.time.LocalDateTime;

public interface ChatroomListProjection {
    Long getChatroomUserId();

    Long getChatroomId();

    String getChatroomName();

    String getLastMsgContent();

    Long getLastMsgSender();

    LocalDateTime getLastMsgDate();

    LocalDateTime getLastConnectionTime();

    LocalDateTime getCreatedTime();

    String getConnectionStatus();

    String getIsDarkMode();

    Long getReceiverId();

    String getReceiverName();

    Integer getUnreadCount();
}
