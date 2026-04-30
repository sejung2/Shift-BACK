package com.project.shift.chat.dto;

import com.project.shift.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUserDTO {

    private long userId;
    private String loginId;
    private String password;
    private String name;
    private String phone;
    private String address;
    private int points;
    private String refreshToken;
    private String adminFlag; // DEFAULT 'N', 'Y' 또는 'N'

}
