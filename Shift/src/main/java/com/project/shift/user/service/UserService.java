package com.project.shift.user.service;

import com.project.shift.auth.repository.RefreshTokenRepository;
import com.project.shift.chat.dao.ChatroomUserDAO;
import com.project.shift.chat.dao.FriendDAO;
import com.project.shift.global.exception.BadRequestException;
import com.project.shift.global.exception.ConflictException;
import com.project.shift.global.exception.NotFoundException;
import com.project.shift.global.security.CurrentUser;
import com.project.shift.shop.dao.CartDAO;
import com.project.shift.shop.entity.Order;
import com.project.shift.shop.repository.DeliveryRepository;
import com.project.shift.shop.repository.OrderRepository;
import com.project.shift.user.dto.request.FindIdRequest;
import com.project.shift.user.dto.request.SignUpRequest;
import com.project.shift.user.dto.request.UpdateRequest;
import com.project.shift.user.dto.response.UserInfoResponse;
import com.project.shift.user.entity.UserEntity;
import com.project.shift.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartDAO cartDAO;
    private final FriendDAO friendDAO;
    private final ChatroomUserDAO chatroomUserDAO;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Long join(SignUpRequest signUpRequest) {

        // 중복 검증
        if (userRepository.existsByLoginId(signUpRequest.loginId())) {
            throw new ConflictException("이미 사용중인 아이디입니다.");
        }
        if (signUpRequest.phone() != null
                && userRepository.existsByPhone(signUpRequest.phone())) {
            throw new ConflictException("이미 사용중인 연락처입니다.");
        }

        UserEntity savedEntity = userRepository.save(
                UserEntity.builder()
                        .loginId(signUpRequest.loginId())
                        .password(passwordEncoder.encode(signUpRequest.password()))
                        .name(signUpRequest.name())
                        .phone(signUpRequest.phone())
                        .address(signUpRequest.address())
                        .build()
        );

        return savedEntity.getUserId();
    }

    // 아이디 중복 확인 - 사용 가능 여부 반환
    public boolean isLoginIdAvailable(String loginId) {
        if (loginId.toLowerCase().startsWith("deleted")) {
            throw new BadRequestException("'deleted'로 시작하는 ID는 사용할 수 없습니다.");
        }

        return userRepository.existsByLoginId(loginId);
    }

    // 연락처 중복 확인 - 사용 가능 여부 반환
    public boolean isPhoneAvailable(String phone) {
        return userRepository.existsByPhone(phone);
    }

    // 로그인 ID로 본인 정보 조회
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo() {
        Long userId = CurrentUser.getUserId();

        //DB에서 회원 조회
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        //비밀번호 제외하고 DTO로 변환하여 반환
        return new UserInfoResponse(
                userEntity.getUserId(),
                userEntity.getLoginId(),
                userEntity.getName(),
                userEntity.getPhone(),
                userEntity.getAddress(),
                userEntity.getPoints()
        );
    }

    // 로그인 ID로 본인 정보 수정
    @Transactional
    public UserInfoResponse updateUserInfo(UpdateRequest updateRequest) {
        Long userId = CurrentUser.getUserId();

        //DB에서 회원 조회
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        // 연락처 변경 시 중복 검증
        if (!Objects.equals(userEntity.getPhone(), updateRequest.phone())
                && userRepository.existsByPhone(updateRequest.phone())) {
            throw new ConflictException("이미 사용중인 연락처 입니다.");
        }

        //회원 정보 수정(Entity 업데이트)
        userEntity.updateInfo(updateRequest.name(), updateRequest.phone(), updateRequest.address());

        return new UserInfoResponse(
                userEntity.getUserId(),
                userEntity.getLoginId(),
                userEntity.getName(),
                userEntity.getPhone(),
                userEntity.getAddress(),
                userEntity.getPoints()
        );
    }

    // 아이디 찾기
    @Transactional(readOnly = true)
    public String findId(FindIdRequest findIdRequest) {
        UserEntity userEntity = userRepository.findByNameAndPhone(findIdRequest.name(), findIdRequest.phone())
                .orElseThrow(() -> new NotFoundException("일치하는 사용자가 없습니다."));

        return maskLoginId(userEntity.getLoginId());
    }

    private String maskLoginId(String loginId) {
        // loginId의 반절만 마스킹 처리
        int length = loginId.length();
        int maskLength = length / 2;
        return loginId.substring(0, length - maskLength) +
                "*".repeat(maskLength);
    }

    // 비밀번호 인증
    @Transactional(readOnly = true)
    public boolean verifyPassword(String password) {
        Long userId = CurrentUser.getUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        return passwordEncoder.matches(password, user.getPassword());
    }

    // 회원 탈퇴
    @Transactional
    public void withdrawUser() {
        Long userId = CurrentUser.getUserId();

        log.info("[USER] 회원 탈퇴 시작 {}", userId);

        // 결제 미완료/에러(P) 자동 삭제
        List<Order> errorOrders = orderRepository.findAllBySenderIdAndOrderStatus(userId, "P");
        if (!errorOrders.isEmpty()) {
            log.info("[탈퇴] 결제 미완료 주문 {}건 자동 삭제 처리", errorOrders.size());
            orderRepository.deleteAll(errorOrders);
        }

        // 진행 중인 주문이 있는지 확인
        boolean hasActiveOrders = deliveryRepository.existsByOrder_SenderIdAndDeliveryStatusIn(userId, List.of("S"));
        if (hasActiveOrders) {
            throw new BadRequestException("현재 배송 중인 상품이 있어 탈퇴할 수 없습니다.\n상품이 도착하여 구매 확정(배송 완료)된 후 다시 시도해주세요.");
        }

        cartDAO.clearCartByUserId(userId); // 장바구니 비우기
        friendDAO.deleteAllFriends(userId); // 친구 관계 삭제
        chatroomUserDAO.deleteChatroomUsersByUserId(userId);

        refreshTokenRepository.deleteByUser_UserId(userId); // 리프레시 토큰 삭제

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        user.withDraw(); // 회원 탈퇴 처리 (정보 초기화 및 탈퇴 시점 기록)

        // SecurityContext 초기화 (로그아웃 처리)
        SecurityContextHolder.clearContext();

        log.info("[USER] 회원 탈퇴 완료 {}", userId);
    }
}
