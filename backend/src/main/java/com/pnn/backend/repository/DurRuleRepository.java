package com.pnn.backend.repository;

import com.pnn.backend.domain.DurgRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DurRuleRepository extends JpaRepository<DurgRule, Long> {

    /**
     * drug_ingredient 또는 nutrient가 dur_ingr_name_eng, contraind_dur_ingr_name_eng에 매칭되는 규칙 조회
     * (drug_ingredient, nutrient) 쌍 매칭: 한쪽이 drug, 다른쪽이 nutrient인 경우
     */
    @Query("SELECT d FROM DurgRule d WHERE " +
            "(LOWER(TRIM(d.durIngrNameEng)) = LOWER(:drugIngredient) AND LOWER(TRIM(d.contraindDurIngrNameEng)) = LOWER(:nutrient)) OR " +
            "(LOWER(TRIM(d.durIngrNameEng)) = LOWER(:nutrient) AND LOWER(TRIM(d.contraindDurIngrNameEng)) = LOWER(:drugIngredient))")
    List<DurgRule> findByDrugIngredientAndNutrient(@Param("drugIngredient") String drugIngredient, @Param("nutrient") String nutrient);

    /**
     * drug_ingredient가 dur_ingr_name_eng 또는 contraind_dur_ingr_name_eng에 포함되는 규칙 조회 (Case B용)
     */
    @Query("SELECT d FROM DurgRule d WHERE " +
            "LOWER(TRIM(d.durIngrNameEng)) = LOWER(:drugIngredient) OR " +
            "LOWER(TRIM(d.contraindDurIngrNameEng)) = LOWER(:drugIngredient)")
    List<DurgRule> findByDrugIngredient(@Param("drugIngredient") String drugIngredient);

    @Query("SELECT d FROM DurgRule d WHERE " +
            "LOWER(TRIM(d.durIngrNameEng)) IN :drugIngredients OR " +
            "LOWER(TRIM(d.contraindDurIngrNameEng)) IN :drugIngredients")
    List<DurgRule> findByDrugIngredientIn(@Param("drugIngredients") List<String> drugIngredients);
}
