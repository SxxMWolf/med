package com.SxxM.med.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonApiService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${python.api.url:http://localhost:8000}")
    private String pythonApiUrl;
    
    private WebClient getWebClient() {
        return webClientBuilder
                .baseUrl(pythonApiUrl)
                .build();
    }
    
    /**
     * OCR 텍스트 정규화
     */
    public List<String> normalizeOcrText(String ocrText) {
        try {
            Map<String, Object> request = Map.of("ocr_text", ocrText);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/ocr/normalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            if (response == null || !response.containsKey("normalized_ingredients")) {
                throw new RuntimeException("Python API 응답이 올바르지 않습니다");
            }
            
            @SuppressWarnings("unchecked")
            List<String> ingredients = (List<String>) response.get("normalized_ingredients");
            return ingredients;
        } catch (Exception e) {
            log.error("OCR 정규화 중 오류 발생", e);
            throw new RuntimeException("OCR 정규화 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 성분 분석 (알러지 비교 포함)
     */
    public Map<String, Object> analyzeIngredients(List<String> ingredients, List<String> allergyIngredients) {
        try {
            Map<String, Object> request = Map.of(
                    "ingredients", ingredients,
                    "allergy_ingredients", allergyIngredients
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/analyze/ingredients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            
            if (response == null) {
                throw new RuntimeException("Python API 응답이 null입니다");
            }
            
            return response;
        } catch (Exception e) {
            log.error("성분 분석 중 오류 발생", e);
            throw new RuntimeException("성분 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 부작용 분석
     */
    public Map<String, Object> analyzeSideEffects(
            List<String> medicationNames,
            List<List<String>> medicationIngredients,
            List<String> allergyIngredients,
            String description
    ) {
        try {
            Map<String, Object> request = Map.of(
                    "medication_names", medicationNames,
                    "medication_ingredients", medicationIngredients,
                    "allergy_ingredients", allergyIngredients != null ? allergyIngredients : List.of(),
                    "description", description != null ? description : ""
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/analyze/sideeffects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            
            if (response == null) {
                throw new RuntimeException("Python API 응답이 null입니다");
            }
            
            return response;
        } catch (Exception e) {
            log.error("부작용 분석 중 오류 발생", e);
            throw new RuntimeException("부작용 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

