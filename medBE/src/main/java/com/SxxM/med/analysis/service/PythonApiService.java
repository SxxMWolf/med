package com.sxxm.med.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
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
     * Python API 호출 시 공통 에러 처리
     */
    private RuntimeException handleApiException(Exception e, String operation) {
        if (e instanceof org.springframework.web.reactive.function.client.WebClientException) {
            log.error("Python API 연결 실패: URL={}, 작업={}, 오류={}", pythonApiUrl, operation, e.getMessage(), e);
            return new RuntimeException("Python API 서비스에 연결할 수 없습니다. Python 서비스가 실행 중인지 확인하세요: " + e.getMessage(), e);
        }
        if (e.getMessage() != null && (e.getMessage().contains("timeout") || 
            e.getCause() instanceof java.util.concurrent.TimeoutException)) {
            log.error("Python API 호출 타임아웃: URL={}, 작업={}", pythonApiUrl, operation, e);
            return new RuntimeException("Python API 호출이 시간 초과되었습니다. Python 서비스가 실행 중인지 확인하세요: " + e.getMessage(), e);
        }
        log.error("Python API 작업 중 오류 발생: 작업={}, URL={}", operation, pythonApiUrl, e);
        return new RuntimeException(operation + " 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
    
    /**
     * Python API 호출 시 공통 에러 핸들러 생성
     */
    private java.util.function.Function<org.springframework.web.reactive.function.client.ClientResponse, Mono<? extends Throwable>> createErrorHandler(String endpoint) {
        return clientResponse -> {
            log.error("Python API 호출 실패: 상태코드={}, URL={}/{}", 
                    clientResponse.statusCode(), pythonApiUrl, endpoint);
            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Python API 에러 응답: {}", body);
                        return Mono.error(new RuntimeException(
                                "Python API 호출 실패: " + clientResponse.statusCode() + " - " + body));
                    });
        };
    }
    
    /**
     * OCR 텍스트 정규화 (정리된 텍스트 포함)
     * 반환: Map with "normalized_ingredients" and "cleaned_text"
     */
    public Map<String, Object> normalizeOcrText(String ocrText) {
        try {
            log.info("Python API 호출 시작: /ocr/normalize, Python API URL: {}", pythonApiUrl);
            Map<String, Object> request = Map.of("ocr_text", ocrText);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/ocr/normalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            createErrorHandler("ocr/normalize"))
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .doOnError(error -> log.error("Python API 호출 중 예외 발생", error))
                    .block();
            
            if (response == null || !response.containsKey("normalized_ingredients")) {
                log.error("Python API 응답이 올바르지 않습니다: response={}", response);
                throw new RuntimeException("Python API 응답이 올바르지 않습니다. normalized_ingredients 필드가 없습니다.");
            }
            
            @SuppressWarnings("unchecked")
            List<String> ingredients = (List<String>) response.get("normalized_ingredients");
            String cleanedText = response.containsKey("cleaned_text") ? 
                    response.get("cleaned_text").toString() : ocrText;
            
            log.info("Python API 호출 성공: 성분 개수={}, 정리된 텍스트 길이={}", 
                    ingredients != null ? ingredients.size() : 0, 
                    cleanedText != null ? cleanedText.length() : 0);
            
            Map<String, Object> result = new HashMap<>();
            result.put("normalized_ingredients", ingredients != null ? ingredients : List.of());
            result.put("cleaned_text", cleanedText != null ? cleanedText : ocrText);
            return result;
        } catch (Exception e) {
            throw handleApiException(e, "OCR 정규화");
        }
    }
    
    /**
     * 성분 분석 (알러지 비교 포함)
     */
    public Map<String, Object> analyzeIngredients(
            List<String> ingredients, 
            List<String> allergyIngredients,
            List<String> medicationAllergies,
            List<String> foodAllergies
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("ingredients", ingredients);
            request.put("allergy_ingredients", allergyIngredients != null ? allergyIngredients : List.of());
            request.put("medication_allergies", medicationAllergies != null ? medicationAllergies : List.of());
            request.put("food_allergies", foodAllergies != null ? foodAllergies : List.of());
            
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
     * 부작용 분석 (그룹 기반)
     * 
     * @param medicationNames 그룹 이름 목록 (예: ["두유", "A, B, C, D", "E, F, G"])
     * @param medicationIngredients 각 그룹별 성분 리스트 (그룹 내 약물들의 합집합)
     * @param allergyIngredients 하위 호환성을 위한 기존 알러지 성분 목록
     * @param description 부작용 설명
     * @param medicationAllergies 약물 알러지 목록
     * @param foodAllergies 식품 알러지 목록
     * @return 분석 결과
     */
    public Map<String, Object> analyzeSideEffects(
            List<String> medicationNames,
            List<List<String>> medicationIngredients,
            List<String> allergyIngredients,
            String description,
            List<String> medicationAllergies,
            List<String> foodAllergies
    ) {
        try {
            log.info("Python API 호출 시작: /analyze/sideeffects, Python API URL: {}", pythonApiUrl);
            Map<String, Object> request = new HashMap<>();
            request.put("medication_names", medicationNames);
            request.put("medication_ingredients", medicationIngredients);
            request.put("allergy_ingredients", allergyIngredients != null ? allergyIngredients : List.of());
            request.put("description", description != null ? description : "");
            request.put("medication_allergies", medicationAllergies != null ? medicationAllergies : List.of());
            request.put("food_allergies", foodAllergies != null ? foodAllergies : List.of());
            
            log.debug("Python API 요청 데이터: medication_names={}, groups={}", 
                    medicationNames.size(), medicationIngredients.size());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/analyze/sideeffects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            createErrorHandler("analyze/sideeffects"))
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .doOnError(error -> log.error("Python API 호출 중 예외 발생", error))
                    .block();
            
            if (response == null) {
                log.error("Python API 응답이 null입니다");
                throw new RuntimeException("Python API 응답이 null입니다");
            }
            
            log.info("Python API 호출 성공: 응답 키 개수={}", response.size());
            return response;
        } catch (Exception e) {
            throw handleApiException(e, "부작용 분석");
        }
    }
    
    /**
     * 식품 성분 추론 (GPT 기반)
     * 
     * @param foodNames 식품명 목록
     * @return 각 식품별 성분 리스트 (Map<String, List<String>>)
     */
    public Map<String, List<String>> inferFoodIngredients(List<String> foodNames) {
        try {
            log.info("Python API 호출 시작: /analyze/food-ingredients, Python API URL: {}", pythonApiUrl);
            Map<String, Object> request = new HashMap<>();
            request.put("food_names", foodNames);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = getWebClient()
                    .post()
                    .uri("/analyze/food-ingredients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            createErrorHandler("analyze/food-ingredients"))
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .doOnError(error -> log.error("Python API 호출 중 예외 발생", error))
                    .block();
            
            if (response == null) {
                log.error("Python API 응답이 null입니다");
                throw new RuntimeException("Python API 응답이 null입니다");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, List<String>> foodIngredients = (Map<String, List<String>>) response.get("food_ingredients");
            
            if (foodIngredients == null) {
                log.warn("Python API 응답에 food_ingredients가 없습니다. 빈 맵 반환");
                return new HashMap<>();
            }
            
            log.info("Python API 호출 성공: 식품 개수={}, 추론된 성분 맵 크기={}", 
                    foodNames.size(), foodIngredients.size());
            return foodIngredients;
        } catch (Exception e) {
            throw handleApiException(e, "식품 성분 추론");
        }
    }
}

