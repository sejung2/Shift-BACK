package com.project.shift.chat.dto.response;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomListDTO {

	private long chatroomUserId;
    private long chatroomId;
    private long lastMsgSender;
    private String lastMsgContent;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMsgDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastConnectionTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;
    private String connectionStatus;
    private String isDarkMode;
    private String chatroomName;
    @Setter
    private int unreadCount;
    private long receiverId;
    private String receiverName;
    
}
