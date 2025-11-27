package com.sxxm.med.ocr.dto;

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
public class OcrAnalysisResponse {
    
    private String ocrText;  // 원본 OCR 텍스트
    private String cleanedText;  // GPT로 정리된 텍스트
    private List<String> extractedIngredients;
    private IngredientAnalysis analysis;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientAnalysis {
        private String safetyLevel; // SAFE, CAUTION, DANGEROUS
        private List<IngredientRisk> ingredientRisks;
        private List<String> expectedSideEffects;
        private String overallAssessment;
        private List<String> recommendations;
        private String foodAllergyRisk;  // 식품 알러지 기반 위험도 평가 (LOW/MEDIUM/HIGH)
        private List<String> matchedFoodAllergens;  // 사용자 식품 알러지와 매칭된 성분 리스트
        private List<String> foodOriginExcipientsDetected;  // 식품 유래 의약품 부형제 목록
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientRisk {
        private String ingredientName;
        private String content;
        private String allergyRisk;
        private String riskLevel;
        private String reason;
    }
}

