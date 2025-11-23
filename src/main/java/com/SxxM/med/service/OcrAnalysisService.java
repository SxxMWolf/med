package com.SxxM.med.service;

import com.SxxM.med.dto.OcrAnalysisRequest;
import com.SxxM.med.dto.OcrAnalysisResponse;
import com.SxxM.med.entity.OcrIngredient;
import com.SxxM.med.entity.User;
import com.SxxM.med.entity.UserAllergy;
import com.SxxM.med.repository.OcrIngredientRepository;
import com.SxxM.med.repository.UserAllergyRepository;
import com.SxxM.med.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final GptService gptService;
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
        
        // 성분 리스트 파싱
        List<String> extractedIngredients = parseIngredientsFromOcrText(ocrText);
        
        // GPT 프롬프트 생성
        String prompt = buildOcrAnalysisPrompt(extractedIngredients, allergyIngredients, ocrText);
        
        // GPT 분석 요청
        try {
            OcrAnalysisResponse response = gptService.analyzeWithGpt(prompt, OcrAnalysisResponse.class);
            response.setOcrText(ocrText);
            response.setExtractedIngredients(extractedIngredients);
            
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
    
    private List<String> parseIngredientsFromOcrText(String ocrText) {
        // GPT를 사용하여 성분 파싱
        try {
            String prompt = String.format("""
                    다음은 의약품 성분표의 OCR 텍스트입니다. 
                    이 텍스트에서 의약품 성분명만 추출하여 JSON 배열 형태로 반환해주세요.
                    각 성분은 순수한 성분명만 포함해야 하며, 함량 정보는 제외해주세요.
                    
                    OCR 텍스트:
                    %s
                    
                    JSON 형식: {"ingredients": ["성분1", "성분2", ...]}
                    """, ocrText);
            
            String response = gptService.analyzeWithGptString(prompt);
            
            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.has("ingredients")) {
                return objectMapper.convertValue(jsonNode.get("ingredients"), new TypeReference<List<String>>() {});
            }
            
            // JSON 형식이 아닌 경우 직접 파싱 시도
            return parseIngredientsFromText(ocrText);
        } catch (Exception e) {
            log.warn("GPT를 통한 성분 파싱 실패, 기본 파싱 사용", e);
            return parseIngredientsFromText(ocrText);
        }
    }
    
    private List<String> parseIngredientsFromText(String ocrText) {
        // 기본 파싱 로직 (정규식 기반)
        // 실제 구현은 OCR 텍스트 형식에 따라 달라질 수 있습니다
        return List.of(ocrText.split("[,\\n]"))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    private String buildOcrAnalysisPrompt(
            List<String> extractedIngredients,
            List<String> allergyIngredients,
            String ocrText
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("의약품 성분표에서 추출한 성분 목록:\n");
        extractedIngredients.forEach(ingredient -> 
            prompt.append("- ").append(ingredient).append("\n")
        );
        prompt.append("\n");
        
        if (!allergyIngredients.isEmpty()) {
            prompt.append("사용자의 알러지 성분 목록:\n");
            allergyIngredients.forEach(ingredient -> 
                prompt.append("- ").append(ingredient).append("\n")
            );
            prompt.append("\n");
        }
        
        prompt.append("원본 OCR 텍스트:\n").append(ocrText).append("\n\n");
        
        prompt.append("""
                다음 정보를 포함하여 JSON 형식으로 응답해주세요:
                1. 각 성분의 위험도 분석 (ingredientRisks): 성분명, 함량 정보(가능한 경우), 알러지 위험도, 위험 수준, 이유
                2. 예상 부작용 (expectedSideEffects): 이 약물을 복용할 때 예상되는 부작용 목록
                3. 전체 안전성 평가 (overallAssessment): 이 약물의 전체적인 안전성에 대한 평가
                4. 복용 안전성 수준 (safetyLevel): SAFE, CAUTION, DANGEROUS 중 하나
                5. 권장 사항 (recommendations): 복용 전 주의사항 및 권장사항
                
                JSON 형식:
                {
                  "safetyLevel": "SAFE|CAUTION|DANGEROUS",
                  "ingredientRisks": [
                    {
                      "ingredientName": "성분명",
                      "content": "함량 정보",
                      "allergyRisk": "위험도 설명",
                      "riskLevel": "LOW|MEDIUM|HIGH",
                      "reason": "이유"
                    }
                  ],
                  "expectedSideEffects": ["부작용1", "부작용2"],
                  "overallAssessment": "전체 평가",
                  "recommendations": ["권장사항1", "권장사항2"]
                }
                """);
        
        return prompt.toString();
    }
}

