package com.project.shift.shop;

import com.project.shift.global.exception.BadRequestException;
import com.project.shift.global.exception.ConflictException;
import com.project.shift.product.repository.PointTransactionRepository;
import com.project.shift.shop.dto.PaymentRequestDTO;
import com.project.shift.shop.dto.RefundRequestDTO;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class PointConcurrencyTest {

    @Autowired
    OrderService orderService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    PointTransactionRepository pointTransactionRepository; // 포인트 기록 정리용

    Long userId;
    List<Long> orderIds = new ArrayList<>();

    private void loginAs(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ExecutorService newPool(int threadCount) {
        return new DelegatingSecurityContextExecutorService(
                new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()));
    }

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.builder()
                .loginId("concurrency_user")
                .password("test")
                .name("테스터")
                .build();
        user.addPoints(1000);
        userId = userRepository.save(user).getUserId();

        for (int i = 0; i < 10; i++) {
            Order order = new Order();
            order.setSenderId(userId);
            order.setReceiverId(userId);
            order.setOrderDate(LocalDateTime.now());
            order.setTotalPrice(100);
            orderIds.add(orderRepository.save(order).getOrderId());
        }

        loginAs(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        pointTransactionRepository.deleteAll(
                pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)); // 주문보다 먼저 삭제 (FK)
        orderRepository.deleteAllById(orderIds);
        userRepository.deleteById(userId);
        orderIds.clear();
    }

    @Test
    @DisplayName("동시 결제 시 성공한 건수만큼만 포인트가 차감되어야 한다")
    void concurrentPointPayment() throws InterruptedException {
        int threadCount = 10;
        ExecutorService es = newPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (Long orderId : orderIds) {
            es.submit(() -> {
                try {
                    PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
                    paymentRequestDTO.setOrderId(orderId);
                    paymentRequestDTO.setAmount(100);
                    paymentRequestDTO.setPointUsed(100);

                    orderService.requestPayment(paymentRequestDTO);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();  // 10개 다 끝날 때까지 대기
        es.shutdown();  // 그다음 스레드 풀 정리

        UserEntity user = userRepository.findById(userId).orElseThrow(); // DB에서 다시 조회
        int points = user.getPoints();
        System.out.println("성공 = " + successCount.get() + ", 실패 = " + failCount.get());
        System.out.println("남은 포인트 = " + points);

        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);                 // 최소 1건은 결제돼야 함
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);  // 10건 다 처리됐는지
        assertThat(points).isEqualTo(1000 - successCount.get() * 100);            // 성공한 만큼만 차감
        assertThat(pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId).size())
                .isEqualTo(successCount.get());                                   // 포인트 사용 기록 수 = 성공 수
    }

    @Test
    @DisplayName("같은 주문에 동시에 결제 요청이 여러 번 와도 한 번만 결제되어야 한다")
    void duplicatePaymentOnSameOrder() throws InterruptedException {
        int threadCount = 10;
        Long orderId = orderIds.get(0);   // 주문 하나만 사용
        ExecutorService es = newPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            es.submit(() -> {
                try {
                    PaymentRequestDTO dto = new PaymentRequestDTO();
                    dto.setOrderId(orderId);
                    dto.setAmount(100);
                    dto.setPointUsed(0);   // 현금만
                    orderService.requestPayment(dto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        es.shutdown();

        System.out.println("성공 = " + successCount.get());
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 결제된 주문은 다시 결제할 수 없다")
    void cannotPayAlreadyPaidOrder() {
        Long orderId = orderIds.get(0);
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setOrderId(orderId);
        dto.setAmount(100);
        dto.setPointUsed(0);

        orderService.requestPayment(dto);   // 첫 번째: 성공해야 함

        assertThatThrownBy(() -> orderService.requestPayment(dto))   // 두 번째: 예외가 나야 함
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("같은 주문에 동시에 환불 요청이 여러 번 와도 한 번만 환불되어야 한다")
    void duplicateRefundOnSameOrder() throws InterruptedException {
        // 준비: 주문 하나를 포인트 100으로 결제 → 남은 포인트 900
        Long orderId = orderIds.get(0);
        PaymentRequestDTO payDto = new PaymentRequestDTO();
        payDto.setOrderId(orderId);
        payDto.setAmount(100);
        payDto.setPointUsed(100);
        orderService.requestPayment(payDto);

        int threadCount = 10;
        ExecutorService es = newPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            es.submit(() -> {
                try {
                    RefundRequestDTO refundDto = new RefundRequestDTO();
                    refundDto.setOrderId(orderId);
                    refundDto.setAmount(100);          // 결제 금액과 같아야 함
                    orderService.requestRefund(refundDto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        es.shutdown();

        int points = userRepository.findById(userId).orElseThrow().getPoints();
        System.out.println("환불 성공 = " + successCount.get() + ", 남은 포인트 = " + points);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(points).isEqualTo(1000);
    }

    @Test
    @DisplayName("로그인하지 않으면 결제할 수 없다")
    void cannotPayWithoutLogin() {
        SecurityContextHolder.clearContext();   // 로그인 안 한 상태

        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setOrderId(orderIds.get(0));
        dto.setAmount(100);
        dto.setPointUsed(100);

        assertThatThrownBy(() -> orderService.requestPayment(dto))
                .isInstanceOf(BadRequestException.class);

        int points = userRepository.findById(userId).orElseThrow().getPoints();
        assertThat(points).isEqualTo(1000);    // 포인트 그대로
    }

    @Test
    @DisplayName("다른 사용자의 주문은 환불할 수 없다")
    void cannotRefundOthersOrder() {
        // 준비: 본인이 포인트 100으로 결제 → 남은 포인트 900
        Long orderId = orderIds.get(0);
        PaymentRequestDTO payDto = new PaymentRequestDTO();
        payDto.setOrderId(orderId);
        payDto.setAmount(100);
        payDto.setPointUsed(100);
        orderService.requestPayment(payDto);

        loginAs(-1L);   // 다른 사용자로 로그인

        RefundRequestDTO refundDto = new RefundRequestDTO();
        refundDto.setOrderId(orderId);
        refundDto.setAmount(100);

        assertThatThrownBy(() -> orderService.requestRefund(refundDto))
                .isInstanceOf(AccessDeniedException.class);

        int points = userRepository.findById(userId).orElseThrow().getPoints();
        assertThat(points).isEqualTo(900);     // 환불 안 됨
    }
}