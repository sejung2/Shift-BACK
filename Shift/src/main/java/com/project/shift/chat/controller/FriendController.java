package com.project.shift.chat.controller;

import com.project.shift.chat.dto.request.FriendRequest;
import com.project.shift.chat.dto.response.FriendInfoResponse;
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
    public ResponseEntity<List<FriendInfoResponse>> getFriendList() {
        long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(friendService.getUserFriends(userId));
    }

    // 친구 추가
    @PostMapping
    public ResponseEntity<Void> addFriendship(@RequestBody FriendRequest friendInfo) {
        friendService.addFriendship(friendInfo);
        return ResponseEntity.ok().build();
    }

    // 친구 삭제
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriend(@PathVariable long friendId) {
        long userId = CurrentUser.getUserId();
        // 친구 삭제
        friendService.deleteFriend(userId, friendId);
        return ResponseEntity.ok().build();
    }

}