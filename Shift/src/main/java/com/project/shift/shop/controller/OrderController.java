package com.project.shift.shop.controller;

import static com.project.shift.global.security.CurrentUser.getUserIdOrNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.shift.shop.dto.OrderCancelResponseDTO;
import com.project.shift.shop.dto.OrderDTO;
import com.project.shift.shop.dto.OrderDetailResponseDTO;
import com.project.shift.shop.dto.OrderListResponseDTO;
import com.project.shift.shop.dto.PointOrderCompleteDTO;
import com.project.shift.shop.dto.PointOrderRequestDTO;
import com.project.shift.shop.dto.PointOrderResponseDTO;
import com.project.shift.shop.service.IOrderService;
import com.project.shift.shop.dto.OrderStatusUpdateResponseDTO;


import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor // 생성자 주입을 임의의 코드없이 자동으로 설정해주는 어노테이션
public class OrderController {

    private final IOrderService orderService;

    // SHOP-006 주문 생성
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO request) {
    	  Long uid = getUserIdOrNull();
          if (uid != null) request.setSenderId(uid); 
          return ResponseEntity.ok(orderService.createOrder(request));
    }
    
    //SHOP-007 주문 내역 조회
    @GetMapping
    public ResponseEntity<OrderListResponseDTO> getOrders(@RequestParam(value="userId", required=false) Long userId) {
        Long uid = getUserIdOrNull();
        Long effectiveUserId = uid != null ? uid : userId;
        if (effectiveUserId == null) throw new IllegalArgumentException("userId가 필요합니다. (JWT 또는 파라미터)");
        return ResponseEntity.ok(orderService.getOrdersByUser(effectiveUserId));
    }
    
    // SHOP-008	주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDTO> getOrderDetail(@PathVariable Long orderId) {
        OrderDetailResponseDTO response = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(response);
    }
    
    // SHOP-012 주문 취소
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderCancelResponseDTO> cancelOrder(@PathVariable("orderId") Long orderId) {
        OrderCancelResponseDTO response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }
    
    // SHOP-016 금액권 주문 생성
    @PostMapping("/point")
    public ResponseEntity<PointOrderResponseDTO> createPointOrder(
            @RequestBody PointOrderRequestDTO dto) {

        return ResponseEntity.ok(orderService.createPointOrder(dto));
    }
    
    // SHOP-017 금액권 결제 완료 (포인트 적립)
    @PutMapping("/point/complete/{orderId}")
    public ResponseEntity<PointOrderCompleteDTO> completePointOrder(
            @PathVariable Long orderId) {

        PointOrderCompleteDTO response = orderService.completePointPayment(orderId);
        return ResponseEntity.ok(response);
    }
    
    // SHOP-019 선물 수락
    @PutMapping("/{orderId}/gift/accept")
    public ResponseEntity<OrderStatusUpdateResponseDTO> acceptGift(@PathVariable Long orderId) {
        OrderStatusUpdateResponseDTO response = orderService.acceptGift(orderId);
        return ResponseEntity.ok(response);
    }

    // SHOP-020 구매/수령 확정
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<OrderStatusUpdateResponseDTO> confirmOrder(@PathVariable Long orderId) {
        OrderStatusUpdateResponseDTO response = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(response);
    }
    
}