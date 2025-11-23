package com.SxxM.med.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GptService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${gpt.api.key}")
    private String apiKey;
    
    @Value("${gpt.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${gpt.model:gpt-4}")
    private String model;
    
    private WebClient getWebClient() {
        return webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    public <T> T analyzeWithGpt(String prompt, Class<T> responseClass) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a medical assistant. Always respond in valid JSON format only."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3,
                    "response_format", Map.of("type", "json_object")
            );
            
            String response = getWebClient()
                    .post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null) {
                throw new RuntimeException("GPT API 응답이 null입니다");
            }
            
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("GPT API 응답에 choices가 없습니다");
            }
            
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            return objectMapper.readValue(content, responseClass);
        } catch (Exception e) {
            log.error("GPT API 호출 중 오류 발생", e);
            throw new RuntimeException("GPT 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    public String analyzeWithGptString(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a medical assistant. Provide detailed analysis."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3
            );
            
            String response = getWebClient()
                    .post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null) {
                throw new RuntimeException("GPT API 응답이 null입니다");
            }
            
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("GPT API 응답에 choices가 없습니다");
            }
            
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("GPT API 호출 중 오류 발생", e);
            throw new RuntimeException("GPT 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

