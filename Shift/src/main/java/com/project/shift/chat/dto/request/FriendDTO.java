package com.project.shift.chat.dto.request;

import com.project.shift.chat.entity.FriendEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDTO {

    private long friendshipId; // PK
    private long userId;       // 사용자 ID
    private long friendId;     // 친구 ID

    // Entity -> DTO 변환
    public static FriendDTO toDto(FriendEntity entity) {
        return FriendDTO.builder()
                .friendshipId(entity.getFriendshipId())
                .userId(entity.getUserId())
                .friendId(entity.getFriendId())
                .build();
    }
}
