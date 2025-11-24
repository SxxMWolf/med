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
                            clientResponse -> {
                                log.error("Python API 호출 실패: 상태코드={}, URL={}/ocr/normalize", 
                                        clientResponse.statusCode(), pythonApiUrl);
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("Python API 에러 응답: {}", body);
                                            return Mono.error(new RuntimeException(
                                                    "Python API 호출 실패: " + clientResponse.statusCode() + " - " + body));
                                        });
                            })
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
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            log.error("Python API 연결 실패: URL={}, 오류={}", pythonApiUrl, e.getMessage(), e);
            throw new RuntimeException("Python API 서비스에 연결할 수 없습니다. Python 서비스가 실행 중인지 확인하세요: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout") || 
                e.getCause() instanceof java.util.concurrent.TimeoutException) {
                log.error("Python API 호출 타임아웃: URL={}", pythonApiUrl, e);
                throw new RuntimeException("Python API 호출이 시간 초과되었습니다. Python 서비스가 실행 중인지 확인하세요: " + e.getMessage(), e);
            }
            log.error("OCR 정규화 중 오류 발생: URL={}", pythonApiUrl, e);
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

