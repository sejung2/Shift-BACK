package com.project.shift.chat.service;

import com.project.shift.chat.dto.request.FriendRequest;
import com.project.shift.chat.dto.response.FriendInfoResponse;
import com.project.shift.chat.entity.FriendEntity;
import com.project.shift.chat.repository.FriendRepository;
import com.project.shift.global.exception.NotFoundException;
import com.project.shift.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FriendInfoResponse> getUserFriends(long userId) {
        return friendRepository.getUserFriends(userId);
    }

    @Transactional
    public void addFriendship(FriendRequest dto) {
        friendRepository.save(FriendEntity.builder()
                .user(userRepository.getReferenceById(dto.getUserId()))
                .friend(userRepository.getReferenceById(dto.getFriendId()))
                .build());
    }

    @Transactional
    public void deleteFriend(long userId, long friendId) {
        if (!friendRepository.existsByUser_UserIdAndFriend_UserId(userId, friendId)) {
            throw new NotFoundException("친구 관계를 찾을 수 없습니다.");
        }
        friendRepository.deleteByUser_UserIdAndFriend_UserId(userId, friendId);
    }

}
