package com.project.shift.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendInfoDTO {

    private long friendshipId; // 친구 관계 테이블 PK
    private long friendId;     // PK
    private String name; // 이름
    private String loginId; // ID(로그인에 사용)
    private String phone; // 전화번호
    
}
