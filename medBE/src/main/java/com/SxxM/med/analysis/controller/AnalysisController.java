package com.sxxm.med.analysis.controller;

import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.ocr.dto.OcrAnalysisRequest;
import com.sxxm.med.ocr.dto.OcrAnalysisResponse;
import com.sxxm.med.analysis.dto.SideEffectAnalysisRequest;
import com.sxxm.med.analysis.dto.SideEffectAnalysisResponse;
import com.sxxm.med.analysis.dto.SymptomAnalysisRequest;
import com.sxxm.med.analysis.dto.SymptomAnalysisResponse;
import com.sxxm.med.ocr.service.OcrAnalysisService;
import com.sxxm.med.analysis.service.SideEffectAnalysisService;
import com.sxxm.med.analysis.service.SymptomAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analysis", description = "의약품 분석 API")
public class AnalysisController {
    
    private final SymptomAnalysisService symptomAnalysisService;
    private final SideEffectAnalysisService sideEffectAnalysisService;
    private final OcrAnalysisService ocrAnalysisService;
    private final UserRepository userRepository;
    
    @PostMapping("/symptom")
    @Operation(summary = "증상 분석", description = "사용자의 증상을 분석하여 추천 약물 및 주의사항을 제공합니다.")
    public ResponseEntity<SymptomAnalysisResponse> analyzeSymptom(
            @Valid @RequestBody SymptomAnalysisRequest request
    ) {
        try {
            SymptomAnalysisResponse response = symptomAnalysisService.analyzeSymptom(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("증상 분석 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/side-effect")
    @Operation(summary = "부작용 분석", description = "복용 중인 약물들의 부작용을 분석하여 공통 성분 및 위험 패턴을 추출합니다.")
    public ResponseEntity<SideEffectAnalysisResponse> analyzeSideEffect(
            @Valid @RequestBody SideEffectAnalysisRequest request
    ) {
        try {
            SideEffectAnalysisResponse response = sideEffectAnalysisService.analyzeSideEffect(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("부작용 분석 요청 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        } catch (Exception e) {
            log.error("부작용 분석 요청 처리 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/ocr")
    @Operation(summary = "OCR 분석", description = "의약품 성분표 이미지를 OCR로 분석하여 성분 리스트 및 안전성을 평가합니다.")
    public ResponseEntity<OcrAnalysisResponse> analyzeOcr(
            Authentication authentication,
            @Valid @RequestBody OcrAnalysisRequest request
    ) {
        try {
            // JWT에서 사용자 정보 추출 (인증이 있으면 userId 설정, 없으면 null 허용)
            if (authentication != null && authentication.getName() != null) {
                userRepository.findByUsername(authentication.getName())
                        .ifPresentOrElse(
                                user -> {
                                    request.setUserId(user.getId());
                                    log.info("OCR 분석 시작: userId={}, username={}", user.getId(), user.getUsername());
                                },
                                () -> log.warn("사용자를 찾을 수 없습니다: {}, userId 없이 진행", authentication.getName())
                        );
            } else {
                log.info("인증 정보가 없습니다. 비로그인 사용자로 OCR 분석 진행");
            }
            
            OcrAnalysisResponse response = ocrAnalysisService.analyzeOcrImage(request);
            log.info("OCR 분석 완료: userId={}", request.getUserId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("OCR 분석 요청 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("OCR 분석 요청 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

