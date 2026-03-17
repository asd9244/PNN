package com.pnn.backend.service;

import com.pnn.backend.client.AiServerClient;
import com.pnn.backend.client.dto.AiRecommendationRequest;
import com.pnn.backend.client.dto.AiRecommendationResponse;
import com.pnn.backend.domain.DrugIngredient;
import com.pnn.backend.domain.DrugsMaster;
import com.pnn.backend.dto.RecommendationRequestDto;
import com.pnn.backend.dto.RecommendationResponseDto;
import com.pnn.backend.repository.DrugIngredientRepository;
import com.pnn.backend.repository.DrugsMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
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
    private final AiServerClient aiServerClient;

    /**
     * 기복용 처방약을 고려한 안전 영양 성분 추천
     * @param request drugIds(기복용 처방약 ID 목록)
     * @return 추천 영양 성분 목록 (이름, 추천 사유). AI 장애 시 빈 목록
     */
    public RecommendationResponseDto recommendSafeNutrients(RecommendationRequestDto request) {
        // 1. DB에서 처방약 목록 조회
        List<DrugsMaster> drugs = drugsMasterRepository.findAllById(request.getDrugIds());
        if (drugs.isEmpty()) {
            return RecommendationResponseDto.builder().recommendedNutrients(Collections.emptyList()).build();
        }

        // 2. 약품명 + 영문 성분 목록을 하나의 리스트로 구성 (AI 입력용)
        List<String> patientDrugs = drugs.stream()
                .flatMap(d -> {
                    List<String> ingredients = drugIngredientRepository.findByItemSeq(d.getItemSeq()).stream()
                            .map(DrugIngredient::getIngrNameEng)
                            .filter(eng -> eng != null && !eng.isBlank())
                            .toList();
                    return Stream.concat(
                            Stream.of(d.getItemName()),
                            ingredients.stream()
                    );
                })
                .distinct()
                .toList();

        // 3. AI 서버 요청 DTO 구성 후 호출
        AiRecommendationRequest aiRequest = AiRecommendationRequest.builder()
                .patientDrugs(patientDrugs)
                .contraindicatedNutrients(List.of())  // LLM이 patient_drugs에서 추론
                .build();

        try {
            AiRecommendationResponse aiResponse = aiServerClient.recommendSafeNutrients(aiRequest);
            return mapToResponse(aiResponse);
        } catch (RestClientException e) {
            // AI 서버 장애 시: 빈 추천 목록 반환
            log.warn("AI 서버 호출 실패: {}", e.getMessage());
            return RecommendationResponseDto.builder()
                    .recommendedNutrients(Collections.emptyList())
                    .build();
        }
    }

    /** AI 서버 응답을 API 응답 DTO로 변환 */
    private RecommendationResponseDto mapToResponse(AiRecommendationResponse ai) {
        if (ai == null || ai.getRecommendedNutrients() == null) {
            return RecommendationResponseDto.builder().recommendedNutrients(List.of()).build();
        }
        List<RecommendationResponseDto.RecommendedNutrient> items = ai.getRecommendedNutrients().stream()
                .map(n -> RecommendationResponseDto.RecommendedNutrient.builder()
                        .name(n.getName())
                        .reasonKr(n.getReasonKr())
                        .build())
                .toList();
        return RecommendationResponseDto.builder().recommendedNutrients(items).build();
    }
}
