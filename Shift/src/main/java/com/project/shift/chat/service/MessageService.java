package com.project.shift.chat.service;

import com.project.shift.chat.dto.ChatroomUserDTO;
import com.project.shift.chat.dto.MessageDTO;
import com.project.shift.chat.dto.request.MessageUserDTO;
import com.project.shift.chat.dto.response.ChatroomListDTO;
import com.project.shift.chat.entity.MessageEntity;
import com.project.shift.chat.repository.ChatUserRepository;
import com.project.shift.chat.repository.ChatroomRepository;
import com.project.shift.chat.repository.ChatroomUserRepository;
import com.project.shift.chat.repository.MessageRepository;
import com.project.shift.global.exception.NotFoundException;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatroomUserRepository chatroomUserRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 메시지 DB 저장
    @Transactional
    public void addMessage(MessageDTO message) {
        messageRepository.save(MessageEntity.builder()
                .chatroom(chatroomRepository.getReferenceById(message.getChatroomId()))
                .user(userRepository.getReferenceById(message.getUserId()))
                .sendDate(message.getSendDate())
                .content(message.getContent())
                .isGift(message.getIsGift())
                .unreadCount(message.getUnreadCount())
                .build());
    }

    // 채팅방 최초 접속 시간 이후 모든 채팅방 메시지 반환
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessageHistory(ChatroomListDTO dto) {
        return messageRepository.findByChatroomId(dto.getChatroomId(), dto.getCreatedTime())
                .stream()
                .map(entity -> MessageDTO.builder()
                        .messageId(entity.getMessageId())
                        .chatroomId(entity.getChatroom().getChatroomId())
                        .userId(entity.getUser().getUserId())
                        .sendDate(entity.getSendDate())
                        .content(entity.getContent())
                        .isGift(entity.getIsGift())
                        .unreadCount(entity.getUnreadCount())
                        .build())
                .toList();
    }

    // 채팅 메시지 전송시 메시지 DB에 저장 및 브로드캐스팅
    @Transactional
    public void sendAndSaveMessage(MessageDTO messageDTO, ChatroomUserDTO chatroomUserDTO) {
        if (chatroomUserDTO.getConnectionStatus().equals("DL")) {
            LocalDateTime now = LocalDateTime.now();
            chatroomUserDTO.setLastConnectionTime(now);

            long receiverId = chatroomUserRepository.getReceiverId(
                    chatroomUserDTO.getChatroomId(),
                    chatroomUserDTO.getUserId()
            ).getFirst();

            UserEntity receiver = chatUserRepository.findById(receiverId)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

            chatroomUserRepository.restoreChatroomUser(
                    chatroomUserDTO.getChatroomId(),
                    chatroomUserDTO.getUserId(),
                    "OF",
                    now,
                    receiver.getName() + "님과의 채팅방"
            );
        }

        if (messageDTO.getSendDate() == null) {
            messageDTO.setSendDate(LocalDateTime.now());
        }

        switch (messageDTO.getType()) {
            case JOIN -> {
                chatroomUserDTO.setConnectionStatus("ON");
                messageRepository.markMessagesAsRead(
                        messageDTO.getChatroomId(),
                        chatroomUserDTO.getLastConnectionTime(),
                        chatroomUserDTO.getUserId()
                );
                chatroomUserDTO.setLastConnectionTime(LocalDateTime.now());
                chatroomUserRepository.updateChatUserInfo(
                        chatroomUserDTO.getConnectionStatus(),
                        chatroomUserDTO.getLastConnectionTime(),
                        chatroomUserDTO.getChatroomUserId()
                );
            }
            case LEAVE -> {
                chatroomUserDTO.setConnectionStatus("OF");
                chatroomUserDTO.setLastConnectionTime(LocalDateTime.now());
                chatroomUserRepository.updateChatUserInfo(
                        chatroomUserDTO.getConnectionStatus(),
                        chatroomUserDTO.getLastConnectionTime(),
                        chatroomUserDTO.getChatroomUserId()
                );
            }
            case CHAT -> {
                setUnreadCount(messageDTO, chatroomUserDTO);
                messageRepository.save(MessageEntity.builder()
                        .chatroom(chatroomRepository.getReferenceById(chatroomUserDTO.getChatroomId()))
                        .user(userRepository.getReferenceById(messageDTO.getUserId()))
                        .sendDate(messageDTO.getSendDate())
                        .content(messageDTO.getContent())
                        .isGift(messageDTO.getIsGift())
                        .unreadCount(messageDTO.getUnreadCount())
                        .build());
                updateChatroomInfo(messageDTO, chatroomUserDTO);
                broadcastToChatUser(chatroomUserDTO);
            }
            default -> {
            }
        }

        broadcastToChatroom(messageDTO);
    }

    // 채팅방의 마지막 메시지와 시간을 업데이트
    private void updateChatroomInfo(MessageDTO messageDTO, ChatroomUserDTO chatroomUserDTO) {
        chatroomRepository.findById(chatroomUserDTO.getChatroomId())
                .ifPresent(chatroom ->
                        chatroomRepository.updateLastMsgAndDate(
                                chatroom.getChatroomId(),
                                messageDTO.getContent(),
                                messageDTO.getSendDate()
                        ));
    }

    // 현재 채팅방에 온라인 상태인 유저의 수를 구해서 메시지의 unreadCount를 세팅
    private void setUnreadCount(MessageDTO messageDTO, ChatroomUserDTO chatroomUserDTO) {
        int unreadCount = Math.max(
                messageDTO.getUnreadCount() - chatroomUserRepository.countOtherUsersOnline(
                        chatroomUserDTO.getChatroomId(),
                        chatroomUserDTO.getUserId()
                ), 0);
        messageDTO.setUnreadCount(unreadCount);
    }

    // 받는 사람들에게 메시지가 왔다고 브로드캐스팅 (채팅방 목록 실시간 갱신용)
    private void broadcastToChatUser(ChatroomUserDTO chatroomUserDTO) {
        try {
            List<Long> receiverIds = chatroomUserRepository.getReceiverId(
                    chatroomUserDTO.getChatroomId(),
                    chatroomUserDTO.getUserId()
            );
            for (Long receiverId : receiverIds) {
                Map<String, Object> data = new HashMap<>();
                data.put("chatroomId", chatroomUserDTO.getChatroomId());
                data.put("unreadCount", messageRepository.countUnreadMessages(
                        chatroomUserDTO.getChatroomId(), receiverId));
                chatroomUserRepository.getChatroomUser(chatroomUserDTO.getChatroomId(), receiverId)
                        .ifPresent(e -> data.put("chatroomUserId", e.getChatroomUserId()));

                messagingTemplate.convertAndSend("/sub/chatroom-list/" + receiverId, data);
            }
        } catch (MessagingException e) {
            log.error("Failed to send message to chatroom {}: {}",
                    chatroomUserDTO.getChatroomId(), e.getMessage(), e);
            throw new IllegalStateException("유저 메시지 전송 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("Unexpected error during message broadcast: {}", e.getMessage(), e);
            throw new RuntimeException("예상치 못한 오류가 발생했습니다.", e);
        }
    }

    private void broadcastToChatroom(MessageDTO messageDTO) {
        try {
            messagingTemplate.convertAndSend("/sub/messages/" + messageDTO.getChatroomId(), messageDTO);
        } catch (MessagingException e) {
            log.error("Failed to send message to chatroom {}: {}",
                    messageDTO.getChatroomId(), e.getMessage(), e);
            throw new IllegalStateException("메시지 전송 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("Unexpected error during message broadcast: {}", e.getMessage(), e);
            throw new RuntimeException("예상치 못한 오류가 발생했습니다.", e);
        }
    }

    // 상대방의 채팅방 접속 상태 확인 후 변경
    public void checkAndUpdateReceiverConnectionStatus(MessageUserDTO messageUserDTO, LocalDateTime now) {
        ChatroomUserDTO chatroomUserDTO = messageUserDTO.getChatroomUserDTO();
        long userId = messageUserDTO.getMessageDTO().getUserId();
        long chatroomId = chatroomUserDTO.getChatroomId();

        boolean ifDeleted = chatroomUserRepository.checkIfChatroomDeleted(chatroomId, userId) > 0;
        if (ifDeleted) {
            UserEntity sender = chatUserRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
            chatroomUserRepository.updateReceiverConnectionStatus(
                    chatroomId,
                    userId,
                    sender.getName() + "님과의 채팅방",
                    now
            );
        }
    }
}
