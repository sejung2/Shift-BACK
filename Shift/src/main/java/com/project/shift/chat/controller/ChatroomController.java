package com.project.shift.chat.controller;

import com.project.shift.chat.dto.MessageWithSenderDTO;
import com.project.shift.chat.dto.response.ChatroomResponse;
import com.project.shift.chat.dto.response.ChatroomListResponse;
import com.project.shift.chat.dto.response.MessageSearchResultResponse;
import com.project.shift.chat.service.ChatroomService;
import com.project.shift.chat.service.ChatroomUserService;
import com.project.shift.chat.service.MessageService;
import com.project.shift.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chatrooms")
public class ChatroomController {

    private final ChatroomService chatroomService;
    private final ChatroomUserService chatroomUserService;
    private final MessageService messageService;

    // 사용자가 참여한 채팅방 목록 반환
    @GetMapping
    public ResponseEntity<List<ChatroomListResponse>> getUserChatroomList() {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatroomService.getUserChatrooms(userId));
    }

    // 특정 채팅방 반환
    @GetMapping("/{chatroomId}")
    public ResponseEntity<ChatroomResponse> getChatroom(@PathVariable long chatroomId) {
        return ResponseEntity.ok(chatroomService.getChatroom(chatroomId));
    }

    // 새로운 채팅방 추가 및 메시지 DB저장 & 브로드캐스팅
    @PostMapping
    public ResponseEntity<Long> addChatroom(@RequestBody MessageWithSenderDTO payload) {
        // 채팅방 생성 (Chatrooms) 및 생성된 채팅방 pk 반환
        // 내부 로직에 객체간 동일한 시간 설정 포함됨
        long newChatroomId = chatroomService.addChatroom(payload);
        // 새로 생성된 채팅방 pk MessageDTO에 세팅
        payload.getMessage().setChatroomId(newChatroomId);

        // 두 사용자의 새로운 채팅방 정보 추가 (ChatroomUsers)
        chatroomUserService.addChatroomUsers(payload, newChatroomId);

        // 메시지 DB저장 & 브로드캐스팅
        messageService.sendAndSaveMessage(payload.getMessage(), payload.getSender());
        return ResponseEntity.ok(newChatroomId);
    }

    // 특정 채팅방에 참여한 모든 사용자의 채팅방 삭제
    // → 실제 데이터 삭제가 아닌 pk, fk 빼고 초기화
    @DeleteMapping("/{chatroomId}") // 브로드캐스팅 추가 (채팅방 나감)
    public ResponseEntity<Void> deleteChatroom(@PathVariable long chatroomId) {
        chatroomService.deleteChatroomAndChatroomUsers(chatroomId);
        return ResponseEntity.ok().build();
    }

    // 특정 채팅방에 참여한 일부 사용자 채팅방 삭제
    // → 실제 데이터 삭제가 아닌 pk, fk 빼고 초기화
    @DeleteMapping("/users/{chatroomUserId}") // 브로드캐스팅 추가 (채팅방 나감)
    public ResponseEntity<Void> deleteUsersChatroom(@PathVariable long chatroomUserId) {
        chatroomUserService.deleteChatroomUser(chatroomUserId);
        return ResponseEntity.ok().build();
    }

    /*
     * ## 채팅 검색 ##
     * 검색 방법에는 두 가지가 있음
     * 1. 검색 키워드가 참여한 채팅 목록의 상대방 이름에 포함될 때
     * 2. 검색 키워드가 참여한 채팅방의 메시지 내용에 포함될 때
     *
     * 각각의 경우가 반환 타입이 다름
     * 1. 기존의 채팅방 목록 형태와 일치
     * 2. 기존의 채팅방 목록에 메시지 내용이 추가되고 채팅방의 최신 메시지와 최신 메시지 전송 시간이 빠짐
     *
     * 사용자가 검색을 하면 두 API를 호출하면 되고 검색 결과 UI를 위, 아래로 나누어 보여주는 것을 고려하여 설계
     */
    // 채팅방 검색 - 1. 검색 키워드가 참여한 채팅 목록의 상대방 이름에 포함될 때
    @GetMapping("/search/name")
    public ResponseEntity<List<ChatroomListResponse>> searchChatroomUsersName(@RequestParam String input) {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatroomService.searchChatroomUsersName(input, userId));
    }

    // 채팅방 검색 - 2. 검색 키워드가 참여한 채팅방의 메시지 내용에 포함될 때
    @GetMapping("/search/messages")
    public ResponseEntity<List<MessageSearchResultResponse>> searchChatroomMessages(@RequestParam String input) {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatroomService.searchChatroomMessages(input, userId));
    }

}
