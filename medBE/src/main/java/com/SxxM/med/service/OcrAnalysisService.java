package com.SxxM.med.service;

import com.SxxM.med.dto.OcrAnalysisRequest;
import com.SxxM.med.dto.OcrAnalysisResponse;
import com.SxxM.med.entity.OcrIngredient;
import com.SxxM.med.entity.User;
import com.SxxM.med.entity.UserAllergy;
import com.SxxM.med.repository.OcrIngredientRepository;
import com.SxxM.med.repository.UserAllergyRepository;
import com.SxxM.med.repository.UserRepository;
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
        // 사용자 정보 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // 사용자 알러지 정보 조회
        List<UserAllergy> allergies = userAllergyRepository.findByUserId(request.getUserId());
        List<String> allergyIngredients = allergies.stream()
                .map(UserAllergy::getIngredientName)
                .collect(Collectors.toList());
        
        // OCR 텍스트 추출
        String ocrText = visionService.extractTextFromImage(request.getImageData(), request.isBase64());
        
        if (ocrText == null || ocrText.trim().isEmpty()) {
            throw new RuntimeException("이미지에서 텍스트를 추출할 수 없습니다");
        }
        
        // Python 서비스를 통해 OCR 텍스트 정규화
        List<String> extractedIngredients = pythonApiService.normalizeOcrText(ocrText);
        
        // Python 서비스를 통해 성분 분석
        try {
            Map<String, Object> analysisResult = pythonApiService.analyzeIngredients(extractedIngredients, allergyIngredients);
            
            // Python 서비스 응답을 OcrAnalysisResponse로 변환
            OcrAnalysisResponse response = convertToOcrAnalysisResponse(analysisResult, ocrText, extractedIngredients);
            
            // 분석 결과를 DB에 저장
            OcrIngredient ocrIngredient = OcrIngredient.builder()
                    .user(user)
                    .imageUrl(request.isBase64() ? "base64_data" : request.getImageData())
                    .ocrText(ocrText)
                    .ingredientList(extractedIngredients)
                    .analysisResult(objectMapper.writeValueAsString(response))
                    .build();
            ocrIngredientRepository.save(ocrIngredient);
            
            return response;
        } catch (Exception e) {
            log.error("OCR 분석 중 오류 발생", e);
            throw new RuntimeException("OCR 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    private OcrAnalysisResponse convertToOcrAnalysisResponse(
            Map<String, Object> analysisResult,
            String ocrText,
            List<String> extractedIngredients
    ) {
        OcrAnalysisResponse response = new OcrAnalysisResponse();
        response.setOcrText(ocrText);
        response.setExtractedIngredients(extractedIngredients);
        
        // Python 서비스 응답 파싱
        OcrAnalysisResponse.IngredientAnalysis analysis = new OcrAnalysisResponse.IngredientAnalysis();
        
        if (analysisResult.containsKey("safety_level")) {
            analysis.setSafetyLevel(analysisResult.get("safety_level").toString());
        }
        
        if (analysisResult.containsKey("ingredient_risks")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> risks = (List<Map<String, Object>>) analysisResult.get("ingredient_risks");
            List<OcrAnalysisResponse.IngredientRisk> ingredientRisks = risks.stream()
                    .map(this::convertToIngredientRisk)
                    .collect(Collectors.toList());
            analysis.setIngredientRisks(ingredientRisks);
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
        if (riskMap.containsKey("ingredient_name")) {
            risk.setIngredientName(riskMap.get("ingredient_name").toString());
        }
        if (riskMap.containsKey("content")) {
            risk.setContent(riskMap.get("content").toString());
        }
        if (riskMap.containsKey("allergy_risk")) {
            risk.setAllergyRisk(riskMap.get("allergy_risk").toString());
        }
        if (riskMap.containsKey("risk_level")) {
            risk.setRiskLevel(riskMap.get("risk_level").toString());
        }
        if (riskMap.containsKey("reason")) {
            risk.setReason(riskMap.get("reason").toString());
        }
        return risk;
    }
}

