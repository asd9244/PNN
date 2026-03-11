package com.pnn.backend.repository;

import com.pnn.backend.domain.InteractionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InteractionRuleRepository extends JpaRepository<InteractionRule, Long> {

    // 처방약 성분과 영양 성분으로 상호작용 룰 조회 (1차 필터 핵심)
    Optional<InteractionRule> findByDrugIngredientAndNutrient(String drugIngredient, String nutrient);

    // 특정 처방약 성분에 대한 모든 상호작용 룰 조회
    List<InteractionRule> findByDrugIngredient(String drugIngredient);

    // 복수 처방약 성분에 대한 모든 상호작용 룰 조회 (Case B 금기 성분 추출용)
    List<InteractionRule> findByDrugIngredientIn(List<String> drugIngredients);
}
