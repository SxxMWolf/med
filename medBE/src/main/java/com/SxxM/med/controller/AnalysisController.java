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
public class AnalysisController {
    
    private final SymptomAnalysisService symptomAnalysisService;
    private final SideEffectAnalysisService sideEffectAnalysisService;
    private final OcrAnalysisService ocrAnalysisService;
    
    @PostMapping("/symptom")
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

