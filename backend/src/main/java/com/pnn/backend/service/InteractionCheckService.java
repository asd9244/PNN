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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Case A: 처방약 목록 ↔ 기복용 영양제 목록 충돌 검사 서비스
 * <p>흐름: drugIds로 DB에서 약품·성분 조회 → 약품별로 Python AI 서버(Gemini) 비동기 병렬 호출 → 결과 취합 반환</p>
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
     * 여러 처방약 + 영양제 상호작용 병렬 검사
     * @param request drugIds(필수), supplements(기복용 영양제 목록)
     */
    public InteractionCheckResponseDto checkInteraction(InteractionCheckRequestDto request) {
        if (request.getDrugIds() == null || request.getDrugIds().isEmpty()) {
            throw new IllegalArgumentException("drugIds 목록이 비어있습니다.");
        }

        List<DrugsMaster> drugs = drugsMasterRepository.findAllById(request.getDrugIds());
        if (drugs.isEmpty()) {
            throw new IllegalArgumentException("요청한 처방약을 DB에서 찾을 수 없습니다.");
        }

        List<AiInteractionRequest.SupplementInput> mappedSupplements = mapSupplements(request.getSupplements());

        // 각 약품에 대해 비동기 병렬 호출 세팅
        List<CompletableFuture<List<InteractionCheckResponseDto.InteractionItem>>> futures = drugs.stream()
                .map(drug -> CompletableFuture.supplyAsync(() -> processSingleDrug(drug, mappedSupplements)))
                .toList();

        // 모든 비동기 작업이 완료될 때까지 대기 후 결과 취합
        List<InteractionCheckResponseDto.InteractionItem> allInteractions = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return InteractionCheckResponseDto.builder()
                .interactions(allInteractions)
                .build();
    }

    /** 단일 약품에 대한 AI 상호작용 검사 수행 */
    private List<InteractionCheckResponseDto.InteractionItem> processSingleDrug(
            DrugsMaster drug, List<AiInteractionRequest.SupplementInput> supplements) {

        List<String> ingredients = drugIngredientRepository.findByItemSeq(drug.getItemSeq()).stream()
                .map(DrugIngredient::getIngrNameEng)
                .filter(eng -> eng != null && !eng.isBlank())
                .distinct()
                .toList();

        AiInteractionRequest aiRequest = AiInteractionRequest.builder()
                .drug(AiInteractionRequest.DrugInput.builder()
                        .id(String.valueOf(drug.getId()))
                        .name(drug.getItemName())
                        .ingredients(ingredients)
                        .build())
                .supplements(supplements)
                .build();

        try {
            AiInteractionResponse aiResponse = aiServerClient.analyzeInteraction(aiRequest);
            return mapToResponseItems(aiResponse);
        } catch (RestClientException e) {
            log.warn("AI 서버 호출 실패 (drug: {}). fallback 메시지 반환: {}", drug.getItemName(), e.getMessage());
            return List.of(InteractionCheckResponseDto.InteractionItem.builder()
                    .nutrient("알 수 없음")
                    .contraindicatedDrugIngredient("알 수 없음")
                    .level("CAUTION")
                    .description("'" + drug.getItemName() + "'에 대한 AI 분석 서비스를 일시적으로 사용할 수 없습니다.")
                    .actionGuide("영양제 복용 전 반드시 의사 또는 약사와 상담하시기 바랍니다.")
                    .sources(Collections.emptyList())
                    .build());
        }
    }

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

    private List<InteractionCheckResponseDto.InteractionItem> mapToResponseItems(AiInteractionResponse ai) {
        if (ai == null || ai.getInteractions() == null) {
            return List.of();
        }
        return ai.getInteractions().stream()
                .map(i -> InteractionCheckResponseDto.InteractionItem.builder()
                        .nutrient(i.getNutrient())
                        .contraindicatedDrugIngredient(i.getContraindicatedDrugIngredient())
                        .level(i.getLevel())
                        .description(i.getDescription())
                        .actionGuide(i.getActionGuide())
                        .sources(i.getSources() != null ? i.getSources() : List.of())
                        .build())
                .toList();
    }
}
