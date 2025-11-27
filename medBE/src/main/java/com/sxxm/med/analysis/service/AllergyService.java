package com.sxxm.med.analysis.service;

import com.sxxm.med.auth.entity.UserAllergy;
import com.sxxm.med.auth.repository.UserAllergyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 알러지 정보를 조회하고 분리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AllergyService {
    
    private final UserAllergyRepository userAllergyRepository;
    
    /**
     * 사용자의 약물 알러지 목록 조회
     */
    public List<String> getMedicationAllergies(Long userId) {
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(userId);
        return allergies.stream()
                .filter(allergy -> allergy.getAllergyType() == null || 
                        allergy.getAllergyType() == UserAllergy.AllergyType.MEDICATION)
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 식품 알러지 목록 조회
     */
    public List<String> getFoodAllergies(Long userId) {
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(userId);
        return allergies.stream()
                .filter(allergy -> allergy.getAllergyType() == UserAllergy.AllergyType.FOOD)
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 모든 알러지 목록 조회 (약물 + 식품)
     */
    public List<String> getAllAllergies(Long userId) {
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(userId);
        return allergies.stream()
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
    }
}

