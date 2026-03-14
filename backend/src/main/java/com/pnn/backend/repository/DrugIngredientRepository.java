package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrugIngredientRepository extends JpaRepository<DrugIngredient, Long> {

    List<DrugIngredient> findByItemSeq(String itemSeq);
}
