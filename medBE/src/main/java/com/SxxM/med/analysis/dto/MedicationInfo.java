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
public class MedicationInfo {
    
    private String name;
    private List<String> ingredients;  // 주성분 (active ingredients)
    private List<String> excipients;   // 부형제 (excipients/additives)
    private String description;
    private String manufacturer;
}

