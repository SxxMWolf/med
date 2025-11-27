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
public class SideEffectAnalysisResponse {
    
    private List<String> commonIngredients;
    private List<SensitiveIngredient> userSensitiveIngredients;
    private List<CommonSideEffectIngredient> commonSideEffectIngredients;
    private String summary;
    private String foodAllergyRisk;  // 식품 알러지 기반 위험도 평가 (LOW/MEDIUM/HIGH)
    private List<String> matchedFoodAllergens;  // 사용자 식품 알러지와 매칭된 성분 리스트
    private List<String> foodOriginExcipientsDetected;  // 식품 유래 의약품 부형제 목록
    private FoodAllergyAnalysis foodAllergyAnalysis;  // 식품 알러지 분석 결과
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FoodAllergyAnalysis {
        private List<String> detectedFoodOriginIngredients;  // 검출된 식품 유래 성분
        private List<String> matchedAllergens;  // 매칭된 식품 알러지
        private String riskAssessment;  // 위험도 평가
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SensitiveIngredient {
        private String ingredientName;
        private String reason;
        private String severity;
        private Boolean isFoodOrigin;  // 식품 유래 성분 여부
        private Boolean foodAllergyMatch;  // 식품 알러지와 매칭 여부
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommonSideEffectIngredient {
        private String ingredientName;
        private String sideEffectDescription;
        private String frequency;
    }
}

