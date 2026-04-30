package com.project.shift.chat.entity;

import com.project.shift.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "CHATROOM_USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatroomUserEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "SEQ_CHATROOM_USERS"
    )
    @SequenceGenerator(
            name = "SEQ_CHATROOM_USERS",
            sequenceName = "SEQ_CHATROOM_USERS",
            allocationSize = 1
    )
    @Column(name = "CHATROOM_USERS_ID", nullable = false)
    private Long chatroomUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHATROOM_ID", nullable = false)
    private ChatroomEntity chatroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @Column(name = "CHATROOM_NAME", length = 30)
    private String chatroomName;

    @Column(name = "LAST_CONNECTION_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastConnectionTime;

    @Column(name = "CREATED_TIME")
    private LocalDateTime createdTime;

    @Column(name = "CONNECTION_STATUS", nullable = false, length = 2)
    private String connectionStatus;

    @Column(name = "IS_DARK_MODE", nullable = false, length = 1, columnDefinition = "CHAR(1) default 'N'")
    private String isDarkMode;
}