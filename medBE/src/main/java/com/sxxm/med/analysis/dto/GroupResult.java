package com.sxxm.med.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 그룹 처리 결과
 * 각 그룹의 처리 결과를 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResult {
    
    /**
     * 그룹 인덱스 (1-based)
     */
    private Integer groupIndex;
    
    /**
     * 원본 항목 목록
     */
    private List<String> originalItems;
    
    /**
     * 그룹 타입: "food" 또는 "drug"
     */
    private String groupType;
    
    /**
     * 그룹 내 모든 항목의 성분 합집합
     */
    private List<String> mergedIngredients;
    
    /**
     * 그룹 이름 (표시용)
     */
    private String groupName;
}

