package com.project.shift.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Entity
@Table(name = "USERS")
@SequenceGenerator(
        name = "SEQ_USERS_GENERATOR",
        sequenceName = "seq_users",
        allocationSize = 1
)
@SQLRestriction("DELETED_AT IS NULL") //DELETED_AT이 NULL인 값만 조회하도록 설정
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_USERS_GENERATOR")
    @Column(name = "USER_ID", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "LOGIN_ID", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String phone;

    @Column
    private String address;

    @Column
    private Integer points = 0;

    @Column(name = "REFRESH_TOKEN")
    private String refreshToken;

    @Column(unique = true)
    private String email;

    @Column
    private String provider; // 가입 경로 (예: "local", "google", "kakao")

    @Column(
            name = "ADMIN_FLAG",
            nullable = false,
            columnDefinition = "CHAR(1)"
    )
    private String adminFlag = "N";

    @Column(name = "DELETED_AT")
    private Timestamp deletedAt;

    //수정 가능 필드만 메서드로 제공
    public void updateInfo(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }
}