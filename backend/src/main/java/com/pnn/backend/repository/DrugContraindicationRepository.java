package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugContraindication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrugContraindicationRepository extends JpaRepository<DrugContraindication, Long> {

    /**
     * (drug_ingredient, nutrient) 쌍이 (ingr_name_1, ingr_name_2) 또는 (ingr_name_2, ingr_name_1)와 일치하는 행 조회
     */
    @Query("SELECT dc FROM DrugContraindication dc WHERE " +
            "(LOWER(TRIM(dc.ingrName1)) = LOWER(:drugIngredient) AND LOWER(TRIM(dc.ingrName2)) = LOWER(:nutrient)) OR " +
            "(LOWER(TRIM(dc.ingrName1)) = LOWER(:nutrient) AND LOWER(TRIM(dc.ingrName2)) = LOWER(:drugIngredient))")
    List<DrugContraindication> findByDrugIngredientAndNutrient(@Param("drugIngredient") String drugIngredient, @Param("nutrient") String nutrient);

    /**
     * drug_ingredient가 ingr_name_1 또는 ingr_name_2에 포함되는 행 조회 (Case B용)
     */
    @Query("SELECT dc FROM DrugContraindication dc WHERE " +
            "LOWER(TRIM(dc.ingrName1)) = LOWER(:drugIngredient) OR " +
            "LOWER(TRIM(dc.ingrName2)) = LOWER(:drugIngredient)")
    List<DrugContraindication> findByDrugIngredient(@Param("drugIngredient") String drugIngredient);

    @Query("SELECT dc FROM DrugContraindication dc WHERE " +
            "LOWER(TRIM(dc.ingrName1)) IN :drugIngredients OR " +
            "LOWER(TRIM(dc.ingrName2)) IN :drugIngredients")
    List<DrugContraindication> findByDrugIngredientIn(@Param("drugIngredients") List<String> drugIngredients);
}
