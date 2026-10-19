package com.project.shift.shop;

import com.project.shift.global.exception.BadRequestException;
import com.project.shift.shop.controller.OrderController;
import com.project.shift.shop.dto.OrderDTO;
import com.project.shift.shop.dto.OrderItemDTO;
import com.project.shift.shop.dto.PointOrderRequestDTO;
import com.project.shift.shop.dto.PointOrderResponseDTO;
import com.project.shift.shop.entity.Order;
import com.project.shift.shop.repository.OrderRepository;
import com.project.shift.shop.service.OrderService;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderSenderTest {

    @Autowired
    OrderController orderController;
    @Autowired
    OrderService orderService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    static final long VOUCHER_PRODUCT_ID = 999001L;

    Long attackerId;
    Long victimId;

    @BeforeEach
    void setUp() {
        attackerId = saveUser("sender_attacker");
        victimId = saveUser("sender_victim");

        jdbcTemplate.update("INSERT INTO categories (category_id, category_name) VALUES (3, '금액권')");
        jdbcTemplate.update(
                "INSERT INTO products (product_id, category_id, product_name, price, stock) VALUES (?, 3, '테스트 금액권', 10000, 100)",
                VOUCHER_PRODUCT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Long saveUser(String loginId) {
        UserEntity user = UserEntity.builder()
                .loginId(loginId)
                .password("test")
                .name("테스터")
                .build();
        return userRepository.save(user).getUserId();
    }

    private void loginAs(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("금액권 주문은 로그인한 사용자 이름으로 만들어진다")
    void pointOrderUsesLoggedInUserAsSender() {
        loginAs(attackerId);

        PointOrderRequestDTO dto = new PointOrderRequestDTO();
        dto.setReceiverId(attackerId);  // 받는 사람은 자기 자신
        dto.setProductId(VOUCHER_PRODUCT_ID);
        dto.setAmount(10000);

        PointOrderResponseDTO response = orderService.createPointOrder(dto);

        Order order = orderRepository.findById(response.getOrderId()).orElseThrow();
        assertThat(order.getSenderId()).isEqualTo(attackerId);
    }

    @Test
    @DisplayName("로그인하지 않으면 요청에 senderId를 넣어도 주문을 만들 수 없다")
    void cannotCreateOrderWithoutLogin() {
        OrderDTO dto = new OrderDTO();
        dto.setSenderId(victimId);  // 다른 사람 ID를 넣음

        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(VOUCHER_PRODUCT_ID);
        item.setQuantity(1);
        dto.setItems(List.of(item));

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("로그인하지 않으면 userId 파라미터로 남의 주문 목록을 조회할 수 없다")
    void cannotGetOrdersWithoutLogin() {
        assertThatThrownBy(() -> orderController.getOrders(victimId))
                .isInstanceOf(BadRequestException.class);
    }
}