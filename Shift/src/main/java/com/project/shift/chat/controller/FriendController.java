package com.project.shift.chat.controller;

import com.project.shift.chat.dto.request.FriendDTO;
import com.project.shift.chat.dto.response.FriendInfoDTO;
import com.project.shift.chat.service.FriendService;
import com.project.shift.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/friends")
public class FriendController {

    private final FriendService friendService;

    // 친구 목록 조회
    @GetMapping
    public ResponseEntity<List<FriendInfoDTO>> getFriendList() {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(friendService.getUserFriends(userId));
    }

    // 친구 추가
    @PostMapping
    public void addFriendship(@RequestBody FriendDTO friendInfo) {
        friendService.addFriendship(friendInfo);
        return;
    }

    // 친구 삭제
    @DeleteMapping("/{friendId}")
    public void deleteFriend(@PathVariable long friendId) {
        long userId = CurrentUser.getUserId();
        // 친구 삭제
        friendService.deleteFriend(userId, friendId);
    }

}