package com.SxxM.med.dto;

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
    
    private String ocrText;
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

