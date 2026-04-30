package com.project.shift.shop.service;

import static com.project.shift.global.security.CurrentUser.getUserIdOrNull;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.shift.chat.dto.ChatroomUserDTO;
import com.project.shift.chat.dto.MessageDTO;
import com.project.shift.chat.service.ChatroomUserService;
import com.project.shift.chat.service.MessageService;
import com.project.shift.product.dao.IPointDAO;
import com.project.shift.product.entity.Image;
import com.project.shift.product.entity.PointTransaction;
import com.project.shift.product.entity.Product;
import com.project.shift.product.repository.PointTransactionRepository;
import com.project.shift.product.repository.ProductRepository;
import com.project.shift.shop.dao.IOrderDAO;
import com.project.shift.shop.dto.DeliverySimpleDTO;
import com.project.shift.shop.dto.OrderCancelResponseDTO;
import com.project.shift.shop.dto.OrderDTO;
import com.project.shift.shop.dto.OrderDetailItemDTO;
import com.project.shift.shop.dto.OrderDetailResponseDTO;
import com.project.shift.shop.dto.OrderItemDTO;
import com.project.shift.shop.dto.OrderListDTO;
import com.project.shift.shop.dto.OrderListResponseDTO;
import com.project.shift.shop.dto.OrderStatusUpdateResponseDTO;
import com.project.shift.shop.dto.PaymentDTO;
import com.project.shift.shop.dto.PaymentRequestDTO;
import com.project.shift.shop.dto.PaymentResponseDTO;
import com.project.shift.shop.dto.PaymentResultDTO;
import com.project.shift.shop.dto.PointHistoryDTO;
import com.project.shift.shop.dto.PointHistoryResponseDTO;
import com.project.shift.shop.dto.PointOrderCompleteDTO;
import com.project.shift.shop.dto.PointOrderRequestDTO;
import com.project.shift.shop.dto.PointOrderResponseDTO;
import com.project.shift.shop.dto.RefundRequestDTO;
import com.project.shift.shop.dto.RefundResponseDTO;
import com.project.shift.shop.entity.Delivery;
import com.project.shift.shop.entity.Order;
import com.project.shift.shop.entity.OrderItem;
import com.project.shift.shop.repository.DeliveryRepository;
import com.project.shift.shop.repository.OrderRepository;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // 생성자 주입을 임의의 코드없이 자동으로 설정해주는 어노테이션
public class OrderService implements IOrderService {

    private final IOrderDAO orderDAO;
    private final IPointDAO pointDAO;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChatroomUserService chatroomUserService;

    private String toDisplayOrderStatus(String code) {
        if (code == null) return "PENDING";
        return switch (code) {
            case "S" -> "PAID";
            case "C" -> "CANCELED";
            case "D" -> "COMPLETED";
            case "P" -> "PENDING";
            default -> "PENDING";
        };
    }
    
    private Map.Entry<String, LocalDateTime> toPaymentStatusAndApprovedAt(
            String orderStatus, LocalDateTime orderDate) {

        switch (orderStatus) {
            case "S":
            case "D":
                return new AbstractMap.SimpleEntry<>("SUCCESS", orderDate);

            case "C":
                return new AbstractMap.SimpleEntry<>("CANCELED", null);

            case "P":
            default:
                return new AbstractMap.SimpleEntry<>("PENDING", null);
        }
    }
    
    // SHOP-006 주문 생성
    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
    	   Long uid = getUserIdOrNull();
    	    if (uid != null) {
    	        orderDTO.setSenderId(uid); 
    	    }
    	    if (orderDTO.getSenderId() == null) {
    	        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 주문할 수 있습니다.");
    	    }
    	    
    	    // receiverId가 비어 있고, chatroomId가 있으면 채팅방에서 수령인 찾기
    	    if (orderDTO.getReceiverId() == null) {
    	        Long chatroomId = orderDTO.getChatroomId();

    	        if (chatroomId != null) {
    	            // 이미 금액권에서 쓰고 있는 DAO 메서드 재사용
    	            Long receiverId = orderDAO.findReceiverInChatroom(chatroomId, orderDTO.getSenderId());

    	            if (receiverId == null) {
    	                // 채팅방에 상대방이 없으면 선물 주문 자체가 성립하지 않음
    	                throw new ResponseStatusException(
    	                        HttpStatus.BAD_REQUEST,
    	                        "채팅방에서 수령인을 찾을 수 없습니다. (chatroomId=" + chatroomId + ")"
    	                );
    	            }
    	            orderDTO.setReceiverId(receiverId);
    	        } else {
    	            // chatroomId도 없고 receiverId도 없으면 "나에게 주문"
    	            orderDTO.setReceiverId(orderDTO.getSenderId());
    	        }
    	    }   	    
    	  
    	    if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
    	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 항목이 비어 있습니다.");
    	    }

    	    Order order = new Order();
    	    order.setSenderId(orderDTO.getSenderId());
    	    order.setReceiverId(orderDTO.getReceiverId());
    	    order.setOrderDate(LocalDateTime.now());
    	    order.setOrderStatus("P"); // 결제 전(P)

    	    int totalPrice = 0;

    	    for (OrderItemDTO itemDTO : orderDTO.getItems()) {
    	        Product product = productRepository.findById(itemDTO.getProductId())
    	                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. product_id=" + itemDTO.getProductId()));

    	        int unitPrice = product.getPrice();        // 상품 현재가
    	        int linePrice = unitPrice * itemDTO.getQuantity();
    	        totalPrice += linePrice;

    	        OrderItem orderItem = new OrderItem();
    	        orderItem.setProductId(itemDTO.getProductId());
    	        orderItem.setQuantity(itemDTO.getQuantity());
    	        orderItem.setItemPrice(unitPrice);         // 단가 저장 
    	        order.addItem(orderItem);
    	    }

    	    order.setTotalPrice(totalPrice);

    	    Order saved = orderDAO.save(order);
    	    UserEntity receiver = userRepository.findById(saved.getReceiverId())
    	            .orElse(null);

    	    String recipientName   = (receiver != null && receiver.getName()   != null) ? receiver.getName()   : "수령인";
    	    String recipientPhone  = (receiver != null && receiver.getPhone()  != null) ? receiver.getPhone()  : "00000000000";
    	    String deliveryAddress = (receiver != null && receiver.getAddress() != null) ? receiver.getAddress() : "주소 미등록";

    	    Delivery delivery = new Delivery();
    	    delivery.setOrder(saved);
    	    delivery.setRecipient(recipientName);
    	    delivery.setRecipientPhone(recipientPhone);
    	    delivery.setDeliveryAddress(deliveryAddress);
    	    delivery.setDeliveryStatus("P"); // 배송준비

    	    deliveryRepository.save(delivery);

    	    OrderDTO resp = new OrderDTO();
    	    resp.setOrderId(saved.getOrderId());
    	    resp.setSenderId(saved.getSenderId());
    	    resp.setReceiverId(saved.getReceiverId());
    	    resp.setTotalPrice(saved.getTotalPrice());
    	    resp.setOrderDate(saved.getOrderDate());
    	    resp.setOrderStatus(saved.getOrderStatus());
    	    resp.setResult(true);
    	    resp.setItems(orderDTO.getItems());
    	    return resp;
    	}
    
    // SHOP-007 주문 내역 조회
    @Override
    public OrderListResponseDTO getOrdersByUser(Long userId) {
        List<Order> orders = orderDAO.findBySenderId(userId);

        Map<Long, String> nameCache = new HashMap<>();
        for (Order o : orders) {
            nameCache.computeIfAbsent(o.getSenderId(),
                    id -> userRepository.findById(id)
                            .map(UserEntity::getName)
                            .orElse("탈퇴한 회원"));
            nameCache.computeIfAbsent(o.getReceiverId(),
                    id -> userRepository.findById(id)
                            .map(UserEntity::getName)
                            .orElse("탈퇴한 회원"));
        }

        List<OrderListDTO> list = orders.stream().map(o -> {
            OrderListDTO dto = new OrderListDTO();
            dto.setOrderId(o.getOrderId());
            dto.setSenderId(o.getSenderId());
            dto.setReceiverId(o.getReceiverId());
            dto.setSenderName(nameCache.get(o.getSenderId()));     
            dto.setReceiverName(nameCache.get(o.getReceiverId())); 
            dto.setOrderStatus(toDisplayOrderStatus(o.getOrderStatus())); 
            dto.setOrderDate(o.getOrderDate());
            dto.setTotalPrice(o.getTotalPrice());
            dto.setPointUsed(o.getPointUsed());
            dto.setCashUsed(o.getCashUsed());
            return dto;
        }).collect(Collectors.toList());

        return new OrderListResponseDTO(list);
    }
    
    // SHOP-008 주문 상세 조회
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponseDTO getOrderDetail(Long orderId) {
    	Long uid = getUserIdOrNull();
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 없습니다. orderId=" + orderId));

        if (uid != null && !order.getSenderId().equals(uid)) {
            throw new AccessDeniedException("본인 주문만 조회할 수 있습니다.");
        }
        
        // 상품명을 한 번에 로드 
        List<Long> productIds = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .toList();

        var products = productRepository.findAllById(productIds);
        var productMap = products.stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

        List<OrderDetailItemDTO> itemDTOs = order.getOrderItems().stream().map(oi -> {
            OrderDetailItemDTO dto = new OrderDetailItemDTO();
            dto.setProductId(oi.getProductId());
            dto.setQuantity(oi.getQuantity());
            dto.setItemPrice(oi.getItemPrice());
            dto.setOrderItemId(oi.getOrderItemId());            

            Product product = productMap.get(oi.getProductId());
            if (product != null) {
                dto.setProductName(product.getName());
                dto.setCategoryId(product.getCategoryId());
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    Image mainImage = product.getImages().get(0);
                    
                    String url = mainImage.getImageUrl();
                    dto.setImageUrl(url);
                }
            }
            return dto;
        }).toList();
         
        
        // 결제 정보 (status/approvedAt 포함)
        Integer cash = order.getCashUsed() == null ? 0 : order.getCashUsed();
        Integer point = order.getPointUsed() == null ? 0 : order.getPointUsed();
        var pair = toPaymentStatusAndApprovedAt(order.getOrderStatus(), order.getOrderDate());
        
        PaymentDTO paymentDTO = new PaymentDTO(cash, point);
        paymentDTO.setStatus(pair.getKey());         // SUCCESS/PENDING/CANCELED
        paymentDTO.setApprovedAt(pair.getValue());   // S일 때만 orderDate
        
        DeliverySimpleDTO deliveryDTO = deliveryRepository.findByOrder_OrderId(orderId)
                .map(d -> new DeliverySimpleDTO(d.getDeliveryStatus(), d.getTrackingNumber()))
                .orElse(null);

        // 이름
        String senderName = userRepository.findById(order.getSenderId())
                .map(UserEntity::getName)
                .orElse("탈퇴한 회원");
        String receiverName = userRepository.findById(order.getReceiverId())
                .map(UserEntity::getName)
                .orElse("탈퇴한 회원");

        // 금액권 여부 판정
        boolean voucherOrder = !products.isEmpty() && products.stream().allMatch(p -> p.getCategoryId() == 3);

        
        OrderDetailResponseDTO resp = new OrderDetailResponseDTO();
        resp.setOrderId(order.getOrderId());
        resp.setSenderId(order.getSenderId());
        resp.setReceiverId(order.getReceiverId());
        resp.setSenderName(senderName);    
        resp.setReceiverName(receiverName);
        resp.setOrderDate(order.getOrderDate());
        resp.setItems(itemDTOs);
        resp.setTotalPrice(order.getTotalPrice());
        resp.setPayment(paymentDTO);
        resp.setDelivery(deliveryDTO);
        resp.setVoucherOrder(voucherOrder);
        return resp;
    }
    
    //############### 선물 구매 및 메시지 전송 ###############
    @Override
    public PaymentResponseDTO requestGiftPayment(PaymentRequestDTO requestDTO, long chatroomId, long userId) {
        PaymentResponseDTO dto = requestPayment(requestDTO);

        String content = setGiftMessage(requestDTO);

        MessageDTO messageDTO = MessageDTO.builder()
                .isGift("Y")
                .type(MessageDTO.MessageType.CHAT)
                .chatroomId(chatroomId)
                .sendDate(LocalDateTime.now())
                .unreadCount(1)
                .content(content)
                .userId(userId)
                .build();

        ChatroomUserDTO chatroomUserDTO = chatroomUserService.getChatroomUser(chatroomId, userId);
        messageService.sendAndSaveMessage(messageDTO, chatroomUserDTO);

        return dto;
    }
    
    //############### 선물 메시지 세팅 및 반환 ###############
    @Override
    public String setGiftMessage(PaymentRequestDTO requestDTO) {

        Order order = orderDAO.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // ★ 금액권 판별 (categoryId == 3)
        boolean isVoucherOrder =
                order.getOrderItems().stream().allMatch(item -> {
                    Product p = productRepository.findById(item.getProductId()).orElse(null);
                    return p != null && p.getCategoryId() == 3;
                });

        // 콤마 포맷
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.KOREA);
        String formattedPrice = nf.format(order.getTotalPrice());

        // 메시지 생성
        if (isVoucherOrder) {
            return "💳 금액권 선물이 도착했습니다!\n" + formattedPrice + "원&" + order.getOrderId() + "&POINT";
        } else {
            return "🎁 선물이 도착했습니다!&" + order.getOrderId() + "&PRODUCT";
        }
    }
    
    // SHOP-009 결제 요청
    @Override
    @Transactional
    public PaymentResponseDTO requestPayment(PaymentRequestDTO requestDTO) {
        Long uid = getUserIdOrNull();

        Order order = orderDAO.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."));

        if (uid != null && !order.getSenderId().equals(uid)) {
            throw new AccessDeniedException("본인 주문만 결제할 수 있습니다.");
        }

        int amount = requestDTO.getAmount();
        if (amount <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");

        int pointUsed = (requestDTO.getPointUsed() == null ? 0 : requestDTO.getPointUsed());
        if (pointUsed < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트 사용 금액은 음수가 될 수 없습니다.");

        int cashAmount = amount - pointUsed;
        if (cashAmount < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트 사용 금액이 결제 금액을 초과했습니다.");

        if (!order.getTotalPrice().equals(amount)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        UserEntity sender = userRepository.findById(order.getSenderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

        int currentPoints = (sender.getPoints() == null ? 0 : sender.getPoints());
        if (pointUsed > currentPoints) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보유 포인트가 부족합니다.");
        }

        int remainPoints = currentPoints - pointUsed;
        sender.updatePoints(remainPoints);
        userRepository.save(sender);

        order.setPointUsed(pointUsed);
        order.setCashUsed(cashAmount);
        order.setRemainPoints(remainPoints);
        order.setOrderStatus("S"); // 결제 완료
        orderDAO.save(order);

        if (pointUsed > 0) {
            PointTransaction tx = new PointTransaction();
            tx.setUserId(sender.getUserId());
            tx.setOrderId(order.getOrderId());
            tx.setType("U");               // U = 포인트 사용
            tx.setAmount(pointUsed);
            tx.setCreatedAt(LocalDateTime.now());

            pointTransactionRepository.save(tx);
        }

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setCashAmount(cashAmount);
        response.setPointUsed(pointUsed);
        response.setStatus("SUCCESS");
        response.setApprovedAt(LocalDateTime.now());
        return response;
    }


    // SHOP-010 결제 결과 조회
    @Override
    @Transactional(readOnly = true)
    public PaymentResultDTO getPaymentResult(Long orderId) {
    	Long uid = getUserIdOrNull();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."));

        if (uid != null && !order.getSenderId().equals(uid)) {
            throw new AccessDeniedException("본인 주문만 조회할 수 있습니다.");
        }

        Integer cashUsed = order.getCashUsed() == null ? 0 : order.getCashUsed();
        Integer pointUsed = order.getPointUsed() == null ? 0 : order.getPointUsed();

        String statusCode = order.getOrderStatus(); // P / S / D / C 
        String status;
        LocalDateTime approvedAt = null;

        switch (statusCode) {
            case "S":
            case "D":
                status = "SUCCESS";
                approvedAt = order.getOrderDate();
                break;
            case "C":
                status = "CANCELED";
                break;
            case "P":
            default:
                status = "PENDING";
                break;
        }

        PaymentResultDTO dto = new PaymentResultDTO();
        dto.setOrderId(order.getOrderId());
        dto.setCashAmount(cashUsed);
        dto.setPointUsed(pointUsed);
        dto.setStatus(status);
        dto.setApprovedAt(approvedAt);
        return dto;
    }
    
    // SHOP-011 포인트 사용/적립 내역 조회
    @Override
    @Transactional(readOnly = true)
    public PointHistoryResponseDTO getPointHistory(Long userId) {

        // 사용자 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        int totalPoints = (user.getPoints() == null ? 0 : user.getPoints());

        // DAO에서 포인트 거래내역 조회
        List<PointTransaction> list = pointDAO.findTransactions(userId);

        // DTO 매핑
        List<PointHistoryDTO> history = list.stream()
                .map(t -> PointHistoryDTO.builder()
                        .transactionId(t.getId())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .createdAt(t.getCreatedAt())
                        .orderId(t.getOrderId())
                        .build()
                ).toList();

        return PointHistoryResponseDTO.builder()
                .userId(userId)
                .totalPoints(totalPoints)
                .history(history)
                .result(true)
                .build();
    }

    
    // SHOP-012 주문 취소
    @Override
    @Transactional
    public OrderCancelResponseDTO cancelOrder(Long orderId) {
    	Long uid = getUserIdOrNull();

        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. order_id=" + orderId));

        //본인주문만 취소가능
        if (uid != null && !order.getSenderId().equals(uid)) {
            throw new AccessDeniedException("본인 주문만 취소할 수 있습니다.");
        }
        
        // 배송 정보 조회
        var deliveryOpt = deliveryRepository.findByOrder_OrderId(orderId);

        String orderStatus = order.getOrderStatus();    // P / S / C
        String deliveryStatus = deliveryOpt
                .map(d -> d.getDeliveryStatus())       // P / S / D / C
                .orElse(null);
        
        // 결제 전(P) 취소 
        if ("P".equals(orderStatus)) {
            // 배송이 이미 진행 중이면 취소 불가
            if (deliveryStatus != null && !"P".equals(deliveryStatus)) {
                return new OrderCancelResponseDTO(orderId, false);
            }

            order.setOrderStatus("C");
            orderDAO.save(order);

            deliveryOpt.ifPresent(d -> {
                d.setDeliveryStatus("C");
                deliveryRepository.save(d);
            });

            return new OrderCancelResponseDTO(orderId, true);
        }

        
        // 결제 완료(S) + 배송상태 P 일 때만 취소
	    if (!"S".equals(orderStatus)) {
	        // 이미 취소(C)거나 기타 상태 → 취소 불가
	        return new OrderCancelResponseDTO(orderId, false);
	    }
	    if (!"P".equals(deliveryStatus)) {
	        // 배송이 이미 출발/완료/취소된 경우
	        return new OrderCancelResponseDTO(orderId, false);
	    }

	    // 결제완료 주문에 대한 간이 환불 처리 
	    Integer cashUsed = order.getCashUsed() == null ? 0 : order.getCashUsed();
	    Integer pointUsed = order.getPointUsed() == null ? 0 : order.getPointUsed();

	    // 	포인트 되돌리기 (requestRefund 로직 축약)
	    if (pointUsed > 0) {
	        UserEntity sender = userRepository.findById(order.getSenderId())
	                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

	        int currentPoints = sender.getPoints() == null ? 0 : sender.getPoints();
	        int newPoints = currentPoints + pointUsed;
	        sender.updatePoints(newPoints);
	        userRepository.save(sender);

	        order.setRemainPoints(newPoints);
	    }
	    
	    // TODO : cashUsed에 대해서는 실제 PG 연동 할건지?? 
	    
	    //주문 취소
        order.setOrderStatus("C");
        orderDAO.save(order);

        // 배송 레코드가 있다면 함께 취소 표기
        deliveryOpt.ifPresent(d -> {
                d.setDeliveryStatus("C");
                deliveryRepository.save(d);
        });

        return new OrderCancelResponseDTO(orderId, true);
    }
    
    // SHOP-013 환불 요청
    @Override
    @Transactional
    public RefundResponseDTO requestRefund(RefundRequestDTO requestDTO) {

        // 1) 주문 조회
        Order order = orderDAO.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 1-1) 금액권 주문 여부 체크
        boolean isVoucherOrder = order.getOrderItems() != null
                && !order.getOrderItems().isEmpty()
                && order.getOrderItems().stream().allMatch(oi -> {
                    Product p = productRepository.findById(oi.getProductId()).orElse(null);
                    return p != null && p.getCategoryId() == 3;
                });

        if (isVoucherOrder) {
            throw new IllegalArgumentException("금액권 주문은 환불이 불가능합니다.");
        }

        // 2) 상태 체크
        if (!"S".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("결제 완료된 주문만 환불할 수 있습니다.");
        }

        // 3) 실제 결제 금액(현금 + 포인트)
        int cashUsed = (order.getCashUsed() == null ? 0 : order.getCashUsed());
        int pointUsed = (order.getPointUsed() == null ? 0 : order.getPointUsed());
        int paidTotal = cashUsed + pointUsed;

        // 4) 요청 금액 검증
        Integer requestedAmount = requestDTO.getAmount();
        if (requestedAmount == null || requestedAmount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        if (!requestedAmount.equals(paidTotal)) {
            throw new IllegalArgumentException("환불 금액이 결제 금액과 일치하지 않습니다.");
        }

        // 5) sender 포인트 환불 처리 (포인트 복원)
        UserEntity sender = userRepository.findById(order.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        int currentPoints = sender.getPoints() == null ? 0 : sender.getPoints();
        int pointRefunded = pointUsed;
        int cashRefunded = cashUsed;

        int newPoints = currentPoints + pointRefunded;
        sender.updatePoints(newPoints);
        userRepository.save(sender);

        // 5-1) 포인트 거래내역: 복원(R) 기록 
        PointTransaction refundTx = new PointTransaction();
        refundTx.setUserId(sender.getUserId());
        refundTx.setType("R");                  // R: 복원
        refundTx.setAmount(pointRefunded);      // 환불 포인트
        refundTx.setOrderId(order.getOrderId());
        refundTx.setCreatedAt(LocalDateTime.now());

        pointTransactionRepository.save(refundTx);

        // 6) 주문 상태 변경
        order.setOrderStatus("C");
        order.setRemainPoints(newPoints);
        orderDAO.save(order);

        // 7) 배송 상태 변경
        deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .ifPresent(delivery -> {
                    if (!"C".equals(delivery.getDeliveryStatus())) {
                        delivery.setDeliveryStatus("C");
                        deliveryRepository.save(delivery);
                    }
                });

        // 8) 응답 DTO 구성
        RefundResponseDTO resp = new RefundResponseDTO();
        resp.setOrderId(order.getOrderId());
        resp.setCashRefunded(cashRefunded);
        resp.setPointRefunded(pointRefunded);
        resp.setStatus("REFUNDED");
        resp.setRefundedAt(LocalDateTime.now());
        resp.setResult(true);

        return resp;
    }
    
    // SHOP-016 금액권 주문 생성
    @Override
    @Transactional
    public PointOrderResponseDTO createPointOrder(PointOrderRequestDTO dto) {

        Long senderId = dto.getSenderId();
        if (senderId == null)
            senderId = getUserIdOrNull();

        if (senderId == null)
            throw new IllegalArgumentException("로그인 후 이용 가능합니다.");

        Long chatroomId = null;
        Long receiverId = null;
        
        if (dto.getChatroomId() == null) {
        	receiverId = dto.getReceiverId();
        }
        else {
        	chatroomId = dto.getChatroomId();
        	receiverId = orderDAO.findReceiverInChatroom(chatroomId, senderId);
        }
        if (chatroomId == null && receiverId == null)
        	throw new IllegalArgumentException("선물할 상대가 없습니다.");

        // 받는 사람이 탈퇴한 회원인지 확인
        userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 탈퇴한 회원에게는 선물할 수 없습니다."));
        
//      기존 로직
//      Long chatroomId = dto.getChatroomId();
//      if (chatroomId == null)
//          throw new IllegalArgumentException("chatroomId 필수");
//
//      // receiver 조회
//      Long receiverId = orderDAO.findReceiverInChatroom(chatroomId, senderId);
//      if (receiverId == null)
//          throw new IllegalArgumentException("대화 상대가 없습니다.");

        // 금액권 상품 검증
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("금액권 상품 없음"));

        if (product.getCategoryId() != 3)
            throw new IllegalArgumentException("금액권 상품 아님");

        // Order 생성
        Order order = new Order();
        order.setSenderId(senderId);
        order.setReceiverId(receiverId);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalPrice(dto.getAmount());
        order.setCashUsed(dto.getAmount());
        order.setPointUsed(0);
        order.setRemainPoints(0);
        order.setOrderStatus("P");

        //주문상품 생성
        OrderItem item = new OrderItem();
        item.setProductId(dto.getProductId()); 
        item.setQuantity(1);
        item.setItemPrice(dto.getAmount());

        order.addItem(item); 

        orderDAO.save(order);

        return PointOrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .senderId(senderId)
                //.chatroomId(chatroomId) // 프론트에서 사용하지는 않는데, 함수에 들어올때 chatroomId가 null이면 오류가 발생해서 주석처리 했습니다
                .amount(dto.getAmount())
                .status("P")
                .result(true)
                .build();
    }
    
    // SHOP-017 금액권 결제 완료 (포인트 적립)
    @Override
    @Transactional
    public PointOrderCompleteDTO completePointPayment(Long orderId) {

        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!"P".equals(order.getOrderStatus())) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다.");
        }

        // 1) 상태 변경
        order.setOrderStatus("S");
        orderDAO.save(order);

        Long receiverId = order.getReceiverId();
        Integer amount = order.getTotalPrice();

        // 2) receiver 포인트 적립
        int updatedTotal = pointDAO.addPoints(receiverId, amount); 
        // addPoint = UPDATE users set points = points + amount

        // 3) 포인트 거래내역 insert
        pointDAO.insertPointTransaction(receiverId, amount, "A", orderId);

        return PointOrderCompleteDTO.builder()
                .chatroomId(null) // 필요하면 order.getChatroomId() 저장해놔야 함
                .receiverId(receiverId)
                .addedPoints(amount)
                .updatedTotalPoints(updatedTotal)
                .result(true)
                .build();
    }
    
    // SHOP-019 선물 수락
    @Override
    @Transactional
    public OrderStatusUpdateResponseDTO acceptGift(Long orderId) {
        Long uid = getUserIdOrNull();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 이용 가능합니다.");
        }

        // 1) 주문 조회
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다. orderId=" + orderId));

        // 2) 수신자만 선물 수락 가능
        if (!uid.equals(order.getReceiverId())) {
            throw new AccessDeniedException("선물 수신자만 선물을 수락할 수 있습니다.");
        }

        // 3) 상태 체크: 주문 S(결제완료), 배송 P(배송준비중) 상태에서만 수락 가능
        if (!"S".equals(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 완료된 주문만 선물을 수락할 수 있습니다.");
        }

        Delivery delivery = deliveryRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "배송 정보가 없습니다. orderId=" + orderId));

        if (!"P".equals(delivery.getDeliveryStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송 준비중 상태에서만 선물을 수락할 수 있습니다.");
        }

        // 4) 선물 수락 → 배송 상태 S(배송중)로 변경
        delivery.setDeliveryStatus("S");
        deliveryRepository.save(delivery);

        // 5) 응답 DTO 구성 (리뷰 버튼은 아직 비활성)
        return OrderStatusUpdateResponseDTO.builder()
                .orderId(order.getOrderId())
                .senderId(order.getSenderId())
                .receiverId(order.getReceiverId())
                .orderStatus(order.getOrderStatus())     // S
                .deliveryStatus(delivery.getDeliveryStatus()) // S
                .reviewButtonEnabled(false)             // (주문,배송 둘 다 D가 아니므로 false)
                .confirmedAt(null)
                .result(true)
                .build();
    }
    
    // SHOP-020 구매/수령 확정
    @Override
    @Transactional
    public OrderStatusUpdateResponseDTO confirmOrder(Long orderId) {
        Long uid = getUserIdOrNull();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 이용 가능합니다.");
        }

        // 1) 주문 조회
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다. orderId=" + orderId));

        boolean isSender   = uid.equals(order.getSenderId());
        boolean isReceiver = uid.equals(order.getReceiverId());

        if (!isSender && !isReceiver) {
            throw new AccessDeniedException("해당 주문에 대한 권한이 없습니다.");
        }

        // 선물 플로우에서는 수신자만 수령 확정 가능
        if (!order.getSenderId().equals(order.getReceiverId()) && !isReceiver) {
            throw new AccessDeniedException("선물 수령확정은 수신자만 가능합니다.");
        }

        // 2) 상태 체크: 주문 S, 배송 S 인 경우에만 확정 가능
        if (!"S".equals(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 완료된 주문만 구매/수령 확정할 수 있습니다.");
        }

        Delivery delivery = deliveryRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "배송 정보가 없습니다. orderId=" + orderId));

        boolean selfOrder = order.getSenderId().equals(order.getReceiverId());  // 나에게 구매 여부

        if (selfOrder) {
            // 나에게 구매: 배송상태가 P(배송준비)든 S(배송중)이든 상관없이 확정 가능
            String deliveryStatus = delivery.getDeliveryStatus();
            if (!"P".equals(deliveryStatus) && !"S".equals(deliveryStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "나에게 구매는 배송 준비 또는 배송중 상태에서만 구매 확정이 가능합니다."
                );
            }
        } else {
            // 선물 플로우: 기존 로직 유지 - 배송중(S)에서만 확정 가능
            if (!"S".equals(delivery.getDeliveryStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송중 상태에서만 구매/수령 확정이 가능합니다.");
            }
        }
        // 3) 주문/배송 상태를 D로 변경
        order.setOrderStatus("D");
        orderDAO.save(order);

        delivery.setDeliveryStatus("D");
        deliveryRepository.save(delivery);

        LocalDateTime now = LocalDateTime.now();

        // 4) 응답 DTO 구성 (리뷰 버튼 활성화 상태)
        return OrderStatusUpdateResponseDTO.builder()
                .orderId(order.getOrderId())
                .senderId(order.getSenderId())
                .receiverId(order.getReceiverId())
                .orderStatus(order.getOrderStatus())       // D
                .deliveryStatus(delivery.getDeliveryStatus()) // D
                .reviewButtonEnabled(true)                 // 주문/배송 모두 D → 리뷰 버튼 on
                .confirmedAt(now)
                .result(true)
                .build();
    }



}
