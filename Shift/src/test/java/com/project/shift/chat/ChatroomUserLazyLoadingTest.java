package com.project.shift.chat;

import com.project.shift.chat.entity.ChatroomEntity;
import com.project.shift.chat.entity.ChatroomUserEntity;
import com.project.shift.chat.repository.ChatroomUserRepository;
import com.project.shift.user.entity.UserEntity;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ChatroomUserLazyLoadingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChatroomUserRepository chatroomUserRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        Session session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("연관관계 매핑 전 — userId 숫자값만 알 수 있고 UserEntity 정보는 접근 불가")
    void beforeMapping_onlyUserIdIsAccessible() {
        // given
        UserEntity user = saveUser("before_user", "매핑전유저", "01011110000");
        ChatroomEntity chatroom = saveChatroom();
        saveChatroomUser(user, chatroom);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        // when
        NoRelationChatroomUserEntity loaded = entityManager.createQuery(
                        "SELECT c FROM NoRelationChatroomUserEntity c WHERE c.userId = :userId",
                        NoRelationChatroomUserEntity.class)
                .setParameter("userId", user.getUserId())
                .getSingleResult();

        // then
        // SQL 1번만 나감 — UserEntity 조회 없음
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
        // userId 숫자값은 알 수 있음
        assertThat(loaded.getUserId()).isEqualTo(user.getUserId());
        // UserEntity 정보(이름 등)는 접근 불가 — getUser() 메서드 자체가 없음
    }

    @Test
    @DisplayName("연관관계 매핑 후 — findById 직후 UserEntity는 프록시, 접근 시점에 SELECT 발생")
    void afterMapping_userEntityLoadsOnAccess() {
        // given
        UserEntity user = saveUser("after_user", "매핑후유저", "01022220000");
        ChatroomEntity chatroom = saveChatroom();
        saveChatroomUser(user, chatroom);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        // when
        ChatroomUserEntity loaded = chatroomUserRepository
                .getChatroomUser(chatroom.getChatroomId(), user.getUserId())
                .orElseThrow();

        // then - 조회 직후
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L); // SQL 1번
        assertThat(Hibernate.isInitialized(loaded.getUser())).isFalse(); // 프록시 상태

        // user.getName() 접근하는 순간 SELECT 발생
        String userName = loaded.getUser().getName();

        // then - 접근 후
        assertThat(statistics.getPrepareStatementCount()).isGreaterThan(1L); // SQL 추가 발생
        assertThat(Hibernate.isInitialized(loaded.getUser())).isTrue(); // 초기화 완료
        assertThat(userName).isEqualTo("매핑후유저"); // 실제 이름 정상 조회
    }

    // ── 헬퍼 메서드 ──────────────────────────────────

    private UserEntity saveUser(String loginId, String name, String phone) {
        UserEntity user = UserEntity.builder()
                .loginId(loginId)
                .password("encoded-password")
                .name(name)
                .phone(phone)
                .address("seoul")
                .build();
        entityManager.persist(user);
        return user;
    }

    private ChatroomEntity saveChatroom() {
        ChatroomEntity chatroom = ChatroomEntity.builder()
                .lastMsgContent("테스트 메시지")
                .lastMsgDate(LocalDateTime.now())
                .build();
        entityManager.persist(chatroom);
        return chatroom;
    }

    private void saveChatroomUser(UserEntity user, ChatroomEntity chatroom) {
        ChatroomUserEntity chatroomUser = ChatroomUserEntity.builder()
                .chatroom(chatroom)
                .user(user)
                .chatroomName("테스트 채팅방")
                .createdTime(LocalDateTime.now())
                .lastConnectionTime(LocalDateTime.now())
                .connectionStatus("ON")
                .isDarkMode("N")
                .build();
        entityManager.persist(chatroomUser);
    }

    // ── 테스트 전용 Entity — 연관관계 없는 케이스 ──────

    @Entity(name = "NoRelationChatroomUserEntity")
    @Table(name = "CHATROOM_USERS")
    static class NoRelationChatroomUserEntity {

        @Id
        @Column(name = "CHATROOM_USERS_ID")
        private Long chatroomUserId;

        @Column(name = "USER_ID", insertable = false, updatable = false)
        private Long userId;

        public Long getUserId() {
            return userId;
        }
    }
}
