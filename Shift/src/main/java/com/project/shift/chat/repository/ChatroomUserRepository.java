package com.project.shift.chat.repository;

import com.project.shift.chat.dto.ChatroomListProjection;
import com.project.shift.chat.entity.ChatroomUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatroomUserRepository extends JpaRepository<ChatroomUserEntity, Long> {

    // 사용자 채팅방 접속 정보 수정
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatroomUserEntity c 
            SET c.connectionStatus = :status, c.lastConnectionTime = :time 
            WHERE c.chatroomUserId = :id
            """)
    void updateChatUserInfo(@Param("status") String status,
                            @Param("time") LocalDateTime time,
                            @Param("id") long id);

    // 특정 채팅방 유저 정보 반환
    @Query("""
            SELECT c FROM ChatroomUserEntity c
            WHERE c.chatroom.chatroomId = :id AND c.user.userId = :userId
            """)
    Optional<ChatroomUserEntity> getChatroomUser(@Param("id") long id,
                                                 @Param("userId") long userId);

    // 특정 채팅방의 특정 유저의 채팅방 삭제시 pk, fk 빼고 전부 초기화
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatroomUserEntity c 
            SET c.connectionStatus = 'DL', 
            c.isDarkMode = 'N' 
            WHERE c.chatroomUserId = :id
            """)
    void initChatroomUserExceptKey(@Param("id") long id);

    // 특정 채팅방의 모든 유저의 채팅방 삭제시 pk, fk 빼고 전부 초기화
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatroomUserEntity c 
            SET c.connectionStatus = 'DL', 
            c.isDarkMode = 'N' 
            WHERE c.chatroom.chatroomId = :chatroomId
            """)
    void initAllChatroomUsersExceptKey(@Param("chatroomId") long chatroomId);

    // 두 사용자가 속한 채팅방 ID 반환
    @Query("""
            SELECT c.chatroom.chatroomId
               FROM ChatroomUserEntity c
               WHERE c.user.userId IN :ids
               GROUP BY c.chatroom.chatroomId
               HAVING COUNT(DISTINCT c.user.userId) = :countUsers 
            """)
    Optional<Long> findChatroomWithUsers(@Param("ids") List<Long> ids, @Param("countUsers") long countUsers);

    // 채팅방 생성 시 두 사용자간 삭제된 채팅방 복구
    @Modifying
    @Transactional
    @Query("""
               UPDATE ChatroomUserEntity c 
               SET c.createdTime = :now,
                c.lastConnectionTime = :now,
                c.connectionStatus = :connectionStatus,
                c.chatroomName = :chatroomName
            WHERE c.chatroom.chatroomId = :chatroomId
            	AND c.user.userId = :userId
            	AND c.connectionStatus = 'DL'
            """)
    void restoreChatroomUser(@Param("chatroomId") long chatroomId,
                             @Param("userId") long userId,
                             @Param("connectionStatus") String connectionStatus,
                             @Param("now") LocalDateTime now,
                             @Param("chatroomName") String chatroomName);

    // 상대방의 채팅방 삭제여부 반환
    @Query("""
                SELECT COUNT(c)
                FROM ChatroomUserEntity c
                WHERE c.connectionStatus = 'DL'
                  AND c.chatroom.chatroomId = :id
                  AND c.user.userId != :userId
            """)
    int checkIfChatroomDeleted(@Param("id") long id, @Param("userId") long userId);

    // 상대방의 채팅방 접속 상태를 'DL'에서 'OF'로 변경 및 채팅방 생성 시간과 마지막 접속시간 변경
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatroomUserEntity c 
            SET c.chatroomName = :newChatroomName,
            	c.connectionStatus = 'OF',
            	c.createdTime = :now,
            	c.isDarkMode = 'N'
            WHERE c.chatroom.chatroomId = :chatroomId
            	AND c.user.userId != :userId
            """)
    void updateReceiverConnectionStatus(@Param("chatroomId") long chatroomId,
                                        @Param("userId") long userId,
                                        @Param("newChatroomName") String newChatroomName,
                                        @Param("now") LocalDateTime now);

    // 채팅방 이름 변경
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatroomUserEntity c
            SET c.chatroomName = :newChatroomName
            WHERE c.chatroomUserId = :id
            """)
    int updateChatroomName(@Param("id") long id,
                           @Param("newChatroomName") String newChatroomName);

    // chatroomUserId와 userId로 해당 채팅방의 모든 ReceiverId를 반환하는 함수
    // 메시지가 전송됐다는 알림을 모든 수신자들에게 보내기 위한 함수 (실시간 채팅방 목록 관련)
    @Query("""
            SELECT r.user.userId 
            FROM ChatroomUserEntity u, ChatroomUserEntity r 
            WHERE u.chatroom.chatroomId = r.chatroom.chatroomId 
            AND u.user.userId = :userId
            AND u.chatroom.chatroomId = :chatroomId 
            AND r.user.userId <> :userId
            """)
    List<Long> getReceiverId(@Param("chatroomId") long chatroomUserId, @Param("userId") long userId);

    // 현재 채팅방에서 나를 제외하고 접속 중인 사용자의 수를 반환
    @Query("""
               SELECT COUNT(r)
               FROM ChatroomUserEntity u, ChatroomUserEntity r
               WHERE u.chatroom.chatroomId = r.chatroom.chatroomId
                 AND u.user.userId = :userId
                 AND u.chatroom.chatroomId = :chatroomId
                 AND r.user.userId <> :userId
                 AND r.connectionStatus = 'ON'
            """)
    int countOtherUsersOnline(@Param("chatroomId") long chatroomId,
                              @Param("userId") long userId);

    // 채팅방 목록 조회
    @Query(value = """
            select
            	cu.chatroom_users_id as chatroomUserId,
            	cu.chatroom_id as chatroomId,
            	cu.chatroom_name as chatroomName,
            	cu.last_connection_time as lastConnectionTime,
            	cu.connection_status as connectionStatus,
            	cu.created_time as createdTime,
            	cu.is_dark_mode as isDarkMode,
            	c.last_msg_content as lastMsgContent,
            	c.last_msg_date as lastMsgDate,
            	cu2.user_id as receiverId,
            	u.name as receiverName
            from chatroom_users cu 
            join chatrooms c ON c.chatroom_id = cu.chatroom_id
            join chatroom_users cu2 on cu2.chatroom_id = cu.chatroom_id
             						and cu2.chatroom_users_id != cu.chatroom_users_id
            join users u on u.user_id = cu2.user_id
            where cu.chatroom_users_id = :id 
            	and cu.connection_status != 'DL'
            """, nativeQuery = true)
    Optional<ChatroomListProjection> findChatroomByChatroomUserId(@Param("id") long id);

    // -- 탈퇴 시 처리 --
    // 탈퇴 시 특정 사용자의 모든 채팅방 접속 상태를 DL로 변경
    @Modifying
    @Transactional
    @Query(value = "UPDATE ChatroomUserEntity cu SET cu.connectionStatus = 'DL', cu.chatroomName = null WHERE cu.user.userId = :userId")
    void updateStatusToDeletedByUserId(@Param("userId") long userId);
}
