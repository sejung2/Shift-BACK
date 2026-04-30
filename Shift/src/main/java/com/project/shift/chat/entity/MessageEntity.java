package com.project.shift.chat.entity;

import com.project.shift.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MESSAGES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MessageEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "SEQ_MESSAGES"
    )
    @SequenceGenerator(
            name = "SEQ_MESSAGES",
            sequenceName = "SEQ_MESSAGES",
            allocationSize = 1
    )
    @Column(name = "MESSAGE_ID", nullable = false)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHATROOM_ID", nullable = false)
    private ChatroomEntity chatroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @Column(name = "SEND_DATE")
    private LocalDateTime sendDate;

    @Column(name = "CONTENT", nullable = false, length = 300)
    private String content;

    @Column(name = "IS_GIFT", nullable = false, length = 1, columnDefinition = "CHAR(1) default 'N'")
    private String isGift;

    @Column(name = "UNREAD_COUNT", nullable = false)
    private int unreadCount;
}