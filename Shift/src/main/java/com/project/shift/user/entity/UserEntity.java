package com.project.shift.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "USERS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_USERS_LOGIN_ID", columnNames = {"LOGIN_ID"}),
                @UniqueConstraint(name = "UK_USERS_PHONE", columnNames = {"PHONE"})
        })
@SQLRestriction("DELETED_AT IS NULL") //DELETED_AT이 NULL인 값만 조회하도록 설정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @SequenceGenerator(
            name = "SEQ_USERS_GENERATOR",
            sequenceName = "seq_users",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_USERS_GENERATOR")
    @Column(name = "USER_ID", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "LOGIN_ID", nullable = false, length = 30)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(nullable = false)
    private Integer points;

    @Column(
            name = "ADMIN_FLAG",
            nullable = false,
            columnDefinition = "CHAR(1)"
    )
    private String adminFlag = "N";

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Builder
    public UserEntity(String loginId, String password, String name, String phone, String address) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.points = 0;
        this.adminFlag = "N";
    }

    //수정 가능 필드만 메서드로 제공
    public void updateInfo(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    // 회원 탈퇴
    public void withDraw() {
        this.loginId = "deleted_" + this.userId; // 탈퇴한 회원의 로그인 아이디를 변경하여 중복 방지
        this.password = "DELETED_USER_PASSWORD"; // 탈퇴한 회원의 비밀번호를 변경하여 보안 강화
        this.name = "탈퇴한 회원";
        this.phone = null;
        this.address = null;
        this.points = 0;
        this.adminFlag = "N";
        this.deletedAt = LocalDateTime.now(); // 탈퇴 시점 기록
    }

    public void updatePoints(int points) {
        this.points = points;
    }
}