package com.pnn.backend.service;

import com.pnn.backend.client.AiServerClient;
import com.pnn.backend.client.dto.AiRecommendationRequest;
import com.pnn.backend.client.dto.AiRecommendationResponse;
import com.pnn.backend.domain.DrugIngredient;
import com.pnn.backend.domain.DrugPermitDetail;
import com.pnn.backend.domain.DrugsMaster;
import com.pnn.backend.dto.RecommendationRequestDto;
import com.pnn.backend.dto.RecommendationResponseDto;
import com.pnn.backend.repository.DrugIngredientRepository;
import com.pnn.backend.repository.DrugPermitDetailRepository;
import com.pnn.backend.repository.DrugsMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Case B: 기복용 처방약 기반 안전 영양제 추천 서비스
 * <p>흐름: drugIds로 DB에서 약품·성분 조회 → Python AI 서버(Gemini) 호출 → 상호작용 적은 영양 성분 추천 반환</p>
 * <p>AI 서버 장애 시 빈 추천 목록 반환</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final DrugsMasterRepository drugsMasterRepository;
    private final DrugIngredientRepository drugIngredientRepository;
    private final DrugPermitDetailRepository drugPermitDetailRepository;
    private final AiServerClient aiServerClient;

    /**
     * 기복용 처방약을 고려한 안전 영양 성분 추천
     * @param request drugIds(기복용 처방약 ID 목록)
     * @return 추천 영양 성분 목록 (이름, 추천 사유). AI 장애 시 빈 목록
     */
    public RecommendationResponseDto recommendSafeNutrients(RecommendationRequestDto request) {
        List<DrugsMaster> drugs = drugsMasterRepository.findAllById(request.getDrugIds());
        DrugIdLookupValidator.assertAllRequestedIdsExist(request.getDrugIds(), drugs);

        List<String> patientDrugs = drugs.stream()
                .flatMap(d -> Stream.concat(
                        Stream.of(d.getItemName()),
                        resolveIngredientNamesForDrug(d).stream()))
                .distinct()
                .toList();

        AiRecommendationRequest aiRequest = AiRecommendationRequest.builder()
                .condition(request.getCondition())
                .patientDrugs(patientDrugs)
                .build();

        try {
            AiRecommendationResponse aiResponse = aiServerClient.recommendSafeNutrients(aiRequest);
            return mapToResponse(aiResponse);
        } catch (RestClientException e) {
            log.warn("AI 서버 호출 실패: {}", e.getMessage());
            return RecommendationResponseDto.builder()
                    .interactionAnalysis("AI 서버 분석 서비스를 일시적으로 사용할 수 없습니다.")
                    .recommendedNutrients(Collections.emptyList())
                    .build();
        }
    }

    /**
     * drug_ingredient 우선, 없으면 drug_permit_detail 영문 성분. 둘 다 없으면 IllegalArgumentException.
     * Case A 상호작용 검사와 동일 규칙.
     */
    private List<String> resolveIngredientNamesForDrug(DrugsMaster drug) {
        List<String> ingredients = drugIngredientRepository.findByItemSeq(drug.getItemSeq()).stream()
                .map(DrugIngredient::getIngrNameEng)
                .filter(eng -> eng != null && !eng.isBlank())
                .toList();

        if (ingredients.isEmpty()) {
            Optional<DrugPermitDetail> permitDetail = drugPermitDetailRepository.findByItemSeq(drug.getItemSeq());
            if (permitDetail.isPresent() && permitDetail.get().getIngrNameEng() != null
                    && !permitDetail.get().getIngrNameEng().isBlank()) {
                return List.of(permitDetail.get().getIngrNameEng());
            }
            throw new IllegalArgumentException(
                    "'" + drug.getItemName() + "' 약품의 성분 데이터가 부족하여 분석을 진행할 수 없습니다.");
        }
        return ingredients;
    }

    private RecommendationResponseDto mapToResponse(AiRecommendationResponse ai) {
        if (ai == null) {
            return RecommendationResponseDto.builder()
                    .recommendedNutrients(List.of())
                    .build();
        }

        List<RecommendationResponseDto.RecommendedNutrient> items =
                ai.getRecommendedNutrients() == null
                        ? List.of()
                        : ai.getRecommendedNutrients().stream()
                                .map(n -> RecommendationResponseDto.RecommendedNutrient.builder()
                                        .nameEn(n.getNameEn())
                                        .nameKr(n.getNameKr())
                                        .reason(n.getReason())
                                        .precaution(n.getPrecaution())
                                        .build())
                                .toList();

        return RecommendationResponseDto.builder()
                .interactionAnalysis(ai.getInteractionAnalysis())
                .recommendedNutrients(items)
                .build();
    }
}
