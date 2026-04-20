package com.project.shift.auth.repository;

import com.project.shift.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    // userId로 바로 조회 - users 테이블 추가 조회 없음
    Optional<RefreshTokenEntity> findByUser_UserId(Long userId);

    // userId로 바로 삭제
    void deleteByUser_UserId(Long userId);
}
