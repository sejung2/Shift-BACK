package com.project.shift.chat.entity;

import com.project.shift.chat.dto.request.FriendDTO;
import com.project.shift.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "FRIENDS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FriendEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "SEQ_FRIENDS"
    )
    @SequenceGenerator(
            name = "SEQ_FRIENDS",
            sequenceName = "SEQ_FRIENDS",
            allocationSize = 1
    )
    @Column(name = "FRIENDSHIP_ID")
    private Long friendshipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FRIEND_ID", nullable = false)
    private UserEntity friend;
}
