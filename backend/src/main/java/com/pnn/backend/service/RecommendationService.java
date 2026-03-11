package com.pnn.backend.service;

import com.pnn.backend.client.PythonAiClient;
import com.pnn.backend.domain.Drug;
import com.pnn.backend.domain.InteractionRule;
import com.pnn.backend.dto.RecommendationAnalyzeRequestDto;
import com.pnn.backend.dto.RecommendationAnalyzeResponseDto;
import com.pnn.backend.dto.RecommendationRequestDto;
import com.pnn.backend.dto.RecommendationResponseDto;
import com.pnn.backend.repository.DrugRepository;
import com.pnn.backend.repository.InteractionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final DrugRepository drugRepository;
    private final InteractionRuleRepository interactionRuleRepository;
    private final PythonAiClient pythonAiClient;

    /**
     * 기복용 처방약 기반으로 금기 영양 성분을 도출하고,
     * 안전한 영양 성분을 AI에게 추천받는 메인 비즈니스 로직입니다.
     */
    @Transactional(readOnly = true)
    public RecommendationResponseDto recommendSafeNutrients(RecommendationRequestDto request) {

        // 1. 요청받은 약물 ID 목록으로 DB에서 의약품 정보 일괄 조회
        List<Drug> drugs = drugRepository.findByIdInWithIngredients(request.getDrugIds());
        if (drugs.isEmpty()) {
            throw new IllegalArgumentException("유효한 처방약 정보를 찾을 수 없습니다.");
        }

        // 2. 의약품의 주성분(영문) 추출 (DrugIngredient 엔티티 활용)
        List<String> patientDrugs = new ArrayList<>();
        for (Drug drug : drugs) {
            boolean hasEnglishIngredient = false;
            // 1:N 관계인 ingredients 리스트를 순회하여 영문 성분명 추출
            if (drug.getIngredients() != null && !drug.getIngredients().isEmpty()) {
                for (var ingredient : drug.getIngredients()) {
                    if (ingredient.getMainIngrEng() != null && !ingredient.getMainIngrEng().trim().isEmpty()) {
                        patientDrugs.add(ingredient.getMainIngrEng().trim());
                        hasEnglishIngredient = true;
                    }
                }
            }

            // 영문 성분이 하나도 없을 경우에만 약품명을 fallback으로 사용
            if (!hasEnglishIngredient) {
                patientDrugs.add(drug.getItemName());
            }
        }

        // 중복 제거 및 공백 제거
        patientDrugs = patientDrugs.stream()
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        // 3. 추출된 성분들로 금기 규칙 조회 (Rule-based 1차 필터링)
        // DB에 적재된 룰 자체가 위험 조합(CAUTION 등)을 의미하므로, 별도 등급 필터링 없이 모두 경고 대상으로 처리
        List<InteractionRule> rules = interactionRuleRepository.findByDrugIngredientIn(patientDrugs);

        List<RecommendationResponseDto.WarningItem> warnings = new ArrayList<>();
        List<String> contraindicatedNutrients = new ArrayList<>(); // AI에게 전달할 영문 이름 목록

        for (InteractionRule rule : rules) {
            warnings.add(RecommendationResponseDto.WarningItem.builder()
                    .nutrient(rule.getNutrient())
                    .reason(rule.getDescription())
                    .actionGuide(rule.getAction())
                    .build());

            contraindicatedNutrients.add(rule.getNutrient());
        }

        // 금기 영양 성분 이름 중복 제거
        contraindicatedNutrients = contraindicatedNutrients.stream().distinct().collect(Collectors.toList());

        // 4. 금기 성분을 바탕으로 Python AI 서버에 안전 성분 추천 요청 (Client)
        RecommendationAnalyzeRequestDto aiRequest = RecommendationAnalyzeRequestDto.builder()
                .patient_drugs(patientDrugs) // 복용 중인 약 성분 이름(영문)
                .contraindicated_nutrients(contraindicatedNutrients) // 피해야 할 영양 성분 이름(영문)
                .build();

        RecommendationAnalyzeResponseDto aiResponse = pythonAiClient.analyzeSafeNutrients(aiRequest);

        // 5. 추천 결과를 클라이언트 응답 DTO로 변환하고 딥링크 URL 생성
        List<RecommendationResponseDto.RecommendationItem> recommendations = new ArrayList<>();
        if (aiResponse != null && aiResponse.getRecommended_nutrients() != null) {
            for (RecommendationAnalyzeResponseDto.RecommendedNutrient rec : aiResponse.getRecommended_nutrients()) {

                // 검색 URL 동적 생성 (예: 아이허브)
                String encodedKeyword = URLEncoder.encode(rec.getName(), StandardCharsets.UTF_8);
                String purchaseLink = "https://kr.iherb.com/search?kw=" + encodedKeyword;

                recommendations.add(RecommendationResponseDto.RecommendationItem.builder()
                        .nutrient(rec.getName())
                        .reason(rec.getReason_kr())
                        .purchaseLink(purchaseLink)
                        .build());
            }
        }

        // 최종 결과 조립
        return RecommendationResponseDto.builder()
                .warnings(warnings)
                .recommendations(recommendations)
                .build();
    }
}
