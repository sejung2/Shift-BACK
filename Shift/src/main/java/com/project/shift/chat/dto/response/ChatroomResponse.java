package com.project.shift.chat.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomResponse {

    private long chatroomId;
    private String lastMsgContent;
    private LocalDateTime lastMsgDate;
}
