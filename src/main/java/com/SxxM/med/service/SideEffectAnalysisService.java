package com.SxxM.med.service;

import com.SxxM.med.dto.MedicationInfo;
import com.SxxM.med.dto.SideEffectAnalysisRequest;
import com.SxxM.med.dto.SideEffectAnalysisResponse;
import com.SxxM.med.entity.SideEffectReport;
import com.SxxM.med.entity.User;
import com.SxxM.med.entity.UserAllergy;
import com.SxxM.med.repository.SideEffectReportRepository;
import com.SxxM.med.repository.UserAllergyRepository;
import com.SxxM.med.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final GptService gptService;
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
        
        // 공통 성분 추출
        List<String> commonIngredients = medicationDbService.extractCommonIngredients(medicationInfos);
        
        // 모든 약물의 성분 목록 수집
        List<String> allIngredients = medicationInfos.stream()
                .flatMap(med -> med.getIngredients().stream())
                .distinct()
                .collect(Collectors.toList());
        
        // GPT 프롬프트 생성
        String prompt = buildSideEffectAnalysisPrompt(
                request.getMedicationNames(),
                medicationInfos,
                commonIngredients,
                allIngredients,
                allergyIngredients,
                request.getDescription()
        );
        
        // GPT 분석 요청
        try {
            SideEffectAnalysisResponse response = gptService.analyzeWithGpt(prompt, SideEffectAnalysisResponse.class);
            
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
    
    private String buildSideEffectAnalysisPrompt(
            List<String> medicationNames,
            List<MedicationInfo> medicationInfos,
            List<String> commonIngredients,
            List<String> allIngredients,
            List<String> allergyIngredients,
            String description
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자가 다음 약물들을 복용한 후 부작용을 경험했습니다:\n\n");
        
        medicationNames.forEach(name -> prompt.append("- ").append(name).append("\n"));
        prompt.append("\n");
        
        if (description != null && !description.isEmpty()) {
            prompt.append("부작용 설명: ").append(description).append("\n\n");
        }
        
        prompt.append("각 약물의 성분 정보:\n");
        medicationInfos.forEach(med -> {
            prompt.append("- ").append(med.getName()).append(": ");
            prompt.append(String.join(", ", med.getIngredients())).append("\n");
        });
        prompt.append("\n");
        
        if (!commonIngredients.isEmpty()) {
            prompt.append("공통 성분: ").append(String.join(", ", commonIngredients)).append("\n\n");
        }
        
        if (!allergyIngredients.isEmpty()) {
            prompt.append("사용자의 알러지 성분 목록:\n");
            allergyIngredients.forEach(ingredient -> 
                prompt.append("- ").append(ingredient).append("\n")
            );
            prompt.append("\n");
        }
        
        prompt.append("""
                다음 정보를 포함하여 JSON 형식으로 응답해주세요:
                1. 공통 성분 (commonIngredients): 모든 약물에 공통으로 포함된 성분 목록
                2. 사용자 민감 가능 성분 (userSensitiveIngredients): 사용자의 알러지 성분과 일치하거나 유사한 성분, 이유, 심각도
                3. 많은 사람에게 부작용이 일어나는 성분 (commonSideEffectIngredients): 일반적으로 부작용을 일으키는 것으로 알려진 성분, 부작용 설명, 발생 빈도
                4. 요약 (summary): 전체 분석 요약
                
                JSON 형식:
                {
                  "commonIngredients": ["성분1", "성분2"],
                  "userSensitiveIngredients": [
                    {
                      "ingredientName": "성분명",
                      "reason": "민감한 이유",
                      "severity": "MILD|MODERATE|SEVERE"
                    }
                  ],
                  "commonSideEffectIngredients": [
                    {
                      "ingredientName": "성분명",
                      "sideEffectDescription": "부작용 설명",
                      "frequency": "빈도"
                    }
                  ],
                  "summary": "전체 분석 요약"
                }
                """);
        
        return prompt.toString();
    }
}

