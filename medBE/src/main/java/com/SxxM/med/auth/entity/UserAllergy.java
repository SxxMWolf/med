package com.sxxm.med.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_allergies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAllergy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String ingredientName;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private AllergySeverity severity;
    
    @Column(name = "allergy_type")
    @Enumerated(EnumType.STRING)
    private AllergyType allergyType;
    
    @Column(name = "food_category")
    @Enumerated(EnumType.STRING)
    private FoodAllergyCategory foodCategory;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum AllergySeverity {
        MILD, MODERATE, SEVERE
    }
    
    public enum AllergyType {
        MEDICATION,  // 약물 알러지
        FOOD         // 식품 알러지
    }
    
    public enum FoodAllergyCategory {
        NUTS,
        DAIRY_EGG,
        SEAFOOD,
        GRAINS_GLUTEN,
        SOY,
        SEEDS,
        OTHER
    }
}

