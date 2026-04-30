package com.project.shift.chat.controller;

import com.project.shift.chat.dto.MessageDTO;
import com.project.shift.chat.dto.request.MessageUserDTO;
import com.project.shift.chat.dto.response.ChatroomListDTO;
import com.project.shift.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/send")
    public void sendMessage(@Payload MessageUserDTO dto) {
        LocalDateTime now = dto.getMessageDTO().getSendDate();
        messageService.checkAndUpdateReceiverConnectionStatus(dto, now);
        messageService.sendAndSaveMessage(dto.getMessageDTO(), dto.getChatroomUserDTO());
    }

    // 채팅방 최초 생성 시간 이후의 채팅 메시지 기록 반환
    @PostMapping("/history")
    public List<MessageDTO> getMessageHistory(@RequestBody ChatroomListDTO chatroomListDto) {
        return messageService.getMessageHistory(chatroomListDto);
    }

}