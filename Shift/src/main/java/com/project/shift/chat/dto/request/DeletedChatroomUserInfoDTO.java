package com.project.shift.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedChatroomUserInfoDTO {

	private long chatroomId;
	private long senderId;
	private long receiverId;
	private String senderName;
	private String receiverName;
}
