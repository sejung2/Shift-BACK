package com.project.shift.chat.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.project.shift.chat.dto.ChatroomListProjection;
import com.project.shift.chat.dto.MessageSearchResultProjection;
import com.project.shift.chat.entity.ChatroomEntity;

public interface ChatroomRepository extends JpaRepository<ChatroomEntity, Long>{

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
			    lm.user_id as lastMsgSender,
			    cu2.user_id as receiverId,
			    u.name as receiverName
			from chatroom_users cu
			join chatrooms c
			  on c.chatroom_id = cu.chatroom_id
			left join (
			    select chatroom_id, user_id
			    from (
			        select m.chatroom_id,
			               m.user_id,
			               row_number() over (
			                   partition by m.chatroom_id
			                   order by m.send_date desc, m.message_id desc
			               ) rn
			        from messages m
			    )
			    where rn = 1
			) lm
			  on lm.chatroom_id = c.chatroom_id
			join chatroom_users cu2
			  on cu2.chatroom_id = cu.chatroom_id
			 and cu2.user_id != :userId
			join users u
			  on u.user_id = cu2.user_id
			where cu.user_id = :userId
			  and cu.connection_status != 'DL'
			""", nativeQuery = true)
	List<ChatroomListProjection> findChatroomsByUserId(@Param("userId") long userId);
	
	// 채팅방 검색 - 검색 키워드가 채팅 참여자 이름이라면 해당 참여자와의 채팅방 반환
	@Query(value = """
			select
		        cu_me.chatroom_users_id as chatroomUserId,
		        cu_me.chatroom_id as chatroomId,
		        cu_me.chatroom_name as chatroomName,
		        cu_me.last_connection_time as lastConnectionTime,
		        cu_me.connection_status as connectionStatus,
		        cu_me.created_time as createdTime,
		        cu_me.is_dark_mode as isDarkMode,
		        lm.user_id as lastMsgSender,
		        cu_other.user_id as receiverId,
		        u.name as receiverName,
		        c.last_msg_content as lastMsgContent,
		        c.last_msg_date as lastMsgDate
		    from chatroom_users cu_me
		    join chatroom_users cu_other
		        on cu_me.chatroom_id = cu_other.chatroom_id
		       and cu_other.user_id != :userId
		    join users u
		        on u.user_id = cu_other.user_id
		    join chatrooms c
		        on c.chatroom_id = cu_me.chatroom_id
		    left join (
		        select m.chatroom_id, m.user_id
		        from messages m
		        where (m.chatroom_id, m.send_date) in (
		            select chatroom_id, max(send_date)
		            from messages
		            group by chatroom_id
		        )
		    ) lm
		        on lm.chatroom_id = cu_me.chatroom_id
		    where cu_me.user_id = :userId
		      and cu_me.connection_status != 'DL'
		      and u.name like '%' || :keyword || '%'
			""", nativeQuery = true)
	List<ChatroomListProjection> findChatroomUsersBySearchInput(@Param("keyword") String keyword,
																@Param("userId") long userId);

	// 채팅방 검색 - 검색 키워드가 포함된 메시지를 담고있는 채팅방 반환
	@Query(value = """
			select
		        cu_me.chatroom_users_id as chatroomUserId,
		        cu_me.chatroom_id as chatroomId,
		        cu_me.chatroom_name as chatroomName,
		        cu_me.last_connection_time as lastConnectionTime,
		        cu_me.connection_status as connectionStatus,
		        cu_me.created_time as createdTime,
		        cu_me.is_dark_mode as isDarkMode,
		        cu_other.user_id as receiverId,
		        m.content as message,
		        m.send_date as sendDate
		    from chatroom_users cu_me
		    join messages m
		        on cu_me.chatroom_id = m.chatroom_id
		    join chatroom_users cu_other
		        on cu_me.chatroom_id = cu_other.chatroom_id
		       and cu_other.user_id != :userId
		    where cu_me.connection_status != 'DL'
		      and cu_me.user_id = :userId
		      and m.send_date > cu_me.created_time
		      and m.content like '%' || :keyword || '%'
		      and m.is_gift = 'N'
			""", nativeQuery = true)
	List<MessageSearchResultProjection> findChatroomMessagesBySearchInput(@Param("keyword") String keyword,
			  															  @Param("userId") long userId);
	// 채팅 삭제 → 키값 빼고 초기화
	@Modifying
	@Transactional
	@Query("""
			UPDATE ChatroomEntity c 
			SET c.lastMsgContent = null,
				   c.lastMsgDate = null 
			WHERE c.chatroomId = :chatroomId
			""")
	void initChatroomExceptKey(@Param("chatroomId") long chatroomId);
	
	// 채팅방 마지막 메시지와 보낸 시간 업데이트
	@Modifying
	@Transactional
	@Query("""
			UPDATE ChatroomEntity c 
			SET c.lastMsgContent = :content,
				c.lastMsgDate = :date 
			WHERE c.chatroomId = :chatroomId
			""")
	void updateLastMsgAndDate(@Param("chatroomId") long chatroomId,
							  @Param("content") String content,
							  @Param("date") Date date);
	
}
