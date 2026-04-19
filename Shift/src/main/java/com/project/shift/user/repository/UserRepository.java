package com.project.shift.user.repository;

import com.project.shift.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// UserDAO 인터페이스를 구현하고, 내부적으로 JPA를 사용합니다.
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByLoginId(String loginId);

    boolean existsByPhone(String phone);

    Optional<UserEntity> findByLoginId(String loginId); // 로그인 아이디로 특정 사용자 추출

    // 사용자 이름과 연락처로 특정 사용자 추출
    Optional<UserEntity> findByNameAndPhone(String name, String phone);
}