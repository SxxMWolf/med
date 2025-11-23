package com.SxxM.med.auth.controller;

import com.SxxM.med.auth.dto.UserAllergyResponse;
import com.SxxM.med.auth.entity.User;
import com.SxxM.med.auth.entity.UserAllergy;
import com.SxxM.med.auth.repository.UserAllergyRepository;
import com.SxxM.med.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "사용자 및 알러지 관리 API")
public class UserController {
    
    private final UserRepository userRepository;
    private final UserAllergyRepository userAllergyRepository;
    
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{userId}/allergies")
    @Operation(summary = "알러지 목록 조회", description = "사용자의 알러지 정보 목록을 조회합니다.")
    public ResponseEntity<List<UserAllergyResponse>> getUserAllergies(@PathVariable Long userId) {
        log.info("알러지 목록 조회 요청: userId={}", userId);
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(userId);
        log.info("조회된 알러지 개수: {}", allergies.size());
        
        List<UserAllergyResponse> responses = allergies.stream()
                .map(allergy -> {
                    log.debug("알러지 변환: id={}, createdAt={}, updatedAt={}", 
                            allergy.getId(), allergy.getCreatedAt(), allergy.getUpdatedAt());
                    return UserAllergyResponse.from(allergy);
                })
                .collect(Collectors.toList());
        
        log.info("알러지 응답 생성 완료: 개수={}", responses.size());
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/{userId}/allergies")
    @Operation(summary = "알러지 추가", description = "사용자의 알러지 정보를 추가합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<UserAllergyResponse> addUserAllergy(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestBody Map<String, String> request
    ) {
        // 본인만 접근 가능하도록 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        String username = authentication.getName();
        if (!user.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        UserAllergy allergy = UserAllergy.builder()
                .user(user)
                .ingredientName(request.get("ingredientName"))
                .description(request.get("description"))
                .severity(UserAllergy.AllergySeverity.valueOf(
                        request.getOrDefault("severity", "MODERATE")))
                .build();
        
        UserAllergy saved = userAllergyRepository.save(allergy);
        UserAllergyResponse response = UserAllergyResponse.from(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/{userId}/allergies/{allergyId}")
    @Operation(summary = "알러지 삭제", description = "사용자의 알러지 정보를 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> deleteUserAllergy(
            Authentication authentication,
            @PathVariable Long userId,
            @PathVariable Long allergyId
    ) {
        try {
            // 본인만 접근 가능하도록 검증
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            String username = authentication.getName();
            if (!user.getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            UserAllergy allergy = userAllergyRepository.findById(allergyId)
                    .orElseThrow(() -> new RuntimeException("알러지 정보를 찾을 수 없습니다: " + allergyId));
            
            // 알러지가 해당 사용자의 것인지 확인
            if (!allergy.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            userAllergyRepository.delete(allergy);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("알러지 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

