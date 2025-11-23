package com.SxxM.med.ocr.repository;

import com.SxxM.med.ocr.entity.OcrIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcrIngredientRepository extends JpaRepository<OcrIngredient, Long> {
    List<OcrIngredient> findByUserId(Long userId);
}

