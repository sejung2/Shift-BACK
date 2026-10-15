package com.project.shift.shop;

import com.project.shift.product.repository.PointTransactionRepository;
import com.project.shift.shop.dto.PaymentRequestDTO;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointConcurrencyTest {

    @Autowired
    OrderService orderService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    PointTransactionRepository pointTransactionRepository; // ← 포인트 기록 정리용

    Long userId;
    List<Long> orderIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.builder()
                .loginId("concurrency_user")
                .password("test")
                .name("테스터")
                .build();
        user.updatePoints(1000);
        userId = userRepository.save(user).getUserId();

        for (int i = 0; i < 10; i++) {
            Order order = new Order();
            order.setSenderId(userId);
            order.setReceiverId(userId);
            order.setOrderDate(LocalDateTime.now());
            order.setTotalPrice(100);
            orderIds.add(orderRepository.save(order).getOrderId());
        }
    }

    @AfterEach
    void tearDown() {
        pointTransactionRepository.deleteAll(
                pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)); // ← 주문보다 먼저 삭제 (FK)
        orderRepository.deleteAllById(orderIds);
        userRepository.deleteById(userId);
        orderIds.clear();
    }

    @Test
    @DisplayName("포인트 1000인 사용자가 100씩 10건 동시 결제하면 잔액이 0이 되어야 한다")
    void concurrentPointPayment() throws InterruptedException {
        int threadCount = 10;
        ExecutorService es = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()); // ←
        CountDownLatch latch = new CountDownLatch(threadCount); // ←

        for (Long orderId : orderIds) {
            es.submit(() -> {
                try {
                    PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
                    paymentRequestDTO.setOrderId(orderId);
                    paymentRequestDTO.setAmount(100);
                    paymentRequestDTO.setPointUsed(100);

                    orderService.requestPayment(paymentRequestDTO);
                } catch (Exception e) {
                    System.out.println("실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();  // ← 10개 다 끝날 때까지 대기
        es.shutdown();  // ← 그다음 스레드 풀 정리

        UserEntity user = userRepository.findById(userId).orElseThrow(); // ← DB에서 다시 조회
        int points = user.getPoints();                                   // ←
        System.out.println("남은 포인트 = " + points);                    // ←

        assertThat(points).isEqualTo(0);
    }
}