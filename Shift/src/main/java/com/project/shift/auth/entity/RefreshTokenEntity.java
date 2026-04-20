package com.project.shift.auth.entity;

import com.project.shift.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "REFRESH_TOKENS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenEntity {

    @Id
    @SequenceGenerator(
            name = "SEQ_TOKEN_GENERATOR",
            sequenceName = "seq_refresh_token",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TOKEN_GENERATOR")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "TOKEN_VALUE", nullable = false)
    private String tokenValue;

    @Column(name = "EXPIRED_AT", nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    RefreshTokenEntity(UserEntity userEntity, String tokenValue, LocalDateTime expiredAt) {
        this.user = userEntity;
        this.tokenValue = tokenValue;
        this.expiredAt = expiredAt;
    }

    // 토큰 갱신
    public void refreshToken(String newToken, LocalDateTime newExpiredAt) {
        this.tokenValue = newToken;
        this.expiredAt = newExpiredAt;
    }
}
