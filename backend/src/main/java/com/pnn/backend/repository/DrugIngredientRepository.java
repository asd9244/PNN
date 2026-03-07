package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugIngredientRepository extends JpaRepository<DrugIngredient, Long> {

    boolean existsByDrugIdAndMtralCode(Long drugId, String mtralCode); // 중복 적재 방지용
}
