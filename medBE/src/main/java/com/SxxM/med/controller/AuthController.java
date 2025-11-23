package com.SxxM.med.controller;

import com.SxxM.med.dto.*;
import com.SxxM.med.service.AuthService;
import com.SxxM.med.service.PasswordService;
import com.SxxM.med.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "인증 및 사용자 관리 API")
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    private final PasswordService passwordService;
    
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 후 JWT 토큰을 발급받습니다.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            LoginResponse response = LoginResponse.builder()
                    .accessToken(token)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("로그인 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    @PostMapping("/find-username")
    public ResponseEntity<MessageResponse> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        try {
            userService.findUsername(request.getEmail());
            // 보안을 위해 항상 동일한 메시지 반환
            MessageResponse response = MessageResponse.builder()
                    .message("입력하신 이메일로 아이디를 발송했습니다. 이메일을 확인해주세요.")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("아이디 찾기 실패", e);
            // 보안을 위해 동일한 메시지 반환
            MessageResponse response = MessageResponse.builder()
                    .message("입력하신 이메일로 아이디를 발송했습니다. 이메일을 확인해주세요.")
                    .build();
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/find-password")
    public ResponseEntity<MessageResponse> findPassword(@Valid @RequestBody FindPasswordRequest request) {
        try {
            passwordService.sendTemporaryPassword(request.getUsername(), request.getEmail());
            MessageResponse response = MessageResponse.builder()
                    .message("입력하신 이메일로 임시 비밀번호를 발송했습니다. 이메일을 확인해주세요.")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비밀번호 찾기 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.builder()
                            .message("아이디 또는 이메일이 일치하지 않습니다.")
                            .build());
        }
    }
    
    @PostMapping("/change-nickname")
    @Operation(summary = "닉네임 변경", description = "인증된 사용자의 닉네임을 변경합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<MessageResponse> changeNickname(
            Authentication authentication,
            @Valid @RequestBody ChangeNicknameRequest request
    ) {
        try {
            String username = authentication.getName();
            userService.changeNickname(username, request.getNickname());
            MessageResponse response = MessageResponse.builder()
                    .message("닉네임이 변경되었습니다.")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("닉네임 변경 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.builder()
                            .message("닉네임 변경에 실패했습니다: " + e.getMessage())
                            .build());
        }
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "인증된 사용자의 비밀번호를 변경합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            String username = authentication.getName();
            passwordService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
            MessageResponse response = MessageResponse.builder()
                    .message("비밀번호가 변경되었습니다.")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비밀번호 변경 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.builder()
                            .message("비밀번호 변경에 실패했습니다: " + e.getMessage())
                            .build());
        }
    }
}

