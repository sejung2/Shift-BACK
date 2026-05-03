package com.project.shift.shop.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.shift.chat.dto.MessageDTO;
import com.project.shift.chat.dto.MessageWithSenderDTO;
import com.project.shift.chat.service.ChatroomService;
import com.project.shift.chat.service.ChatroomUserService;
import com.project.shift.chat.service.MessageService;
import com.project.shift.global.jwt.JwtService;
import com.project.shift.shop.dto.PaymentRequestDTO;
import com.project.shift.shop.dto.PaymentResponseDTO;
import com.project.shift.shop.dto.PaymentResultDTO;
import com.project.shift.shop.service.IOrderService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final IOrderService orderService;
	private final JwtService jwtService;
	private final ChatroomService chatroomService;
	private final ChatroomUserService chatroomUserService;
	private final MessageService messageService;
	private final ObjectMapper objectMapper;
	
    // SHOP-009 결제 요청
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> requestPayment(@RequestBody PaymentRequestDTO request) {
        PaymentResponseDTO response = orderService.requestPayment(request);
        return ResponseEntity.ok(response);
    }
    
    // SHOP-010 결제 결과 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResultDTO> getPaymentResult(@PathVariable Long orderId) {
        PaymentResultDTO response = orderService.getPaymentResult(orderId);
        return ResponseEntity.ok(response);
    }
    
	// SHOP-018 선물 결제 및 채팅이 전송
    @PostMapping("/gift/{chatroomId}")
    public ResponseEntity<PaymentResponseDTO> requestGiftPayment(HttpServletRequest request, @RequestBody PaymentRequestDTO dto,
    																@PathVariable long chatroomId) {
    	// jwt에서 현재 사용자의 토큰 추출
		String token = jwtService.extractTokenFromRequest(request);
        // 토큰에서 현재 사용자의 PK 추출
		long userId = jwtService.extractUserIdFromValidToken(token);
    		
		PaymentResponseDTO response = orderService.requestGiftPayment(dto, chatroomId, userId);
        return ResponseEntity.ok(response);
    }
    
    // 임시 : 아직 채팅방이 생성되지 않아도 선물을 보낼 수 있게 하는 API
    @PostMapping("/gift/first")
    public ResponseEntity<?> requestFirstGiftPayment(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
    	// jwt에서 현재 사용자의 토큰, pk 추출
		String token = jwtService.extractTokenFromRequest(request);
		long userId = jwtService.extractUserIdFromValidToken(token);
		
		// payload에서 PaymentRequestDTO 추출
		Object paymentRequestObj = payload.get("paymentRequestDTO");
		PaymentRequestDTO paymentRequestDTO = objectMapper.convertValue(paymentRequestObj, PaymentRequestDTO.class);
		
		// payload에서 MessageWithSenderDTO 추출
		Object MessageWithSenderObj = payload.get("messageWithSenderDTO");
		MessageWithSenderDTO messageWithSenderDTO = objectMapper.convertValue(MessageWithSenderObj, MessageWithSenderDTO.class);
		
		// 메시지 내용 세팅 (채팅방 생성을 위해 필요)
		MessageDTO message = messageWithSenderDTO.getMessage();
		message.setContent(orderService.setGiftMessage(paymentRequestDTO));
		messageWithSenderDTO.setMessage(message);
		
		// 채팅방 생성
		long newChatroomId = chatroomService.addChatroom(messageWithSenderDTO);
		messageWithSenderDTO.getMessage().setChatroomId(newChatroomId);
		chatroomUserService.addChatroomUsers(messageWithSenderDTO, newChatroomId);
    	
		// 결제 진행 및 메시지 전송
		PaymentResponseDTO paymentResponse = orderService.requestGiftPayment(paymentRequestDTO, newChatroomId, userId);
		
		// 반환 결과 세팅
		Map<String, Object> response = new HashMap<>();
		response.put("paymentResponse", paymentResponse);
	    response.put("newChatroomId", newChatroomId);
	    
        return ResponseEntity.ok(response);
    }
}
