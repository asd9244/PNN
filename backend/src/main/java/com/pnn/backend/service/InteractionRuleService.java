package com.pnn.backend.service;

import com.pnn.backend.domain.InteractionRule;
import com.pnn.backend.repository.InteractionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * interaction_rules 조회 서비스.
 * drug_ingredient, nutrient는 영문으로 입력 (drugs.main_ingr_eng split, LLM 영문 출력).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionRuleService {

    private final InteractionRuleRepository interactionRuleRepository;

    /**
     * 처방약 성분 + 영양 성분으로 상호작용 룰 조회.
     * 입력은 영문 (drugs.main_ingr_eng split, user_supplements.nutrients).
     */
    public Optional<InteractionRule> findByDrugIngredientAndNutrient(String drugIngredient, String nutrient) {
        return interactionRuleRepository.findByDrugIngredientAndNutrient(drugIngredient, nutrient);
    }

    /**
     * 특정 처방약 성분에 대한 모든 상호작용 룰 조회.
     */
    public List<InteractionRule> findByDrugIngredient(String drugIngredient) {
        return interactionRuleRepository.findByDrugIngredient(drugIngredient);
    }
}
