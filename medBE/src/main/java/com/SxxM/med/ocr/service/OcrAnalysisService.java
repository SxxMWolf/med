package com.sxxm.med.ocr.service;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.analysis.service.AllergyService;
import com.sxxm.med.analysis.service.PythonApiService;
import com.sxxm.med.ocr.dto.OcrAnalysisRequest;
import com.sxxm.med.ocr.dto.OcrAnalysisResponse;
import com.sxxm.med.ocr.entity.OcrIngredient;
import com.sxxm.med.ocr.repository.OcrIngredientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OcrAnalysisService {
    
    private final UserRepository userRepository;
    private final AllergyService allergyService;
    private final OcrIngredientRepository ocrIngredientRepository;
    private final VisionService visionService;
    private final PythonApiService pythonApiService;
    private final ObjectMapper objectMapper;
    
    public OcrAnalysisResponse analyzeOcrImage(OcrAnalysisRequest request) {
        if (request.getUserId() == null) {
            throw new RuntimeException("사용자 ID가 설정되지 않았습니다");
        }
        
        log.info("OCR 분석 시작: userId={}", request.getUserId());
        
        // 사용자 정보 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // 사용자 알러지 정보 조회 (약물 알러지와 식품 알러지 분리)
        List<String> medicationAllergies = allergyService.getMedicationAllergies(request.getUserId());
        List<String> foodAllergies = allergyService.getFoodAllergies(request.getUserId());
        List<String> allAllergies = new ArrayList<>(medicationAllergies);
        allAllergies.addAll(foodAllergies);
        
        log.info("사용자 알러지 정보 조회 완료: 약물 알러지 개수={}, 식품 알러지 개수={}", 
                medicationAllergies.size(), foodAllergies.size());
        
        // OCR 텍스트 추출
        log.info("OCR 텍스트 추출 시작");
        String ocrText = visionService.extractTextFromImage(request.getImageData(), request.isBase64());
        
        if (ocrText == null || ocrText.trim().isEmpty()) {
            log.warn("이미지에서 텍스트를 추출할 수 없습니다");
            throw new RuntimeException("이미지에서 텍스트를 추출할 수 없습니다");
        }
        
        log.info("OCR 텍스트 추출 완료: 텍스트 길이={}", ocrText.length());
        
        // Python 서비스를 통해 OCR 텍스트 정규화 및 정리
        log.info("Python 서비스 호출: OCR 텍스트 정규화 및 정리");
        List<String> extractedIngredients;
        String cleanedText = ocrText; // 기본값은 원본 텍스트
        try {
            Map<String, Object> normalizeResult = pythonApiService.normalizeOcrText(ocrText);
            @SuppressWarnings("unchecked")
            List<String> ingredients = (List<String>) normalizeResult.get("normalized_ingredients");
            extractedIngredients = ingredients != null ? ingredients : List.of();
            cleanedText = normalizeResult.containsKey("cleaned_text") ? 
                    normalizeResult.get("cleaned_text").toString() : ocrText;
            log.info("OCR 텍스트 정규화 완료: 성분 개수={}, 정리된 텍스트 길이={}", 
                    extractedIngredients.size(), cleanedText.length());
        } catch (Exception e) {
            log.error("OCR 텍스트 정규화 실패", e);
            throw new RuntimeException("OCR 텍스트 정규화 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
        
        // OCR 텍스트에서 식품 알러지 트리거 성분 검색
        List<String> detectedFoodAllergenTriggers = detectFoodAllergenTriggers(ocrText, foodAllergies);
        log.info("식품 알러지 트리거 성분 감지: {}", detectedFoodAllergenTriggers);
        
        // Python 서비스를 통해 성분 분석
        log.info("Python 서비스 호출: 성분 분석");
        try {
            Map<String, Object> analysisResult = pythonApiService.analyzeIngredients(
                    extractedIngredients, 
                    allAllergies,
                    medicationAllergies,
                    foodAllergies
            );
            log.info("성분 분석 완료");
            
            // 식품 알러지 트리거 성분 정보를 분석 결과에 추가
            if (!detectedFoodAllergenTriggers.isEmpty()) {
                analysisResult.put("detected_food_allergen_triggers", detectedFoodAllergenTriggers);
            }
            
            // Python 서비스 응답을 OcrAnalysisResponse로 변환
            OcrAnalysisResponse response = convertToOcrAnalysisResponse(analysisResult, ocrText, cleanedText, extractedIngredients);
            
            // 분석 결과를 DB에 저장
            try {
                OcrIngredient ocrIngredient = OcrIngredient.builder()
                        .user(user)
                        .imageUrl(request.isBase64() ? "base64_data" : request.getImageData())
                        .ocrText(ocrText)
                        .ingredientList(extractedIngredients)
                        .analysisResult(objectMapper.writeValueAsString(response))
                        .build();
                ocrIngredientRepository.save(ocrIngredient);
                log.info("OCR 분석 결과 DB 저장 완료");
            } catch (Exception e) {
                log.warn("OCR 분석 결과 DB 저장 실패 (응답은 반환)", e);
                // DB 저장 실패해도 응답은 반환
            }
            
            return response;
        } catch (Exception e) {
            log.error("OCR 분석 중 오류 발생", e);
            throw new RuntimeException("OCR 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    private OcrAnalysisResponse convertToOcrAnalysisResponse(
            Map<String, Object> analysisResult,
            String ocrText,
            String cleanedText,
            List<String> extractedIngredients
    ) {
        OcrAnalysisResponse response = new OcrAnalysisResponse();
        response.setOcrText(ocrText);
        response.setCleanedText(cleanedText != null ? cleanedText : ocrText);
        response.setExtractedIngredients(extractedIngredients);
        
        // Python 서비스 응답 파싱
        OcrAnalysisResponse.IngredientAnalysis analysis = new OcrAnalysisResponse.IngredientAnalysis();
        
        if (analysisResult.containsKey("safety_level")) {
            analysis.setSafetyLevel(analysisResult.get("safety_level").toString());
        }
        
        if (analysisResult.containsKey("ingredient_risks")) {
            try {
                Object risksObj = analysisResult.get("ingredient_risks");
                if (risksObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> risks = (List<Map<String, Object>>) risksObj;
                    // 중첩 구조를 피하기 위해 각 Map의 값들을 안전하게 추출
                    List<OcrAnalysisResponse.IngredientRisk> ingredientRisks = risks.stream()
                            .map(this::convertToIngredientRisk)
                            .collect(Collectors.toList());
                    analysis.setIngredientRisks(ingredientRisks);
                } else {
                    log.warn("ingredient_risks가 List 타입이 아닙니다: {}", risksObj != null ? risksObj.getClass() : "null");
                }
            } catch (Exception e) {
                log.error("ingredient_risks 파싱 중 오류 발생", e);
                // 파싱 실패 시 빈 리스트 설정
                analysis.setIngredientRisks(List.of());
            }
        }
        
        if (analysisResult.containsKey("expected_side_effects")) {
            @SuppressWarnings("unchecked")
            List<String> sideEffects = (List<String>) analysisResult.get("expected_side_effects");
            analysis.setExpectedSideEffects(sideEffects);
        }
        
        if (analysisResult.containsKey("overall_assessment")) {
            analysis.setOverallAssessment(analysisResult.get("overall_assessment").toString());
        }
        
        if (analysisResult.containsKey("recommendations")) {
            @SuppressWarnings("unchecked")
            List<String> recommendations = (List<String>) analysisResult.get("recommendations");
            analysis.setRecommendations(recommendations);
        }
        
        if (analysisResult.containsKey("food_allergy_risk")) {
            analysis.setFoodAllergyRisk(analysisResult.get("food_allergy_risk").toString());
        }
        
        if (analysisResult.containsKey("matched_food_allergens")) {
            @SuppressWarnings("unchecked")
            List<String> matchedAllergens = (List<String>) analysisResult.get("matched_food_allergens");
            analysis.setMatchedFoodAllergens(matchedAllergens);
        }
        
        if (analysisResult.containsKey("food_origin_excipients_detected")) {
            @SuppressWarnings("unchecked")
            List<String> excipients = (List<String>) analysisResult.get("food_origin_excipients_detected");
            analysis.setFoodOriginExcipientsDetected(excipients);
        }
        
        if (analysisResult.containsKey("detected_food_allergen_triggers")) {
            @SuppressWarnings("unchecked")
            List<String> triggers = (List<String>) analysisResult.get("detected_food_allergen_triggers");
            // 트리거 정보를 matchedFoodAllergens에 추가
            if (analysis.getMatchedFoodAllergens() == null) {
                analysis.setMatchedFoodAllergens(new ArrayList<>());
            }
            analysis.getMatchedFoodAllergens().addAll(triggers);
        }
        
        response.setAnalysis(analysis);
        return response;
    }
    
    private OcrAnalysisResponse.IngredientRisk convertToIngredientRisk(Map<String, Object> riskMap) {
        OcrAnalysisResponse.IngredientRisk risk = new OcrAnalysisResponse.IngredientRisk();
        
        // 안전하게 값 추출 및 문자열 변환
        if (riskMap.containsKey("ingredient_name")) {
            Object value = riskMap.get("ingredient_name");
            risk.setIngredientName(value != null ? value.toString() : null);
        }
        if (riskMap.containsKey("content")) {
            Object value = riskMap.get("content");
            risk.setContent(value != null ? value.toString() : null);
        }
        if (riskMap.containsKey("allergy_risk")) {
            Object value = riskMap.get("allergy_risk");
            risk.setAllergyRisk(value != null ? value.toString() : null);
        }
        if (riskMap.containsKey("risk_level")) {
            Object value = riskMap.get("risk_level");
            risk.setRiskLevel(value != null ? value.toString() : null);
        }
        if (riskMap.containsKey("reason")) {
            Object value = riskMap.get("reason");
            risk.setReason(value != null ? value.toString() : null);
        }
        return risk;
    }
    
    /**
     * OCR 텍스트에서 식품 알러지 트리거 성분 검색
     * 예: "젤라틴", "소젤라틴", "유당", "레시틴", "땅콩유", "전분", "콩유", "난백" 등
     */
    private List<String> detectFoodAllergenTriggers(String ocrText, List<String> foodAllergies) {
        if (ocrText == null || ocrText.trim().isEmpty() || foodAllergies == null || foodAllergies.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> detectedTriggers = new ArrayList<>();
        String lowerOcrText = ocrText.toLowerCase();
        
        // 식품 알러지 그룹별 트리거 성분 매핑 (7개 그룹)
        Map<String, List<String>> foodAllergenGroups = new HashMap<>();
        foodAllergenGroups.put("NUTS", Arrays.asList("땅콩", "peanut", "아몬드", "almond", "호두", "walnut",
                "피스타치오", "pistachio", "캐슈넛", "cashew", "헤이즐넛", "hazelnut", "macadamia", "브라질넛"));
        foodAllergenGroups.put("DAIRY_EGG", Arrays.asList("우유", "milk", "유청", "whey", "카제인", "casein",
                "계란", "egg", "난백", "albumin", "ovalbumin", "lysozyme"));
        foodAllergenGroups.put("SEAFOOD", Arrays.asList("연어", "salmon", "참치", "tuna", "cod", "fish",
                "새우", "shrimp", "게", "crab", "crustacean", "조개", "clam", "mussel", "oyster", "mollusc"));
        foodAllergenGroups.put("GRAINS_GLUTEN", Arrays.asList("밀", "wheat", "글루텐", "gluten", "보리", "barley", "호밀", "rye"));
        foodAllergenGroups.put("SOY", Arrays.asList("대두", "soy", "soybean", "레시틴", "lecithin"));
        foodAllergenGroups.put("SEEDS", Arrays.asList("참깨", "sesame", "해바라기씨", "sunflower seed"));
        foodAllergenGroups.put("OTHER", Arrays.asList("젤라틴", "gelatin", "아황산", "sulfite", "sulphite",
                "셀러리", "celery", "겨자", "mustard", "루핀", "lupin"));
        
        // 사용자의 식품 알러지 그룹에 대해 트리거 성분 검색
        for (String foodAllergy : foodAllergies) {
            String upperFoodAllergy = foodAllergy.toUpperCase();
            List<String> triggers = foodAllergenGroups.getOrDefault(upperFoodAllergy, new ArrayList<>());
            
            // 트리거 성분이 OCR 텍스트에 포함되어 있는지 확인
            for (String trigger : triggers) {
                if (lowerOcrText.contains(trigger.toLowerCase())) {
                    detectedTriggers.add(trigger);
                }
            }
        }
        
        return detectedTriggers.stream().distinct().collect(Collectors.toList());
    }
}

