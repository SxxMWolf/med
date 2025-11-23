package com.SxxM.med.ocr.service;

import com.SxxM.med.auth.entity.User;
import com.SxxM.med.auth.entity.UserAllergy;
import com.SxxM.med.auth.repository.UserAllergyRepository;
import com.SxxM.med.auth.repository.UserRepository;
import com.SxxM.med.analysis.service.PythonApiService;
import com.SxxM.med.ocr.dto.OcrAnalysisRequest;
import com.SxxM.med.ocr.dto.OcrAnalysisResponse;
import com.SxxM.med.ocr.entity.OcrIngredient;
import com.SxxM.med.ocr.repository.OcrIngredientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OcrAnalysisService {
    
    private final UserRepository userRepository;
    private final UserAllergyRepository userAllergyRepository;
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
        
        // 사용자 알러지 정보 조회
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(request.getUserId());
        List<String> allergyIngredients = allergies.stream()
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
        
        log.info("사용자 알러지 정보 조회 완료: 알러지 개수={}", allergyIngredients.size());
        
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
        
        // Python 서비스를 통해 성분 분석
        log.info("Python 서비스 호출: 성분 분석");
        try {
            Map<String, Object> analysisResult = pythonApiService.analyzeIngredients(extractedIngredients, allergyIngredients);
            log.info("성분 분석 완료");
            
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
}

