package com.project.shift.chat.repository;

import com.project.shift.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhone(String phone);

    // userId로 친구 검색
    @Query("""
            SELECT u FROM UserEntity u
                     WHERE u.userId IN :userIds
                     ORDER BY u.name ASC
            """)
    List<UserEntity> findUserInfoByIds(@Param("friendIds") List<Long> userIds);
}
