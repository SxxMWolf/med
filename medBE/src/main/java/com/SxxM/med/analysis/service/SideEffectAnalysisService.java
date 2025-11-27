package com.sxxm.med.analysis.service;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.analysis.dto.GroupRequest;
import com.sxxm.med.analysis.dto.GroupResult;
import com.sxxm.med.analysis.dto.MedicationInfo;
import com.sxxm.med.analysis.dto.SideEffectAnalysisRequest;
import com.sxxm.med.analysis.dto.SideEffectAnalysisResponse;
import com.sxxm.med.analysis.entity.SideEffectReport;
import com.sxxm.med.analysis.repository.SideEffectReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SideEffectAnalysisService {
    
    private final UserRepository userRepository;
    private final AllergyService allergyService;
    private final SideEffectReportRepository sideEffectReportRepository;
    private final MedicationDbService medicationDbService;
    private final PythonApiService pythonApiService;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 정의 그룹 기반 부작용 분석
     * 각 그룹은 type(food/drug)과 items 배열을 가지며, 그룹 단위로 처리됨
     */
    public SideEffectAnalysisResponse analyzeSideEffect(SideEffectAnalysisRequest request) {
        // 사용자 정보 조회 (비로그인 사용자 지원)
        User user = null;
        List<String> medicationAllergies = new ArrayList<>();
        List<String> foodAllergies = new ArrayList<>();
        
        if (request.getUserId() != null && request.getUserId() > 0) {
            user = userRepository.findById(request.getUserId())
                    .orElse(null);
            
            if (user != null) {
                medicationAllergies = allergyService.getMedicationAllergies(request.getUserId());
                foodAllergies = allergyService.getFoodAllergies(request.getUserId());
            } else {
                log.warn("사용자를 찾을 수 없습니다: userId={}, 알러지 정보 없이 분석 진행", request.getUserId());
            }
        } else {
            log.info("비로그인 사용자로 부작용 분석 진행");
        }
        
        // 그룹 검증
        if (request.getGroups() == null || request.getGroups().isEmpty()) {
            log.error("그룹이 비어있습니다.");
            throw new RuntimeException("최소 하나의 그룹을 입력해주세요.");
        }
        
        log.info("부작용 분석 시작: 사용자 ID={}, 그룹 개수={}", 
                request.getUserId() != null ? request.getUserId() : "비로그인", 
                request.getGroups().size());
        
        // ============================================================
        // 사용자 정의 그룹 단위 처리
        // ============================================================
        List<GroupResult> groupResults = new ArrayList<>();
        List<String> allMedicationNames = new ArrayList<>(); // 전체 약물명 목록 (로깅/저장용)
        
        for (int i = 0; i < request.getGroups().size(); i++) {
            GroupRequest group = request.getGroups().get(i);
            int groupIndex = i + 1; // 1-based index
            
            // 그룹 검증
            if (group.getType() == null || group.getItems() == null || group.getItems().isEmpty()) {
                log.warn("그룹 {}: 타입 또는 항목이 비어있어 건너뜁니다.", groupIndex);
                continue;
            }
            
            String groupType = group.getType().toLowerCase();
            if (!"food".equals(groupType) && !"drug".equals(groupType)) {
                log.warn("그룹 {}: 잘못된 타입 '{}', 건너뜁니다.", groupIndex, groupType);
                continue;
            }
            
            // 그룹 처리
            GroupResult groupResult = processGroup(group, groupIndex);
            if (groupResult != null) {
                groupResults.add(groupResult);
                
                // 의약품인 경우 전체 약물명 목록에 추가
                if ("drug".equals(groupType)) {
                    allMedicationNames.addAll(group.getItems());
                }
                
                log.info("그룹 {} 처리 완료: type={}, 항목 수={}, 성분 수={}", 
                        groupIndex, groupType, group.getItems().size(), 
                        groupResult.getMergedIngredients().size());
            }
        }
        
        if (groupResults.isEmpty()) {
            log.error("처리된 그룹이 없습니다.");
            throw new RuntimeException("유효한 그룹이 없습니다. 그룹 정보를 확인해주세요.");
        }
        
        log.info("사용자 커스텀 그룹 기반 분석 완료: 총 그룹 개수={}", groupResults.size());
        
        // ============================================================
        // 그룹 결과를 Python API 형식으로 변환
        // ============================================================
        List<String> groupNames = groupResults.stream()
                .map(GroupResult::getGroupName)
                .collect(Collectors.toList());
        
        List<List<String>> groupedIngredients = groupResults.stream()
                .map(GroupResult::getMergedIngredients)
                .collect(Collectors.toList());
        
        // Python 서비스를 통해 부작용 분석
        try {
            Map<String, Object> analysisResult = pythonApiService.analyzeSideEffects(
                    groupNames,
                    groupedIngredients,
                    new ArrayList<>(), // 하위 호환성을 위한 빈 리스트
                    request.getDescription(),
                    medicationAllergies,
                    foodAllergies
            );
            
            // Python 서비스 응답을 SideEffectAnalysisResponse로 변환
            SideEffectAnalysisResponse response = convertToSideEffectAnalysisResponse(analysisResult);
            
            // 분석 결과를 DB에 저장 (로그인 사용자인 경우에만)
            if (user != null) {
                try {
                    SideEffectReport report = SideEffectReport.builder()
                            .user(user)
                            .medicationNames(allMedicationNames)
                            .description(request.getDescription())
                            .analysisResult(objectMapper.writeValueAsString(response))
                            .build();
                    sideEffectReportRepository.save(report);
                    log.info("부작용 분석 결과 저장 완료: userId={}", user.getId());
                } catch (Exception e) {
                    log.warn("부작용 분석 결과 저장 실패 (분석은 계속 진행): userId={}, error={}", 
                            user.getId(), e.getMessage());
                }
            } else {
                log.info("비로그인 사용자 분석 결과는 DB에 저장하지 않습니다");
            }
            
            return response;
        } catch (Exception e) {
            log.error("부작용 분석 중 오류 발생", e);
            throw new RuntimeException("부작용 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 개별 그룹 처리
     * 
     * @param group 그룹 요청
     * @param groupIndex 그룹 인덱스 (1-based)
     * @return 그룹 처리 결과
     */
    private GroupResult processGroup(GroupRequest group, int groupIndex) {
        String groupType = group.getType().toLowerCase();
        List<String> items = group.getItems();
        
        try {
            if ("food".equals(groupType)) {
                return processFoodGroup(group, groupIndex);
            } else if ("drug".equals(groupType)) {
                return processDrugGroup(group, groupIndex);
            } else {
                log.warn("그룹 {}: 알 수 없는 타입 '{}'", groupIndex, groupType);
                return null;
            }
        } catch (Exception e) {
            log.error("그룹 {} 처리 중 오류 발생: type={}, items={}", 
                    groupIndex, groupType, items, e);
            // 오류가 발생해도 빈 성분 리스트로 처리하여 분석 계속 진행
            return GroupResult.builder()
                    .groupIndex(groupIndex)
                    .originalItems(new ArrayList<>(items))
                    .groupType(groupType)
                    .mergedIngredients(new ArrayList<>())
                    .groupName(String.join(", ", items))
                    .build();
        }
    }
    
    /**
     * 식품 그룹 처리: GPT 기반 성분 추론
     */
    private GroupResult processFoodGroup(GroupRequest group, int groupIndex) {
        List<String> items = group.getItems();
        
        log.info("그룹 {} 처리 시작: type=food, 항목 수={}", groupIndex, items.size());
        
        // Python API를 통해 식품 성분 추론
        Map<String, List<String>> foodIngredientsMap = pythonApiService.inferFoodIngredients(items);
        
        // 그룹 내 모든 식품의 성분을 합집합으로 처리
        Set<String> groupIngredientSet = new HashSet<>();
        for (String foodName : items) {
            List<String> ingredients = foodIngredientsMap.get(foodName);
            if (ingredients != null && !ingredients.isEmpty()) {
                groupIngredientSet.addAll(ingredients);
            }
        }
        
        String groupName = String.join(", ", items);
        
        log.info("그룹 {} 처리 완료: type=food, 항목 수={}, 성분 수={}", 
                groupIndex, items.size(), groupIngredientSet.size());
        
        return GroupResult.builder()
                .groupIndex(groupIndex)
                .originalItems(new ArrayList<>(items))
                .groupType("food")
                .mergedIngredients(new ArrayList<>(groupIngredientSet))
                .groupName(groupName)
                .build();
    }
    
    /**
     * 의약품 그룹 처리: MFDS API를 통한 성분 조회
     */
    private GroupResult processDrugGroup(GroupRequest group, int groupIndex) {
        List<String> items = group.getItems();
        
        log.info("그룹 {} 처리 시작: type=drug, 항목 수={}", groupIndex, items.size());
        
        // 그룹 내 모든 약물의 정보 조회
        List<MedicationInfo> medicationInfos = medicationDbService.getMedicationInfoList(items);
        
        // 그룹 내 모든 약물의 성분을 합집합으로 처리
        Set<String> groupIngredientSet = new HashSet<>();
        for (MedicationInfo med : medicationInfos) {
            if (med.getIngredients() != null) {
                groupIngredientSet.addAll(med.getIngredients());
            }
            if (med.getExcipients() != null) {
                groupIngredientSet.addAll(med.getExcipients());
            }
        }
        
        String groupName = String.join(", ", items);
        
        log.info("그룹 {} 처리 완료: type=drug, 항목 수={}, 성분 수={}", 
                groupIndex, items.size(), groupIngredientSet.size());
        
        return GroupResult.builder()
                .groupIndex(groupIndex)
                .originalItems(new ArrayList<>(items))
                .groupType("drug")
                .mergedIngredients(new ArrayList<>(groupIngredientSet))
                .groupName(groupName)
                .build();
    }
    
    private SideEffectAnalysisResponse convertToSideEffectAnalysisResponse(Map<String, Object> analysisResult) {
        SideEffectAnalysisResponse response = new SideEffectAnalysisResponse();
        
        if (analysisResult.containsKey("common_ingredients")) {
            @SuppressWarnings("unchecked")
            List<String> commonIngredients = (List<String>) analysisResult.get("common_ingredients");
            response.setCommonIngredients(commonIngredients);
        }
        
        if (analysisResult.containsKey("user_sensitive_ingredients")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sensitiveList = (List<Map<String, Object>>) analysisResult.get("user_sensitive_ingredients");
            List<SideEffectAnalysisResponse.SensitiveIngredient> sensitiveIngredients = sensitiveList.stream()
                    .map(this::convertToSensitiveIngredient)
                    .collect(Collectors.toList());
            response.setUserSensitiveIngredients(sensitiveIngredients);
        }
        
        if (analysisResult.containsKey("common_side_effect_ingredients")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sideEffectList = (List<Map<String, Object>>) analysisResult.get("common_side_effect_ingredients");
            List<SideEffectAnalysisResponse.CommonSideEffectIngredient> sideEffectIngredients = sideEffectList.stream()
                    .map(this::convertToCommonSideEffectIngredient)
                    .collect(Collectors.toList());
            response.setCommonSideEffectIngredients(sideEffectIngredients);
        }
        
        if (analysisResult.containsKey("summary")) {
            response.setSummary(analysisResult.get("summary").toString());
        }
        
        if (analysisResult.containsKey("food_allergy_risk")) {
            response.setFoodAllergyRisk(analysisResult.get("food_allergy_risk").toString());
        }
        
        if (analysisResult.containsKey("matched_food_allergens")) {
            @SuppressWarnings("unchecked")
            List<String> matchedAllergens = (List<String>) analysisResult.get("matched_food_allergens");
            response.setMatchedFoodAllergens(matchedAllergens);
        }
        
        if (analysisResult.containsKey("food_origin_excipients_detected")) {
            @SuppressWarnings("unchecked")
            List<String> excipients = (List<String>) analysisResult.get("food_origin_excipients_detected");
            response.setFoodOriginExcipientsDetected(excipients);
        }
        
        // 식품 알러지 분석 결과 처리
        if (analysisResult.containsKey("food_allergy_analysis")) {
            Object foodAllergyAnalysisObj = analysisResult.get("food_allergy_analysis");
            if (foodAllergyAnalysisObj != null && foodAllergyAnalysisObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> foodAllergyAnalysisMap = (Map<String, Object>) foodAllergyAnalysisObj;
                SideEffectAnalysisResponse.FoodAllergyAnalysis foodAllergyAnalysis = 
                        convertToFoodAllergyAnalysis(foodAllergyAnalysisMap);
                response.setFoodAllergyAnalysis(foodAllergyAnalysis);
            }
        }
        
        return response;
    }
    
    private SideEffectAnalysisResponse.SensitiveIngredient convertToSensitiveIngredient(Map<String, Object> map) {
        SideEffectAnalysisResponse.SensitiveIngredient ingredient = new SideEffectAnalysisResponse.SensitiveIngredient();
        if (map.containsKey("ingredient_name")) {
            ingredient.setIngredientName(map.get("ingredient_name").toString());
        }
        if (map.containsKey("reason")) {
            ingredient.setReason(map.get("reason").toString());
        }
        if (map.containsKey("severity")) {
            ingredient.setSeverity(map.get("severity").toString());
        }
        if (map.containsKey("is_food_origin")) {
            ingredient.setIsFoodOrigin(Boolean.parseBoolean(map.get("is_food_origin").toString()));
        }
        if (map.containsKey("food_allergy_match")) {
            ingredient.setFoodAllergyMatch(Boolean.parseBoolean(map.get("food_allergy_match").toString()));
        }
        return ingredient;
    }
    
    private SideEffectAnalysisResponse.CommonSideEffectIngredient convertToCommonSideEffectIngredient(Map<String, Object> map) {
        SideEffectAnalysisResponse.CommonSideEffectIngredient ingredient = new SideEffectAnalysisResponse.CommonSideEffectIngredient();
        if (map.containsKey("ingredient_name")) {
            ingredient.setIngredientName(map.get("ingredient_name").toString());
        }
        if (map.containsKey("side_effect_description")) {
            ingredient.setSideEffectDescription(map.get("side_effect_description").toString());
        }
        if (map.containsKey("frequency")) {
            ingredient.setFrequency(map.get("frequency").toString());
        }
        return ingredient;
    }
    
    private SideEffectAnalysisResponse.FoodAllergyAnalysis convertToFoodAllergyAnalysis(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        SideEffectAnalysisResponse.FoodAllergyAnalysis analysis = 
                new SideEffectAnalysisResponse.FoodAllergyAnalysis();
        
        if (map.containsKey("detected_food_origin_ingredients")) {
            Object ingredientsObj = map.get("detected_food_origin_ingredients");
            if (ingredientsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> ingredients = (List<String>) ingredientsObj;
                analysis.setDetectedFoodOriginIngredients(ingredients);
            } else {
                analysis.setDetectedFoodOriginIngredients(new ArrayList<>());
            }
        } else {
            analysis.setDetectedFoodOriginIngredients(new ArrayList<>());
        }
        
        if (map.containsKey("matched_allergens")) {
            Object allergensObj = map.get("matched_allergens");
            if (allergensObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> allergens = (List<String>) allergensObj;
                analysis.setMatchedAllergens(allergens);
            } else {
                analysis.setMatchedAllergens(new ArrayList<>());
            }
        } else {
            analysis.setMatchedAllergens(new ArrayList<>());
        }
        
        if (map.containsKey("risk_assessment")) {
            Object riskAssessmentObj = map.get("risk_assessment");
            if (riskAssessmentObj != null) {
                analysis.setRiskAssessment(riskAssessmentObj.toString());
            } else {
                analysis.setRiskAssessment("");
            }
        } else {
            analysis.setRiskAssessment("");
        }
        
        return analysis;
    }
}
