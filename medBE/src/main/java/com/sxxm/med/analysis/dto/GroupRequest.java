package com.sxxm.med.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 사용자가 정의한 그룹 단위 요청
 * 각 그룹은 type(food/drug)과 items 배열을 가짐
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    
    /**
     * 그룹 타입: "food" 또는 "drug"
     */
    private String type;
    
    /**
     * 그룹 내 항목 목록
     * 예: ["두유"] 또는 ["베아크라정", "타리온정", "코대원정"]
     */
    private List<String> items;
}

