package com.sxxm.med.analysis.controller;

import com.sxxm.med.analysis.dto.MedicationInfo;
import com.sxxm.med.analysis.service.MedicationDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Medications", description = "의약품 검색 API")
public class MedicationController {
    
    private final MedicationDbService medicationDbService;
    
    @GetMapping("/search")
    @Operation(summary = "약 검색", description = "약물명으로 의약품 정보를 검색합니다.")
    public ResponseEntity<MedicationInfo> searchMedication(@RequestParam String name) {
        try {
            MedicationInfo medication = medicationDbService.getMedicationInfo(name);
            return ResponseEntity.ok(medication);
        } catch (Exception e) {
            log.error("약 검색 실패: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/search/batch")
    @Operation(summary = "약 일괄 검색", description = "여러 약물명으로 의약품 정보를 일괄 검색합니다.")
    public ResponseEntity<List<MedicationInfo>> searchMedications(@RequestBody List<String> medicationNames) {
        try {
            List<MedicationInfo> medications = medicationDbService.getMedicationInfoList(medicationNames);
            return ResponseEntity.ok(medications);
        } catch (Exception e) {
            log.error("약 일괄 검색 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

