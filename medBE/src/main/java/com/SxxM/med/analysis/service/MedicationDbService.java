package com.SxxM.med.analysis.service;

import com.SxxM.med.analysis.dto.MedicationInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationDbService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${medication.db.api.url:}")
    private String apiUrl;
    
    @Value("${medication.db.api.key:}")
    private String apiKey;
    
    public MedicationInfo getMedicationInfo(String medicationName) {
        // 외부 의약품 DB API 호출
        // 실제 API 엔드포인트와 형식에 맞게 수정 필요
        try {
            if (apiUrl == null || apiUrl.isEmpty()) {
                // API가 설정되지 않은 경우 내부 데이터셋에서 조회
                return getMedicationInfoFromInternalDb(medicationName);
            }
            
            WebClient webClient = webClientBuilder
                    .baseUrl(apiUrl)
                    .build();
            
            String response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/medication")
                            .queryParam("name", medicationName)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null) {
                return getMedicationInfoFromInternalDb(medicationName);
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            return parseMedicationInfo(jsonNode);
        } catch (Exception e) {
            log.error("외부 의약품 DB 조회 중 오류 발생: {}", medicationName, e);
            return getMedicationInfoFromInternalDb(medicationName);
        }
    }
    
    public List<MedicationInfo> getMedicationInfoList(List<String> medicationNames) {
        return medicationNames.stream()
                .map(this::getMedicationInfo)
                .collect(Collectors.toList());
    }
    
    private MedicationInfo getMedicationInfoFromInternalDb(String medicationName) {
        // 내부 데이터셋에서 조회하는 로직
        // 실제로는 별도의 내부 DB나 파일에서 조회
        log.warn("내부 DB에서 의약품 정보를 조회합니다: {}", medicationName);
        
        // 예시: 간단한 매핑 테이블이나 파일 기반 조회
        // 실제 구현은 데이터 소스에 따라 달라짐
        return MedicationInfo.builder()
                .name(medicationName)
                .ingredients(new ArrayList<>())
                .description("의약품 정보를 찾을 수 없습니다")
                .build();
    }
    
    private MedicationInfo parseMedicationInfo(JsonNode jsonNode) {
        // 외부 API 응답을 MedicationInfo로 파싱
        // 실제 API 응답 형식에 맞게 수정 필요
        List<String> ingredients = new ArrayList<>();
        if (jsonNode.has("ingredients")) {
            jsonNode.get("ingredients").forEach(ingredient -> {
                ingredients.add(ingredient.asText());
            });
        }
        
        return MedicationInfo.builder()
                .name(jsonNode.has("name") ? jsonNode.get("name").asText() : "")
                .ingredients(ingredients)
                .description(jsonNode.has("description") ? jsonNode.get("description").asText() : "")
                .manufacturer(jsonNode.has("manufacturer") ? jsonNode.get("manufacturer").asText() : "")
                .build();
    }
}

