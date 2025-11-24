package com.sxxm.med.analysis.repository;

import com.sxxm.med.analysis.entity.SideEffectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SideEffectReportRepository extends JpaRepository<SideEffectReport, Long> {
    List<SideEffectReport> findByUserId(Long userId);
}

