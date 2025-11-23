package com.SxxM.med.controller;

import com.SxxM.med.dto.OcrAnalysisRequest;
import com.SxxM.med.dto.OcrAnalysisResponse;
import com.SxxM.med.dto.SideEffectAnalysisRequest;
import com.SxxM.med.dto.SideEffectAnalysisResponse;
import com.SxxM.med.dto.SymptomAnalysisRequest;
import com.SxxM.med.dto.SymptomAnalysisResponse;
import com.SxxM.med.service.OcrAnalysisService;
import com.SxxM.med.service.SideEffectAnalysisService;
import com.SxxM.med.service.SymptomAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        } catch (Exception e) {
            log.error("부작용 분석 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/ocr")
    @Operation(summary = "OCR 분석", description = "의약품 성분표 이미지를 OCR로 분석하여 성분 리스트 및 안전성을 평가합니다.")
    public ResponseEntity<OcrAnalysisResponse> analyzeOcr(
            @Valid @RequestBody OcrAnalysisRequest request
    ) {
        try {
            OcrAnalysisResponse response = ocrAnalysisService.analyzeOcrImage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OCR 분석 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

