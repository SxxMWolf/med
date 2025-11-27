package com.sxxm.med.analysis.service;

import com.sxxm.med.analysis.dto.MedicationInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

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
    
    @PostConstruct
    public void init() {
        // 시스템 환경 변수 직접 확인
        String envApiUrl = System.getenv("MFDS_API_URL");
        String envApiKey = System.getenv("MFDS_API_KEY");
        
        log.info("=== MFDS API 환경 변수 로드 상태 ===");
        log.info("시스템 환경 변수 MFDS_API_URL: {}", envApiUrl != null && !envApiUrl.isEmpty() ? "설정됨 (" + envApiUrl + ")" : "미설정");
        log.info("시스템 환경 변수 MFDS_API_KEY: {}", envApiKey != null && !envApiKey.isEmpty() ? "설정됨 (길이: " + envApiKey.length() + ")" : "미설정");
        log.info("Spring @Value로 주입된 apiUrl: {}", apiUrl != null && !apiUrl.isEmpty() ? "설정됨 (" + apiUrl + ")" : "미설정");
        log.info("Spring @Value로 주입된 apiKey: {}", apiKey != null && !apiKey.isEmpty() ? "설정됨 (길이: " + apiKey.length() + ")" : "미설정");
        
        if ((apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) 
            && (envApiUrl != null && !envApiUrl.isEmpty() && envApiKey != null && !envApiKey.isEmpty())) {
            log.warn("⚠️ 시스템 환경 변수는 설정되어 있지만 Spring이 읽지 못했습니다!");
            log.warn("⚠️ IDE에서 실행하는 경우, 실행 설정(Run Configuration)에서 환경 변수를 명시적으로 추가하세요.");
        } else if (apiUrl != null && !apiUrl.isEmpty() && apiKey != null && !apiKey.isEmpty()) {
            log.info("✅ MFDS API 설정이 정상적으로 로드되었습니다.");
        } else {
            log.warn("⚠️ MFDS API 환경 변수가 설정되지 않았습니다. 의약품 정보 조회가 제한됩니다.");
        }
        log.info("=====================================");
    }
    
    public MedicationInfo getMedicationInfo(String medicationName) {
        // 식품의약품안전처(MFDS) API 호출
        try {
            // 환경 변수 로드 상태 확인 (실제 값 확인용)
            log.info("MFDS API 설정 확인: 약물명={}, apiUrl={}, apiKey 설정 여부={}", 
                    medicationName,
                    apiUrl != null && !apiUrl.isEmpty() ? apiUrl : "미설정",
                    apiKey != null && !apiKey.isEmpty() ? "설정됨 (길이: " + apiKey.length() + ")" : "미설정");
            
            if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
                log.warn("MFDS API 설정이 없어 내부 DB에서 조회합니다: 약물명={}, apiUrl={}, apiKey 설정 여부={}", 
                        medicationName,
                        apiUrl != null && !apiUrl.isEmpty() ? apiUrl : "미설정",
                        apiKey != null && !apiKey.isEmpty() ? "설정됨" : "미설정");
                return getMedicationInfoFromInternalDb(medicationName);
            }
            
            WebClient webClient = webClientBuilder
                    .baseUrl(apiUrl)
                    .build();
            
            // 공공데이터포털 API 형식에 맞춰 요청
            // API 문서: http://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07/getDrugPrdtPrmsnInq07
            // 엔드포인트 경로 추가 및 파라미터 설정
            String encodedName = URLEncoder.encode(medicationName, StandardCharsets.UTF_8);
            String encodedServiceKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            
            String requestUrl = UriComponentsBuilder.fromPath("/getDrugPrdtPrmsnInq07")
                    .queryParam("serviceKey", encodedServiceKey)  // URL 인코딩된 serviceKey
                    .queryParam("item_name", encodedName)  // 의약품명 (품목명)
                    .queryParam("type", "json")  // 응답 형식
                    .queryParam("numOfRows", "10")  // 한 페이지 결과 수
                    .queryParam("pageNo", "1")  // 페이지 번호
                    .build()
                    .toUriString();
            
            // 실제 요청 URL 로깅 (API 키는 마스킹)
            String maskedUrl = requestUrl.replaceAll("serviceKey=[^&]+", "serviceKey=***");
            log.info("MFDS API 호출 시작: 약물명={}, 요청 URL={}", medicationName, maskedUrl);
            String response = webClient
                    .get()
                    .uri(requestUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .block();
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("MFDS API 응답이 비어있습니다: {}", medicationName);
                return getMedicationInfoFromFallback(medicationName, "API 응답이 비어있음");
            }
            
            log.info("MFDS API 응답 수신 완료: 약물명={}, 응답 길이={}", medicationName, response.length());
            JsonNode jsonNode = objectMapper.readTree(response);
            MedicationInfo info = parseMfdsApiResponse(jsonNode, medicationName);
            
            if (info == null || (info.getIngredients().isEmpty() && info.getExcipients().isEmpty())) {
                log.warn("MFDS API에서 정보를 찾을 수 없어 빈 데이터 반환: {}", medicationName);
                return getMedicationInfoFromFallback(medicationName, "API에서 약물 정보를 찾을 수 없음");
            }
            
            return info;
        } catch (WebClientResponseException e) {
            // HTTP 응답 에러 (4xx, 5xx)
            int statusCode = e.getStatusCode().value();
            String errorMessage = e.getMessage();
            String responseBody = e.getResponseBodyAsString();
            
            if (statusCode >= 500) {
                log.error("MFDS API 서버 오류 발생 (HTTP {}): 약물명={}, 에러={}", statusCode, medicationName, errorMessage);
                log.error("API URL: {}", apiUrl);
                if (responseBody != null && !responseBody.isEmpty()) {
                    log.error("응답 본문: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
                }
                log.warn("MFDS API 서버에서 500 에러를 반환했습니다. API 키나 요청 파라미터를 확인하세요.");
            } else if (statusCode == 404) {
                log.warn("MFDS API에서 약물 정보를 찾을 수 없습니다 (HTTP 404): 약물명={}", medicationName);
                if (responseBody != null && !responseBody.isEmpty()) {
                    log.warn("응답 본문: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
                }
            } else if (statusCode == 401 || statusCode == 403) {
                log.error("MFDS API 인증 실패 (HTTP {}): API 키를 확인하세요. 약물명={}", statusCode, medicationName);
                if (responseBody != null && !responseBody.isEmpty()) {
                    log.error("응답 본문: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
                }
            } else {
                log.error("MFDS API 호출 실패 (HTTP {}): 약물명={}, 에러={}", statusCode, medicationName, errorMessage);
                if (responseBody != null && !responseBody.isEmpty()) {
                    log.error("응답 본문: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
                }
            }
            return getMedicationInfoFromFallback(medicationName, "MFDS API 호출 실패 (HTTP " + statusCode + ")");
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            // 네트워크 오류 (연결 실패, 타임아웃 등)
            log.error("MFDS API 호출 중 네트워크 오류 발생: 약물명={}, 에러={}", medicationName, e.getMessage());
            return getMedicationInfoFromFallback(medicationName, "네트워크 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error("MFDS API 조회 중 오류 발생: 약물명={}", medicationName, e);
            return getMedicationInfoFromFallback(medicationName, "예상치 못한 오류: " + e.getMessage());
        }
    }
    
    public List<MedicationInfo> getMedicationInfoList(List<String> medicationNames) {
        return medicationNames.stream()
                .map(this::getMedicationInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * MFDS API가 설정되지 않은 경우 사용 (환경 변수 미설정)
     */
    private MedicationInfo getMedicationInfoFromInternalDb(String medicationName) {
        log.warn("MFDS API가 설정되지 않아 의약품 정보를 조회할 수 없습니다: 약물명={}", medicationName);
        log.warn("환경 변수 MFDS_API_URL과 MFDS_API_KEY를 설정하세요.");
        
        return MedicationInfo.builder()
                .name(medicationName)
                .ingredients(new ArrayList<>())
                .excipients(new ArrayList<>())
                .description("MFDS API가 설정되지 않아 의약품 정보를 조회할 수 없습니다. 환경 변수 MFDS_API_URL과 MFDS_API_KEY를 설정하세요.")
                .build();
    }
    
    /**
     * MFDS API 호출은 성공했지만 실패한 경우 사용 (네트워크 오류, 서버 오류 등)
     */
    private MedicationInfo getMedicationInfoFromFallback(String medicationName, String reason) {
        log.warn("MFDS API 호출 실패로 빈 데이터 반환: 약물명={}, 이유={}", medicationName, reason);
        
        return MedicationInfo.builder()
                .name(medicationName)
                .ingredients(new ArrayList<>())
                .excipients(new ArrayList<>())
                .description("MFDS API 호출 실패: " + reason)
                .build();
    }
    
    /**
     * 식품의약품안전처 API 응답을 MedicationInfo로 파싱
     * 공공데이터포털 API 응답 형식에 맞춰 파싱
     */
    private MedicationInfo parseMfdsApiResponse(JsonNode jsonNode, String medicationName) {
        try {
            // 공공데이터포털 API 응답 형식 확인
            // 형식 1: {"header":{...},"body":{"items":[...]}}
            // 형식 2: {"response":{"body":{"items":[...]}}}
            
            JsonNode body = null;
            JsonNode items = null;
            
            // 형식 1: body가 루트에 있는 경우
            if (jsonNode.has("body")) {
                body = jsonNode.get("body");
                if (body.has("items")) {
                    items = body.get("items");
                    log.info("MFDS API 파싱: body 루트 구조 감지, 약물명={}, items 타입={}, isArray={}, size={}", 
                            medicationName, items.getNodeType(), items.isArray(), 
                            items.isArray() ? items.size() : (items.isObject() ? "객체" : "기타"));
                } else {
                    log.warn("MFDS API 파싱: body에 items 키가 없습니다. 약물명={}, body 키들={}", 
                            medicationName, body.fieldNames().hasNext() ? body.fieldNames().next() : "없음");
                }
            }
            // 형식 2: response.body 구조인 경우
            else if (jsonNode.has("response")) {
                JsonNode response = jsonNode.get("response");
                if (response.has("body")) {
                    body = response.get("body");
                    if (body.has("items")) {
                        items = body.get("items");
                        log.info("MFDS API 파싱: response.body 구조 감지, 약물명={}, items 타입={}, isArray={}, size={}", 
                                medicationName, items.getNodeType(), items.isArray(), 
                                items.isArray() ? items.size() : (items.isObject() ? "객체" : "기타"));
                    }
                }
            } else {
                // 루트 키 목록 수집
                List<String> rootKeys = new ArrayList<>();
                jsonNode.fieldNames().forEachRemaining(rootKeys::add);
                log.warn("MFDS API 파싱: body 또는 response 키를 찾을 수 없습니다. 약물명={}, 루트 키들={}", 
                        medicationName, rootKeys.isEmpty() ? "없음" : String.join(", ", rootKeys));
            }
            
            if (items != null && items.isArray() && items.size() > 0) {
                JsonNode item = items.get(0); // 첫 번째 결과 사용
                log.info("MFDS API 파싱 성공: 약물명={}, 첫 번째 결과 사용, items 개수={}", medicationName, items.size());
                return parseMfdsItem(item, medicationName);
            }
            // items가 객체인 경우
            else if (items != null && items.isObject() && !items.isEmpty()) {
                log.info("MFDS API 파싱 성공: 약물명={}, items 객체 사용", medicationName);
                return parseMfdsItem(items, medicationName);
            }
            // item이 직접 있는 경우
            else if (body != null && body.has("item")) {
                log.info("MFDS API 파싱 성공: 약물명={}, body.item 사용", medicationName);
                return parseMfdsItem(body.path("item"), medicationName);
            }
            
            // 디버깅을 위한 상세 로그
            if (items != null) {
                log.warn("MFDS API 응답 형식을 파싱할 수 없습니다: 약물명={}, items 타입={}, isArray={}, isEmpty={}, size={}", 
                        medicationName, 
                        items.getNodeType().toString(),
                        items.isArray(),
                        items.isEmpty(),
                        items.isArray() ? items.size() : "N/A");
            } else {
                log.warn("MFDS API 응답 형식을 파싱할 수 없습니다: 약물명={}, items가 null입니다. body={}", 
                        medicationName, body != null ? "존재" : "null");
            }
            return null;
        } catch (Exception e) {
            log.error("MFDS API 응답 파싱 중 오류 발생: 약물명={}", medicationName, e);
            return null;
        }
    }
    
    /**
     * MFDS API의 개별 아이템을 파싱
     */
    private MedicationInfo parseMfdsItem(JsonNode item, String medicationName) {
        List<String> ingredients = new ArrayList<>();
        List<String> excipients = new ArrayList<>();
        String name = medicationName;
        String description = "";
        String manufacturer = "";
        
        // 의약품명
        if (item.has("ITEM_NAME") || item.has("itemName") || item.has("item_name")) {
            name = item.path("ITEM_NAME").asText(
                    item.path("itemName").asText(
                            item.path("item_name").asText(medicationName)));
        }
        
        // 제조사
        if (item.has("ENTP_NAME") || item.has("entpName") || item.has("entp_name")) {
            manufacturer = item.path("ENTP_NAME").asText(
                    item.path("entpName").asText(
                            item.path("entp_name").asText("")));
        }
        
        // 주성분 (성분명)
        // DrugPrdtPrmsnInfoService07 API는 ITEM_INGR_NAME 필드를 사용
        String ingredientText = "";
        if (item.has("ITEM_INGR_NAME")) {
            ingredientText = item.path("ITEM_INGR_NAME").asText("");
        } else if (item.has("itemIngrName") || item.has("item_ingr_name")) {
            ingredientText = item.path("itemIngrName").asText(
                    item.path("item_ingr_name").asText(""));
        } else if (item.has("MAIN_ITEM_INGR") || item.has("mainItemIngr") || item.has("main_item_ingr")) {
            ingredientText = item.path("MAIN_ITEM_INGR").asText(
                    item.path("mainItemIngr").asText(
                            item.path("main_item_ingr").asText("")));
        }
        
        if (!ingredientText.isEmpty()) {
            // 성분명을 파싱하여 리스트로 변환
            // ITEM_INGR_NAME은 "/" 또는 "·" 또는 ","로 구분될 수 있음
            // 예: "Amoxicillin Hydrate/Dilute Potassium Clavulanate"
            String[] parts = ingredientText.split("[/·,，\n\r]+");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    ingredients.add(trimmed);
                }
            }
        }
        
        // 부형제 (첨가제)
        if (item.has("ADDITIVE") || item.has("additive") || item.has("ADDITIVE_INGR") || 
            item.has("additiveIngr") || item.has("additive_ingr") || item.has("EXCIPIENT") ||
            item.has("excipient") || item.has("excipients")) {
            String excipientText = "";
            if (item.has("ADDITIVE")) {
                excipientText = item.path("ADDITIVE").asText("");
            } else if (item.has("additive")) {
                excipientText = item.path("additive").asText("");
            } else if (item.has("ADDITIVE_INGR")) {
                excipientText = item.path("ADDITIVE_INGR").asText("");
            } else if (item.has("additiveIngr")) {
                excipientText = item.path("additiveIngr").asText("");
            } else if (item.has("additive_ingr")) {
                excipientText = item.path("additive_ingr").asText("");
            } else if (item.has("EXCIPIENT")) {
                excipientText = item.path("EXCIPIENT").asText("");
            } else if (item.has("excipient")) {
                excipientText = item.path("excipient").asText("");
            } else if (item.has("excipients")) {
                excipientText = item.path("excipients").asText("");
            }
            
            if (!excipientText.isEmpty()) {
                // 부형제를 파싱하여 리스트로 변환
                String[] parts = excipientText.split("[,，\n\r]+");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        excipients.add(trimmed);
                    }
                }
            }
        }
        
        // 설명 (효능효과)
        if (item.has("EE_DOC_DATA") || item.has("eeDocData") || item.has("ee_doc_data") ||
            item.has("EFFECT") || item.has("effect")) {
            description = item.path("EE_DOC_DATA").asText(
                    item.path("eeDocData").asText(
                            item.path("ee_doc_data").asText(
                                    item.path("EFFECT").asText(
                                            item.path("effect").asText("")))));
        }
        
        return MedicationInfo.builder()
                .name(name)
                .ingredients(ingredients)
                .excipients(excipients)
                .description(description)
                .manufacturer(manufacturer)
                .build();
    }
}

