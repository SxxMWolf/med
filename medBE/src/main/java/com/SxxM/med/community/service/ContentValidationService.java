package com.SxxM.med.community.service;

import com.SxxM.med.analysis.service.GptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentValidationService {
    
    private final GptService gptService;
    
    @Value("${content.validation.enabled:false}")
    private boolean validationEnabled;
    
    public boolean validateContent(String content) {
        if (!validationEnabled) {
            return true; // 검증이 비활성화된 경우 항상 통과
        }
        
        try {
            String prompt = String.format("""
                    다음 텍스트가 부적절한 내용(욕설, 스팸, 혐오 표현 등)을 포함하고 있는지 검사해주세요.
                    부적절한 내용이 있으면 "REJECT", 적절한 내용이면 "APPROVE"만 응답해주세요.
                    
                    텍스트: %s
                    
                    응답 형식: JSON
                    {"result": "APPROVE" 또는 "REJECT", "reason": "이유"}
                    """, content);
            
            String response = gptService.analyzeWithGptString(prompt);
            
            // 간단한 검증 로직 (실제로는 더 정교한 파싱 필요)
            return !response.contains("REJECT");
        } catch (Exception e) {
            log.error("콘텐츠 검증 중 오류 발생", e);
            // 검증 실패 시 기본적으로 통과 (서비스 중단 방지)
            return true;
        }
    }
}

