package com.project.shift.chat.dto;

import jakarta.persistence.Transient;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    @Transient //DB와 매핑하지 않는 필드
    @Setter
    private MessageType type;

    private long messageId;
    @Setter
    private long chatroomId;
    @Setter
    private long userId;
    @Setter
    private LocalDateTime sendDate;
    @Setter
    private String content;
    @Setter
    private String isGift;
    @Setter
    private int unreadCount;
}
