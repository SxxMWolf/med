package com.sxxm.med.ocr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcrAnalysisRequest {
    
    private Long userId; // JWT에서 자동 설정되므로 validation 제거
    
    @NotBlank(message = "이미지 URL 또는 Base64 데이터는 필수입니다")
    private String imageData; // URL 또는 Base64
    
    private boolean isBase64;
}

