package com.pnn.backend.service;

import com.pnn.backend.domain.DrugPermitDetail;
import com.pnn.backend.domain.DrugsMaster;
import com.pnn.backend.dto.InteractionEvidenceResponseDto;
import com.pnn.backend.dto.InteractionEvidenceResponseDto.NutrientEvidence;
import com.pnn.backend.dto.InteractionEvidenceResponseDto.Status;
import com.pnn.backend.repository.DrugPermitDetailRepository;
import com.pnn.backend.repository.DrugsMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 약 설명서(주의사항)에서 영양 성분별 근거 문장을 찾는 서비스. AI를 쓰지 않는다.
 * <p>흐름: drugId → item_seq → 주의사항(PDF면 글로 바꿔 저장) → 상호작용 항목 → 성분별 문장</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionEvidenceService {

    private final DrugsMasterRepository drugsMasterRepository;
    private final DrugPermitDetailRepository drugPermitDetailRepository;
    private final PdfTextExtractService pdfTextExtractService;

    public InteractionEvidenceResponseDto findEvidence(Long drugId, List<String> nutrientNames) {
        if (nutrientNames == null || nutrientNames.isEmpty()) {
            throw new IllegalArgumentException("nutrients 목록이 비어있습니다.");
        }
        DrugsMaster drug = drugsMasterRepository.findById(drugId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 drugId: " + drugId));

        DrugPermitDetail permit = drugPermitDetailRepository.findByItemSeq(drug.getItemSeq()).orElse(null);
        if (permit != null && pdfTextExtractService.isPdfUrl(permit.getCaution())) {
            permit = pdfTextExtractService.resolveAndCache(permit); // 처음 한 번만 PDF를 받아 글로 저장
        }
        String section = permit != null ? InteractionSectionExtractor.extract(permit.getCaution()) : null;

        List<NutrientEvidence> results = nutrientNames.stream()
                .map(name -> toEvidence(name, section))
                .toList();

        return InteractionEvidenceResponseDto.builder()
                .drugId(drug.getId())
                .drugName(drug.getItemName())
                .sectionFound(section != null)
                .nutrients(results)
                .build();
    }

    /** 성분 하나의 결과: 대응표에 없음 → 항목 없음 → 문장 있음/없음 순으로 판단 */
    private static NutrientEvidence toEvidence(String nutrientName, String section) {
        Optional<NutrientGroup> group = NutrientGroup.fromNutrientName(nutrientName);
        if (group.isEmpty()) {
            return evidence(nutrientName, null, Status.NOT_IN_TABLE, List.of());
        }
        if (section == null) {
            return evidence(nutrientName, group.get(), Status.NO_SECTION, List.of());
        }
        List<String> sentences = EvidenceSentenceFinder.findSentences(section, group.get());
        return evidence(nutrientName, group.get(), sentences.isEmpty() ? Status.NOT_MENTIONED : Status.FOUND, sentences);
    }

    private static NutrientEvidence evidence(String name, NutrientGroup group, Status status, List<String> sentences) {
        return NutrientEvidence.builder()
                .nutrient(name)
                .matchedGroup(group != null ? group.getLabel() : null)
                .status(status)
                .sentences(sentences)
                .build();
    }
}
