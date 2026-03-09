package com.pnn.backend.service;

import com.pnn.backend.client.PythonAiClient;
import com.pnn.backend.domain.Drug;
import com.pnn.backend.domain.InteractionRule;
import com.pnn.backend.dto.InteractionRequestDto;
import com.pnn.backend.dto.InteractionResponseDto;
import com.pnn.backend.dto.NutrientInputDto;
import com.pnn.backend.dto.SupplementInputDto;
import com.pnn.backend.repository.DrugRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 약물-영양제 간 실제 상호작용(충돌)을 판별하는 핵심 비즈니스 로직 서비스 (Case A)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionCheckService {

    private final DrugRepository drugRepository;
    private final InteractionRuleService interactionRuleService;
    private final PythonAiClient pythonAiClient; // 2차 분석(LLM)에 사용할 클라이언트

    /**
     * 특정 약물(Drug)과 영양제(Supplement)들의 상호작용을 검사합니다.
     */
    public InteractionResponseDto checkInteraction(InteractionRequestDto request) {

        // 1. Drug 정보 조회 및 예외 처리
        // 클라이언트가 보낸 drugId가 DB에 없으면 에러를 발생시킵니다.
        Drug drug = drugRepository.findById(request.getDrugId())
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 처방약을 찾을 수 없습니다. id: " + request.getDrugId()));

        // 2. 주성분(main_ingr_eng) 가져와서 "/" 기준으로 분리하기
        String rawIngredients = drug.getMainIngrEng();
        List<String> drugIngredients = new ArrayList<>();

        if (rawIngredients != null && !rawIngredients.trim().isEmpty()) {
            // 예: "Acetaminophen/Guaifenesin" -> ["Acetaminophen", "Guaifenesin"]
            drugIngredients = Arrays.asList(rawIngredients.split("/"));
        } else {
            // 상세 성분 정보가 없는 약품의 경우 처리 로직 (TODO: 약품명으로 2차 검사 바로 넘기기)
            log.warn("처방약(ID: {})의 영문 주성분(main_ingr_eng) 정보가 없습니다.", drug.getId());
        }

        // 3. Rule-based 1차 필터링 (interaction_rules 매칭)
        // 약성분과 영양제성분간 상호작용 데이터가 있으면 결과를 담을 리스트를 미리 생성해놓음.
        List<InteractionResponseDto.InteractionItem> matchedInteractions = new ArrayList<>();

        for (SupplementInputDto supplement : request.getSupplements()) {
            for (NutrientInputDto nutrient : supplement.getNutrients()) {

                String nutrientName = nutrient.getName(); // 영양제 성분명 (예: "Magnesium")

                // 처방약의 여러 주성분들을 하나씩 돌면서 규칙이 있는지 확인합니다.
                for (String drugIngredient : drugIngredients) {
                    // 영문 성분명 앞뒤 공백 제거 후 검색
                    String cleanDrugIngredient = drugIngredient.trim();

                    Optional<InteractionRule> ruleOpt = interactionRuleService
                            .findByDrugIngredientAndNutrient(cleanDrugIngredient, nutrientName);

                    // 규칙이 존재하면 결과 리스트에 추가합니다.
                    if (ruleOpt.isPresent()) {
                        InteractionRule rule = ruleOpt.get();

                        InteractionResponseDto.InteractionItem item = InteractionResponseDto.InteractionItem.builder()
                                .nutrient(rule.getNutrient())
                                .contraindicatedDrugIngredient(rule.getDrugIngredient())
                                .level(rule.getLevel().name()) // Enum(CAUTION 등)을 문자열로 변환
                                .description(rule.getDescription())
                                .actionGuide(rule.getAction())
                                .build();

                        matchedInteractions.add(item);
                    }
                }
            }
        }

        // 4. 결과 반환 결정 로직
        InteractionResponseDto response = new InteractionResponseDto();

        if (!matchedInteractions.isEmpty()) { // 1차 DB 검사에서 충돌(매칭)이 발견된 경우 즉시 반환 (확정적 데이터)

            response.setSource("RULE");
            response.setSummary("DB 기반 규칙 검사에서 총 " + matchedInteractions.size() + "건의 상호작용이 발견되었습니다.");
            response.setInteractions(matchedInteractions);
            return response;
        }

        // TODO: 5. 매칭되는 규칙이 하나도 없거나, 성분 정보가 없는 경우 Python AI(LLM)에 2차 분석 요청
        log.info("1차 규칙 검사 결과 매칭 없음. Python AI 서버로 2차 분석을 요청할 예정입니다.");

        response.setSource("RULE"); // 임시 (나중에 "LLM"으로 대체)
        response.setSummary("알려진 상호작용이 없습니다. (추후 AI 분석 결과로 대체될 예정)");

        return response;
    }
}