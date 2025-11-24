package com.sxxm.med.ocr.repository;

import com.sxxm.med.ocr.entity.OcrIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcrIngredientRepository extends JpaRepository<OcrIngredient, Long> {
    List<OcrIngredient> findByUserId(Long userId);
}

