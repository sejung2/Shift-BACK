package com.project.shift.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequest {

    private long friendshipId; // PK
    private long userId;       // 사용자 ID
    private long friendId;     // 친구 ID
}
