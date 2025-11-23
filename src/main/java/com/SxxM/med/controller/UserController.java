package com.SxxM.med.controller;

import com.SxxM.med.entity.User;
import com.SxxM.med.entity.UserAllergy;
import com.SxxM.med.repository.UserAllergyRepository;
import com.SxxM.med.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<List<UserAllergy>> getUserAllergies(@PathVariable Long userId) {
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(userId);
        return ResponseEntity.ok(allergies);
    }
    
    @PostMapping("/{userId}/allergies")
    public ResponseEntity<UserAllergy> addUserAllergy(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        UserAllergy allergy = UserAllergy.builder()
                .user(user)
                .ingredientName(request.get("ingredientName"))
                .description(request.get("description"))
                .severity(UserAllergy.AllergySeverity.valueOf(
                        request.getOrDefault("severity", "MODERATE")))
                .build();
        
        UserAllergy saved = userAllergyRepository.save(allergy);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}

