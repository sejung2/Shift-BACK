package com.project.shift.shop.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointOrderRequestDTO { // SHOP-016 금액권 주문 생성

    private Long chatroomId;   // 금액권을 보내는 채팅방
    private Long receiverId;
    private Long productId;    // 금액권 product_id (category_id = 3)
    private Integer amount;    // 사용자가 입력한 금액
    private Integer cashUsed;  // = amount (금액권은 포인트 결제 불가)
}