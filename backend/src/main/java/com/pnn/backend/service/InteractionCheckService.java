package com.pnn.backend.service;

import com.pnn.backend.client.AiServerClient;
import com.pnn.backend.client.dto.AiInteractionRequest;
import com.pnn.backend.client.dto.AiInteractionResponse;
import com.pnn.backend.domain.DrugIngredient;
import com.pnn.backend.domain.DrugsMaster;
import com.pnn.backend.dto.InteractionCheckRequestDto;
import com.pnn.backend.dto.InteractionCheckResponseDto;
import com.pnn.backend.repository.DrugIngredientRepository;
import com.pnn.backend.repository.DrugsMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

/**
 * Case A: 신규 처방약 ↔ 기복용 영양제 충돌 검사 서비스
 * <p>흐름: drugId로 DB에서 약품·성분 조회 → Python AI 서버(Gemini) 호출 → 상호작용 등급·설명·행동 가이드 반환</p>
 * <p>AI 서버 장애 시 CAUTION 등급의 fallback 메시지 반환 (의사/약사 상담 권장)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionCheckService {

    private final DrugsMasterRepository drugsMasterRepository;
    private final DrugIngredientRepository drugIngredientRepository;
    private final AiServerClient aiServerClient;

    /**
     * 처방약 + 영양제 상호작용 검사
     * @param request drugId(필수), supplements(기복용 영양제 목록)
     * @return 상호작용 목록 (등급, 설명, 행동 가이드). AI 장애 시 fallback 메시지
     */
    public InteractionCheckResponseDto checkInteraction(InteractionCheckRequestDto request) {
        // 1. DB에서 처방약 정보 조회
        DrugsMaster drug = drugsMasterRepository.findById(request.getDrugId())
                .orElseThrow(() -> new IllegalArgumentException("약품을 찾을 수 없습니다. drugId=" + request.getDrugId()));

        // 2. 처방약 영문 성분 목록 추출 (AI 분석용)
        List<String> ingredients = drugIngredientRepository.findByItemSeq(drug.getItemSeq()).stream()
                .map(DrugIngredient::getIngrNameEng)
                .filter(eng -> eng != null && !eng.isBlank())
                .distinct()
                .toList();

        // 3. AI 서버 요청 DTO 구성 후 호출
        AiInteractionRequest aiRequest = AiInteractionRequest.builder()
                .drug(AiInteractionRequest.DrugInput.builder()
                        .id(String.valueOf(drug.getId()))
                        .name(drug.getItemName())
                        .ingredients(ingredients)
                        .build())
                .supplements(mapSupplements(request.getSupplements()))
                .build();

        try {
            AiInteractionResponse aiResponse = aiServerClient.analyzeInteraction(aiRequest);
            return mapToResponse(aiResponse);
        } catch (RestClientException e) {
            // AI 서버 장애 시: 의사/약사 상담 권장 메시지로 fallback
            log.warn("AI 서버 호출 실패. fallback 메시지 반환: {}", e.getMessage());
            return InteractionCheckResponseDto.builder()
                    .interactions(List.of(InteractionCheckResponseDto.InteractionItem.builder()
                            .nutrient("알 수 없음")
                            .contraindicatedDrugIngredient("알 수 없음")
                            .level("CAUTION")
                            .description("AI 분석 서비스를 일시적으로 사용할 수 없습니다.")
                            .actionGuide("영양제 복용 전 반드시 의사 또는 약사와 상담하시기 바랍니다.")
                            .sources(Collections.emptyList())
                            .build()))
                    .build();
        }
    }

    /** API 요청 DTO의 supplements를 AI 서버 요청 형식으로 변환 */
    private List<AiInteractionRequest.SupplementInput> mapSupplements(
            List<InteractionCheckRequestDto.SupplementInput> supplements) {
        if (supplements == null) return List.of();
        return supplements.stream()
                .map(s -> AiInteractionRequest.SupplementInput.builder()
                        .name(s.getName())
                        .nutrients(s.getNutrients() != null ? s.getNutrients().stream()
                                .map(n -> AiInteractionRequest.NutrientInput.builder()
                                        .name(n.getName())
                                        .amount(n.getAmount())
                                        .unit(n.getUnit())
                                        .build())
                                .toList() : List.of())
                        .build())
                .toList();
    }

    /** AI 서버 응답을 API 응답 DTO로 변환 */
    private InteractionCheckResponseDto mapToResponse(AiInteractionResponse ai) {
        if (ai == null || ai.getInteractions() == null) {
            return InteractionCheckResponseDto.builder().interactions(List.of()).build();
        }
        List<InteractionCheckResponseDto.InteractionItem> items = ai.getInteractions().stream()
                .map(i -> InteractionCheckResponseDto.InteractionItem.builder()
                        .nutrient(i.getNutrient())
                        .contraindicatedDrugIngredient(i.getContraindicatedDrugIngredient())
                        .level(i.getLevel())
                        .description(i.getDescription())
                        .actionGuide(i.getActionGuide())
                        .sources(i.getSources() != null ? i.getSources() : List.of())
                        .build())
                .toList();
        return InteractionCheckResponseDto.builder().interactions(items).build();
    }
}
