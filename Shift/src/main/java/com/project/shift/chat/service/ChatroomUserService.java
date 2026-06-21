package com.project.shift.chat.service;

import com.project.shift.chat.dto.ChatroomUserDTO;
import com.project.shift.chat.dto.MessageWithSenderDTO;
import com.project.shift.chat.dto.projection.ChatroomListProjection;
import com.project.shift.chat.dto.request.DeletedChatroomUserInfoRequest;
import com.project.shift.chat.dto.response.ChatroomListResponse;
import com.project.shift.chat.entity.ChatroomUserEntity;
import com.project.shift.chat.repository.ChatroomRepository;
import com.project.shift.chat.repository.ChatroomUserRepository;
import com.project.shift.chat.repository.MessageRepository;
import com.project.shift.global.exception.NotFoundException;
import com.project.shift.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatroomUserService {

    private final ChatroomUserRepository chatroomUserRepository;
    private final ChatroomRepository chatroomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    // 특정 채팅방에 참여
    @Transactional
    public void addChatroomUsers(MessageWithSenderDTO dto, long chatroomId) {
        // 채팅 생성자 생성 후 저장
        ChatroomUserDTO sender = dto.getSender();
        sender.setChatroomId(chatroomId);
        sender.setConnectionStatus("ON");

        ChatroomUserEntity senderEntity = ChatroomUserEntity.builder()
                .chatroom(chatroomRepository.getReferenceById(chatroomId))
                .user(userRepository.getReferenceById(sender.getUserId()))
                .chatroomName(sender.getChatroomName())
                .lastConnectionTime(sender.getLastConnectionTime())
                .createdTime(sender.getCreatedTime())
                .connectionStatus(sender.getConnectionStatus())
                .isDarkMode(sender.getIsDarkMode())
                .build();
        chatroomUserRepository.save(senderEntity);

        ChatroomUserEntity receiverEntity = ChatroomUserEntity.builder()
                .chatroom(chatroomRepository.getReferenceById(chatroomId))
                .user(userRepository.getReferenceById(dto.getReceiverId()))
                .chatroomName(dto.getSenderName() + "님과의 채팅방")
                .createdTime(dto.getMessage().getSendDate())
                .lastConnectionTime(dto.getMessage().getSendDate().minusSeconds(1))
                .connectionStatus("OF")
                .isDarkMode("N")
                .build();
        chatroomUserRepository.save(receiverEntity);
    }

    // 특정 채팅방에서 특정 사용자만 나가기 (사용자 key 보존, 상대방 데이터 보존)
    @Transactional
    public void deleteChatroomUser(long chatroomUserId) {
        if (!chatroomUserRepository.existsById(chatroomUserId)) {
            throw new NotFoundException("채팅방을 찾을 수 없습니다.");
        }
        chatroomUserRepository.initChatroomUserExceptKey(chatroomUserId);
    }

    // 특정 채팅방 유저 정보 반환
    @Transactional(readOnly = true)
    public ChatroomUserDTO getChatroomUser(long chatroomId, long userId) {
        return chatroomUserRepository.getChatroomUser(chatroomId, userId)
                .map(entity -> ChatroomUserDTO.builder()
                        .chatroomUserId(entity.getChatroomUserId())
                        .chatroomId(entity.getChatroom().getChatroomId())
                        .userId(entity.getUser().getUserId())
                        .chatroomName(entity.getChatroomName())
                        .lastConnectionTime(entity.getLastConnectionTime())
                        .createdTime(entity.getCreatedTime())
                        .connectionStatus(entity.getConnectionStatus())
                        .isDarkMode(entity.getIsDarkMode())
                        .build())
                .orElseThrow(() -> new NotFoundException("채팅방 유저 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public ChatroomListResponse getChatroomListView(long chatroomUserId, long userId) {
        ChatroomListProjection p = chatroomUserRepository.findChatroomByChatroomUserId(chatroomUserId)
                .orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));

        ChatroomListResponse dto = ChatroomListResponse.builder()
                .chatroomUserId(p.getChatroomUserId())
                .chatroomId(p.getChatroomId())
                .chatroomName(p.getChatroomName())
                .lastMsgContent(p.getLastMsgContent())
                .lastMsgDate(p.getLastMsgDate())
                .lastConnectionTime(p.getLastConnectionTime())
                .createdTime(p.getCreatedTime())
                .connectionStatus(p.getConnectionStatus())
                .isDarkMode(p.getIsDarkMode())
                .receiverId(p.getReceiverId())
                .receiverName(p.getReceiverName())
                .build();

        dto.setUnreadCount(p.getUnreadCount());
        return dto;
    }

    @Transactional(readOnly = true)
    public Optional<ChatroomUserDTO> getChatroomWithReceiver(long userId, long receiverId) {
        List<Long> ids = List.of(userId, receiverId);
        return chatroomUserRepository.findChatroomWithUsers(ids, ids.size())
                .map(chatroomId -> getChatroomUser(chatroomId, userId));
    }

    // 채팅방 생성 시 두 사용자간 삭제된 채팅방 복구
    @Transactional
    public void restoreChatroomBetweenUsers(DeletedChatroomUserInfoRequest dto) {
        LocalDateTime now = LocalDateTime.now();
        String senderChatroomName = dto.receiverName() + "님과의 채팅방";
        chatroomUserRepository.restoreChatroomUser(dto.chatroomId(), dto.senderId(), "ON", now, senderChatroomName);
    }

    @Transactional
    public void updateChatroomName(ChatroomUserDTO dto) {
        int updated = chatroomUserRepository.updateChatroomName(dto.getChatroomUserId(), dto.getChatroomName());
        if (updated == 0) {
            throw new NotFoundException("채팅방을 찾을 수 없습니다.");
        }
    }
}
