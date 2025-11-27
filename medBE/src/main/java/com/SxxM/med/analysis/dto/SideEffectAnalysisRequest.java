package com.sxxm.med.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 부작용 분석 요청 DTO
 * 사용자가 정의한 그룹 구조를 그대로 유지
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SideEffectAnalysisRequest {
    
    /**
     * 사용자 ID (null 허용 - 비로그인 사용자 지원)
     */
    private Long userId;
    
    /**
     * 사용자가 정의한 그룹 목록
     * 각 그룹은 type(food/drug)과 items 배열을 가짐
     * 예: [
     *   {type: "food", items: ["두유"]},
     *   {type: "drug", items: ["베아크라정", "타리온정", "코대원정"]},
     *   {type: "drug", items: ["애니펜정", "키도라제정"]}
     * ]
     */
    private List<GroupRequest> groups;
    
    /**
     * 부작용 설명 (선택적)
     */
    private String description;
}

