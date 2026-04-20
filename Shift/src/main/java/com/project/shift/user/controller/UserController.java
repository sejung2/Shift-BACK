package com.project.shift.user.controller;

import com.project.shift.global.security.CurrentUser;
import com.project.shift.shop.dto.PointHistoryResponseDTO;
import com.project.shift.shop.service.IOrderService;
import com.project.shift.user.dto.request.*;
import com.project.shift.user.dto.response.UserInfoResponse;
import com.project.shift.user.dto.response.UserPointResponse;
import com.project.shift.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final IOrderService orderService;

    @PostMapping
    public ResponseEntity<?> registerUser(@RequestBody @Valid SignUpRequest signUpRequest) {
        //서버 회원가입 요청
        Long userId = userService.join(signUpRequest);

        //성공 응답(201 Created)
        return new ResponseEntity<>("회원가입 성공. 할당된 사용자 ID:" + userId, HttpStatus.CREATED);
    }

    // 연락처 중복 확인
    @PostMapping("/check/phone")
    public ResponseEntity<?> checkPhone(@RequestBody @Valid PhoneCheckRequest phoneCheckRequest) {
        String phone = phoneCheckRequest.phone();
        boolean isDuplicate = userService.isPhoneAvailable(phone);
        return ResponseEntity.ok(Map.of(
                "available", !isDuplicate,
                "message", isDuplicate ? "이미 사용중인 연락처입니다." : "사용 가능한 연락처입니다."
        ));
    }

    // 아이디 중복 확인
    @PostMapping("/check")
    public ResponseEntity<?> checkLoginId(@RequestBody @Valid LoginIdCheckRequest loginIdCheckRequest) {
        String loginId = loginIdCheckRequest.loginId();
        boolean isDuplicate = userService.isLoginIdAvailable(loginId);
        return ResponseEntity.ok(Map.of(
                "available", !isDuplicate,
                "message", isDuplicate ? "이미 사용중인 아이디입니다." : "사용 가능한 아이디입니다."
        ));
    }

    // 본인 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    // 본인 정보 수정
    @PutMapping("/info")
    public ResponseEntity<UserInfoResponse> updateMyInfo(
            @RequestBody @Valid UpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateUserInfo(updateRequest));
    }

    // 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody @Valid FindIdRequest findIdRequest) {
        String loginId = userService.findId(findIdRequest);
        return ResponseEntity.ok(Map.of("loginId", loginId));
    }

    // SHOP-011 포인트 사용/적립 내역 조회
    @GetMapping("/points/history")
    public ResponseEntity<PointHistoryResponseDTO> getPointHistory() {
        Long userId = CurrentUser.getUserId();
        return ResponseEntity.ok(orderService.getPointHistory(userId));
    }

    // 마이포인트 조회
    @GetMapping("/points")
    public ResponseEntity<UserPointResponse> getMyPoints() {
        UserInfoResponse user = userService.getUserInfo();
        return ResponseEntity.ok(new UserPointResponse(user.points()));
    }

    // 비밀번호 인증
    @PostMapping("/check/password")
    public ResponseEntity<?> verifyPassword(@RequestBody @Valid PasswordCheckRequest passwordCheckRequest) {
        String password = passwordCheckRequest.password();

        boolean isValid = userService.verifyPassword(password);
        return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "message", isValid ? "비밀번호 인증에 성공했습니다." : "비밀번호가 일치하지 않습니다."
        ));
    }

    // 회원 탈퇴
    @DeleteMapping
    public ResponseEntity<?> withdrawUser() {
        userService.withdrawUser();
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 성공적으로 처리되었습니다."));
    }
}