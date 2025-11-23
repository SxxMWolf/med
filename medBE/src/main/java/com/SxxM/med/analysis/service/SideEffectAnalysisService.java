package com.SxxM.med.analysis.service;

import com.SxxM.med.auth.entity.User;
import com.SxxM.med.auth.entity.UserAllergy;
import com.SxxM.med.auth.repository.UserAllergyRepository;
import com.SxxM.med.auth.repository.UserRepository;
import com.SxxM.med.analysis.dto.MedicationInfo;
import com.SxxM.med.analysis.dto.SideEffectAnalysisRequest;
import com.SxxM.med.analysis.dto.SideEffectAnalysisResponse;
import com.SxxM.med.analysis.entity.SideEffectReport;
import com.SxxM.med.analysis.repository.SideEffectReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SideEffectAnalysisService {
    
    private final UserRepository userRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final SideEffectReportRepository sideEffectReportRepository;
    private final MedicationDbService medicationDbService;
    private final PythonApiService pythonApiService;
    private final ObjectMapper objectMapper;
    
    public SideEffectAnalysisResponse analyzeSideEffect(SideEffectAnalysisRequest request) {
        // 사용자 정보 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // 사용자 알러지 정보 조회
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(request.getUserId());
        List<String> allergyIngredients = allergies.stream()
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
        
        // 각 약물의 성분 정보 조회
        List<MedicationInfo> medicationInfos = medicationDbService.getMedicationInfoList(request.getMedicationNames());
        
        // 약물별 성분 리스트 준비
        List<List<String>> medicationIngredients = medicationInfos.stream()
                .map(MedicationInfo::getIngredients)
                .collect(Collectors.toList());
        
        // Python 서비스를 통해 부작용 분석
        try {
            Map<String, Object> analysisResult = pythonApiService.analyzeSideEffects(
                    request.getMedicationNames(),
                    medicationIngredients,
                    allergyIngredients,
                    request.getDescription()
            );
            
            // Python 서비스 응답을 SideEffectAnalysisResponse로 변환
            SideEffectAnalysisResponse response = convertToSideEffectAnalysisResponse(analysisResult);
            
            // 분석 결과를 DB에 저장
            SideEffectReport report = SideEffectReport.builder()
                    .user(user)
                    .medicationNames(request.getMedicationNames())
                    .description(request.getDescription())
                    .analysisResult(objectMapper.writeValueAsString(response))
                    .build();
            sideEffectReportRepository.save(report);
            
            return response;
        } catch (Exception e) {
            log.error("부작용 분석 중 오류 발생", e);
            throw new RuntimeException("부작용 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    private SideEffectAnalysisResponse convertToSideEffectAnalysisResponse(Map<String, Object> analysisResult) {
        SideEffectAnalysisResponse response = new SideEffectAnalysisResponse();
        
        if (analysisResult.containsKey("common_ingredients")) {
            @SuppressWarnings("unchecked")
            List<String> commonIngredients = (List<String>) analysisResult.get("common_ingredients");
            response.setCommonIngredients(commonIngredients);
        }
        
        if (analysisResult.containsKey("user_sensitive_ingredients")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sensitiveList = (List<Map<String, Object>>) analysisResult.get("user_sensitive_ingredients");
            List<SideEffectAnalysisResponse.SensitiveIngredient> sensitiveIngredients = sensitiveList.stream()
                    .map(this::convertToSensitiveIngredient)
                    .collect(Collectors.toList());
            response.setUserSensitiveIngredients(sensitiveIngredients);
        }
        
        if (analysisResult.containsKey("common_side_effect_ingredients")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sideEffectList = (List<Map<String, Object>>) analysisResult.get("common_side_effect_ingredients");
            List<SideEffectAnalysisResponse.CommonSideEffectIngredient> sideEffectIngredients = sideEffectList.stream()
                    .map(this::convertToCommonSideEffectIngredient)
                    .collect(Collectors.toList());
            response.setCommonSideEffectIngredients(sideEffectIngredients);
        }
        
        if (analysisResult.containsKey("summary")) {
            response.setSummary(analysisResult.get("summary").toString());
        }
        
        return response;
    }
    
    private SideEffectAnalysisResponse.SensitiveIngredient convertToSensitiveIngredient(Map<String, Object> map) {
        SideEffectAnalysisResponse.SensitiveIngredient ingredient = new SideEffectAnalysisResponse.SensitiveIngredient();
        if (map.containsKey("ingredient_name")) {
            ingredient.setIngredientName(map.get("ingredient_name").toString());
        }
        if (map.containsKey("reason")) {
            ingredient.setReason(map.get("reason").toString());
        }
        if (map.containsKey("severity")) {
            ingredient.setSeverity(map.get("severity").toString());
        }
        return ingredient;
    }
    
    private SideEffectAnalysisResponse.CommonSideEffectIngredient convertToCommonSideEffectIngredient(Map<String, Object> map) {
        SideEffectAnalysisResponse.CommonSideEffectIngredient ingredient = new SideEffectAnalysisResponse.CommonSideEffectIngredient();
        if (map.containsKey("ingredient_name")) {
            ingredient.setIngredientName(map.get("ingredient_name").toString());
        }
        if (map.containsKey("side_effect_description")) {
            ingredient.setSideEffectDescription(map.get("side_effect_description").toString());
        }
        if (map.containsKey("frequency")) {
            ingredient.setFrequency(map.get("frequency").toString());
        }
        return ingredient;
    }
}

