package com.sxxm.med.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sxxm.med.auth.entity.UserAllergy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAllergyResponse {
    
    private Long id;
    private String ingredientName;
    private String description;
    private UserAllergy.AllergySeverity severity;
    private UserAllergy.AllergyType allergyType;
    private UserAllergy.FoodAllergyCategory foodCategory;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;
    
    public static UserAllergyResponse from(UserAllergy allergy) {
        if (allergy == null) {
            return null;
        }
        return UserAllergyResponse.builder()
                .id(allergy.getId())
                .ingredientName(allergy.getIngredientName())
                .description(allergy.getDescription())
                .severity(allergy.getSeverity())
                .allergyType(allergy.getAllergyType())
                .foodCategory(allergy.getFoodCategory())
                .createdAt(allergy.getCreatedAt())
                .updatedAt(allergy.getUpdatedAt())
                .build();
    }
}

