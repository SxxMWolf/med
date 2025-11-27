package com.sxxm.med.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomAnalysisResponse {
    
    private List<RecommendedMedication> recommendedMedications;
    private List<NotRecommendedMedication> notRecommendedMedications;
    private List<String> precautions;
    private String foodAllergyRisk;  // 식품 알러지 기반 위험도 평가 (LOW/MEDIUM/HIGH)
    private List<String> matchedFoodAllergens;  // 사용자 식품 알러지와 매칭된 성분 리스트
    private List<String> foodOriginExcipientsDetected;  // 식품 유래 의약품 부형제 목록
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedMedication {
        private String name;
        private String reason;
        private String dosage;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotRecommendedMedication {
        private String name;
        private String reason;
        private List<String> allergicIngredients;
    }
}

