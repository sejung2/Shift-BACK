package com.project.shift.chat.repository;

import com.project.shift.chat.entity.MessageEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    // 특정 사용자가 읽지 않은 특정 채팅방의 메시지 개수 반환
    @Query(value = """
            select count(*)
            from messages m, chatroom_users cu
            where cu.user_id = :userId and
            	  cu.chatroom_id = :chatroomId and
            	  m.chatroom_id = cu.chatroom_id and
            	  m.send_date >= GREATEST(cu.last_connection_time, cu.created_time) and
            	  m.user_id <> :userId
            """, nativeQuery = true)
    int countUnreadMessages(@Param("chatroomId") long chatroomId, @Param("userId") long userId);

    // 채팅방 최초 생성 시간 이후의 메시지 반환
    @Query("""
            SELECT m FROM MessageEntity m
            WHERE m.chatroom.chatroomId = :chatroomId AND
            	  m.sendDate >= :createdDateTime
            """)
    List<MessageEntity> findByChatroomId(@Param("chatroomId") long chatroomId,
                                         @Param("createdDateTime") LocalDateTime createdDateTime);

    // 마지막 접속 시간 이후의 메시지 읽음 처리
    @Modifying
    @Transactional
    @Query("""
            UPDATE MessageEntity m
            SET m.unreadCount = m.unreadCount-1
            WHERE m.chatroom.chatroomId = :chatroomId AND
            	  m.unreadCount > 0 AND
            	  m.sendDate >= :lastConnectionTime AND
            	  m.user.userId <> :userId
            """)
    void markMessagesAsRead(@Param("chatroomId") long chatroomId, @Param("lastConnectionTime") LocalDateTime lastConnectionTime, @Param("userId") long userId);

}
