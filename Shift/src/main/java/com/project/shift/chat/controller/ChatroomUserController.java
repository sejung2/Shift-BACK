package com.project.shift.chat.controller;

import com.project.shift.chat.dto.ChatroomUserDTO;
import com.project.shift.chat.dto.request.DeletedChatroomUserInfoRequest;
import com.project.shift.chat.dto.response.ChatroomListResponse;
import com.project.shift.chat.service.ChatroomUserService;
import com.project.shift.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chatroom/users")
public class ChatroomUserController {

    private final ChatroomUserService chatroomUserService;

    // 특정 두 유저가 참여한 채팅방 정보 확인 및 반환
    @GetMapping("/receiver/{receiverId}")
    public ResponseEntity<?> getChatroomWithReceiver(@PathVariable long receiverId) {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatroomUserService.getChatroomWithReceiver(userId, receiverId));
    }


    // CHATROOM-08 : 특정 채팅방 정보 반환
    @GetMapping("/{chatroomUserId}")
    public ResponseEntity<ChatroomListResponse> getChatroomListView(@PathVariable long chatroomUserId) {
		long userId = CurrentUser.getUserId();
		return ResponseEntity.ok(chatroomUserService.getChatroomListView(chatroomUserId, userId));
    }

    // 채팅방 생성 시 두 사용자간 삭제된 채팅방 복구
    @PostMapping("/restore")
    public ResponseEntity<Void> restoreChatroomBetweenUsers(@RequestBody @Valid DeletedChatroomUserInfoRequest dto) {
        chatroomUserService.restoreChatroomBetweenUsers(dto);
		return ResponseEntity.ok().build();
    }

    // 채팅방 이름 변경
    @PatchMapping("/chatroom-name")
    public ResponseEntity<?> updateChatroomName(@RequestBody ChatroomUserDTO dto) {
		chatroomUserService.updateChatroomName(dto);
		return ResponseEntity.ok().build();
    }

}
