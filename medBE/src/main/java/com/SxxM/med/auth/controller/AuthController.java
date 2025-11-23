package com.SxxM.med.auth.controller;

import com.SxxM.med.auth.dto.*;
import com.SxxM.med.auth.entity.User;
import com.SxxM.med.auth.service.AuthService;
import com.SxxM.med.auth.service.PasswordService;
import com.SxxM.med.auth.service.UserService;
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
    
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getNickname()
            );
            
            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("회원가입 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 후 JWT 토큰 및 사용자 정보를 받습니다.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("로그인 시도: username={}", request.getUsername());
            String token = authService.login(request.getUsername(), request.getPassword());
            log.info("로그인 성공: username={}, token 생성됨", request.getUsername());
            
            User user = authService.getUserByUsername(request.getUsername());
            
            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .build();
            
            LoginResponse response = LoginResponse.builder()
                    .accessToken(token)
                    .user(userInfo)
                    .build();
            
            log.info("로그인 응답 생성 완료: username={}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("로그인 실패: username={}, error={}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("로그인 중 예상치 못한 오류 발생: username={}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 사용자의 정보를 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null) {
                log.error("인증 정보가 없습니다. JWT 필터에서 인증이 설정되지 않았습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String username = authentication.getName();
            log.info("사용자 정보 조회 요청: username={}", username);
            
            User user = authService.getUserByUsername(username);
            
            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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

