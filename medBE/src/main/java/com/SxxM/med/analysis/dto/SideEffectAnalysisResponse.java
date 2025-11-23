package com.SxxM.med.analysis.dto;

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
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SensitiveIngredient {
        private String ingredientName;
        private String reason;
        private String severity;
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

