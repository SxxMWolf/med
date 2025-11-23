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
public class SymptomAnalysisResponse {
    
    private List<RecommendedMedication> recommendedMedications;
    private List<NotRecommendedMedication> notRecommendedMedications;
    private List<String> precautions;
    
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

