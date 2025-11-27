package com.sxxm.med.analysis.service;

import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.analysis.dto.SymptomAnalysisRequest;
import com.sxxm.med.analysis.dto.SymptomAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SymptomAnalysisService {
    
    private final UserRepository userRepository;
    private final AllergyService allergyService;
    private final GptService gptService;
    
    public SymptomAnalysisResponse analyzeSymptom(SymptomAnalysisRequest request) {
        // 사용자 존재 여부 확인
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // 사용자 알러지 정보 조회 (약물 알러지와 식품 알러지 분리)
        List<String> medicationAllergies = allergyService.getMedicationAllergies(request.getUserId());
        List<String> foodAllergies = allergyService.getFoodAllergies(request.getUserId());
        
        // GPT 프롬프트 생성
        String prompt = buildSymptomAnalysisPrompt(request.getSymptomText(), medicationAllergies, foodAllergies);
        
        // GPT 분석 요청
        try {
            SymptomAnalysisResponse response = gptService.analyzeWithGpt(prompt, SymptomAnalysisResponse.class);
            return response;
        } catch (Exception e) {
            log.error("증상 분석 중 오류 발생", e);
            throw new RuntimeException("증상 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    private String buildSymptomAnalysisPrompt(String symptomText, List<String> medicationAllergies, List<String> foodAllergies) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자가 다음과 같은 증상을 호소하고 있습니다:\n\n");
        prompt.append("증상: ").append(symptomText).append("\n\n");
        
        // 약물 알러지 정보
        if (!medicationAllergies.isEmpty()) {
            prompt.append("사용자의 약물 알러지 성분 목록:\n");
            medicationAllergies.forEach(ingredient -> 
                prompt.append("- ").append(ingredient).append("\n")
            );
            prompt.append("\n");
        }
        
        // 식품 알러지 정보
        if (!foodAllergies.isEmpty()) {
            prompt.append("사용자의 식품 알러지 성분 목록:\n");
            foodAllergies.forEach(ingredient -> 
                prompt.append("- ").append(ingredient).append("\n")
            );
            prompt.append("\n");
        }
        
        prompt.append("""
                다음 규칙을 반드시 준수하여 분석하세요:
                
                1. 약물 추천 시 고려사항:
                   - 사용자의 약물 알러지 성분 목록과 식품 알러지 성분 목록을 모두 고려하세요.
                   - 약물의 주성분(active ingredients)뿐만 아니라 부형제(excipients), 첨가제(additives), 식품 유래 성분까지 모두 확인하세요.
                   - 특히 식품 알러지 → 의약품 부형제 위험성 연결 규칙을 명확히 적용하세요.
                   예: 땅콩 알러지 → 땅콩유가 포함된 약물 피하기
                   예: 유당 불내증 → 유당(락토스)이 포함된 약물 피하기
                   예: 계란 알러지 → 난백, 계란알부민이 포함된 약물 피하기
                   예: 글루텐 알러지 → 밀전분, 글루텐이 포함된 약물 피하기
                
                2. 피해야 할 약물 판단 기준:
                   - 약물의 주성분에 사용자 알러지 성분이 포함된 경우
                   - 약물의 부형제나 첨가제에 사용자 알러지 성분이 포함된 경우
                   - 식품 알러지와 관련된 의약품 부형제가 포함된 경우
                
                3. 다음 정보를 포함하여 JSON 형식으로 응답해주세요:
                   - 추천 약물 목록 (recommendedMedications): 각 약물의 이름, 추천 이유, 복용법
                   - 피해야 할 약물 목록 (notRecommendedMedications): 알러지 성분이 포함된 약물, 피해야 하는 이유, 포함된 알러지 성분
                   - 주의 사항 (precautions): 복용 시 주의해야 할 사항들
                   - 식품 알러지 기반 위험 분석 (foodAllergyRisk): 식품 알러지로 인한 위험도 평가
                   - 매칭된 식품 알러지 성분 (matchedFoodAllergens): 사용자 식품 알러지와 매칭된 성분 리스트
                   - 식품 유래 부형제 감지 (foodOriginExcipientsDetected): 식품 유래 의약품 부형제 목록
                
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
                  "precautions": ["주의사항1", "주의사항2"],
                  "foodAllergyRisk": "위험도 평가 (LOW/MEDIUM/HIGH)",
                  "matchedFoodAllergens": ["매칭된 식품 알러지 성분1", "매칭된 식품 알러지 성분2"],
                  "foodOriginExcipientsDetected": ["젤라틴", "유당", "레시틴"]
                }
                """);
        
        return prompt.toString();
    }
}

