package com.project.shift.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.shift.chat.dto.response.FriendInfoResponse;
import com.project.shift.chat.entity.FriendEntity;
import org.springframework.transaction.annotation.Transactional;

public interface FriendRepository extends JpaRepository<FriendEntity, Long>{
	
	// 친구 목록(이름, 아이디 포함) 반환
	@Query(value = """
			select
				f.friendship_id as friendshipId,
				f.friend_id as friendId,
				u.name,
				u.login_id as loginId,
				u.phone
			from friends f, users u
			where f.friend_id = u.user_id
				and f.user_id = :userId
				and u.deleted_at is null
			""", nativeQuery = true)
	List<FriendInfoResponse> getUserFriends(@Param("userId") long userId);

	boolean existsByUser_UserIdAndFriend_UserId(long userId, long friendId);

	void deleteByUser_UserIdAndFriend_UserId(long userId, long friendId);

    // 친구 관계 삭제(탈퇴 시)
	@Transactional
    @Modifying
    @Query(value = "DELETE FROM FriendEntity f WHERE f.user.userId = :userId OR f.friend.userId = :userId")
    void deleteFriendship(@Param("userId") long userId);
}
