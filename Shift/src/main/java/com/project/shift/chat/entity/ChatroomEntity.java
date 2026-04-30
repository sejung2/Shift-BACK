package com.project.shift.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHATROOMS")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatroomEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "SEQ_CHATROOMS"
    )
    @SequenceGenerator(
            name = "SEQ_CHATROOMS",
            sequenceName = "SEQ_CHATROOMS",
            allocationSize = 1
    )
    @Column(name = "CHATROOM_ID", nullable = false)
    private Long chatroomId;

    @Column(name = "LAST_MSG_CONTENT")
    private String lastMsgContent;

    @Column(name = "LAST_MSG_DATE")
    private LocalDateTime lastMsgDate;
}