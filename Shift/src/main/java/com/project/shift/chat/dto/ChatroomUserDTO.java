package com.project.shift.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomUserDTO {

    // 0이 기본값으로 들어가는 방지하기 위해서 long → Long으로 타입 변경
    private Long chatroomUserId;
    private long chatroomId;
    private long userId;
    private String chatroomName;
    private LocalDateTime lastConnectionTime;
    private LocalDateTime createdTime;
    private String connectionStatus;
    private String isDarkMode;
}
