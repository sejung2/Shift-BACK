package com.project.shift.chat.controller;

import com.project.shift.chat.dto.ChatUserSearchResponse;
import com.project.shift.chat.dto.ChatroomUserDTO;
import com.project.shift.chat.dto.response.ChatUserMyPageInfoResponse;
import com.project.shift.chat.service.ChatUserService;
import com.project.shift.chat.service.ChatroomUserService;
import com.project.shift.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/chat/users")
public class ChatUserController {

    private final ChatUserService chatUserService;
    private final ChatroomUserService chatroomUserService;

    // 특정 채팅방 유저 정보 반환
    @GetMapping("/{chatroomId}")
    public ResponseEntity<ChatroomUserDTO> getChatroomUser(@PathVariable long chatroomId) {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatroomUserService.getChatroomUser(chatroomId, userId));
    }

    // 전화번호로 사용자 검색 및 친구여부 반환
    @GetMapping("/search/{phone}")
    public ResponseEntity<ChatUserSearchResponse> searchUser(@PathVariable String phone) {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatUserService.searchUserByPhone(userId, phone));
    }

    // 채팅-마이페이지 개인 정보(ID, 이름, 핸드폰 번호) 반환, 프로필 이미지는 추후 추가 예정
    @GetMapping("/me")
    public ResponseEntity<ChatUserMyPageInfoResponse> getChatUserInfo() {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(chatUserService.getChatUserInfo(userId));
    }

    // 프로필 이미지 업로드
    @PostMapping("/uploadProfileImage")
    public ResponseEntity<Void> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        long userId = CurrentUser.getUserId();

        chatUserService.uploadProfileImage(userId, file);
        return ResponseEntity.ok().build();
    }
}