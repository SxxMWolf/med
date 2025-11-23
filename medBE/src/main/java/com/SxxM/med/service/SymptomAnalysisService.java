package com.SxxM.med.service;

import com.SxxM.med.dto.SymptomAnalysisRequest;
import com.SxxM.med.dto.SymptomAnalysisResponse;
import com.SxxM.med.entity.UserAllergy;
import com.SxxM.med.repository.UserAllergyRepository;
import com.SxxM.med.repository.UserRepository;
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
public class SymptomAnalysisService {
    
    private final UserRepository userRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final GptService gptService;
    
    public SymptomAnalysisResponse analyzeSymptom(SymptomAnalysisRequest request) {
        // 사용자 존재 여부 확인
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // 사용자 알러지 정보 조회
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(request.getUserId());
        List<String> allergyIngredients = allergies.stream()
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
        
        // GPT 프롬프트 생성
        String prompt = buildSymptomAnalysisPrompt(request.getSymptomText(), allergyIngredients);
        
        // GPT 분석 요청
        try {
            SymptomAnalysisResponse response = gptService.analyzeWithGpt(prompt, SymptomAnalysisResponse.class);
            return response;
        } catch (Exception e) {
            log.error("증상 분석 중 오류 발생", e);
            throw new RuntimeException("증상 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    private String buildSymptomAnalysisPrompt(String symptomText, List<String> allergyIngredients) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자가 다음과 같은 증상을 호소하고 있습니다:\n\n");
        prompt.append("증상: ").append(symptomText).append("\n\n");
        
        if (!allergyIngredients.isEmpty()) {
            prompt.append("사용자의 알러지 성분 목록:\n");
            allergyIngredients.forEach(ingredient -> 
                prompt.append("- ").append(ingredient).append("\n")
            );
            prompt.append("\n");
        }
        
        prompt.append("""
                다음 정보를 포함하여 JSON 형식으로 응답해주세요:
                1. 추천 약물 목록 (recommendedMedications): 각 약물의 이름, 추천 이유, 복용법
                2. 피해야 할 약물 목록 (notRecommendedMedications): 알러지 성분이 포함된 약물, 피해야 하는 이유, 포함된 알러지 성분
                3. 주의 사항 (precautions): 복용 시 주의해야 할 사항들
                
                JSON 형식:
                {
                  "recommendedMedications": [
                    {
                      "name": "약물명",
                      "reason": "추천 이유",
                      "dosage": "복용법"
                    }
                  ],
                  "notRecommendedMedications": [
                    {
                      "name": "약물명",
                      "reason": "피해야 하는 이유",
                      "allergicIngredients": ["알러지 성분1", "알러지 성분2"]
                    }
                  ],
                  "precautions": ["주의사항1", "주의사항2"]
                }
                """);
        
        return prompt.toString();
    }
}

