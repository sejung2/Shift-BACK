package com.project.shift.chat.dto.response;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import com.project.shift.chat.entity.ChatroomEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomDTO {

    private long chatroomId;
    private String lastMsgContent;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMsgDate;

    // Entity -> DTO 변환
    public static ChatroomDTO toDto(ChatroomEntity entity) {
        return ChatroomDTO.builder()
                .chatroomId(entity.getChatroomId())
                .lastMsgContent(entity.getLastMsgContent())
                .lastMsgDate(entity.getLastMsgDate())
                .build();
    }
    
    // 채팅방 생성시 사용할 생성자
    ChatroomDTO(String lastMsgContent){
    	this.lastMsgContent = lastMsgContent;
    }
    
}
