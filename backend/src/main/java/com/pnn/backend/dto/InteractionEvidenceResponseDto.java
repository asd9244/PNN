package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 약 설명서(식약처 주의사항)의 상호작용 항목에서 영양 성분별로 찾은 원문 문장
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionEvidenceResponseDto {

    private Long drugId;
    private String drugName;
    private boolean sectionFound;              // 설명서에서 상호작용 항목을 찾았는지
    private List<NutrientEvidence> nutrients;  // 요청한 성분 순서대로

    public enum Status {
        FOUND,          // 설명서에 언급 있음 (sentences에 원문)
        NOT_MENTIONED,  // 상호작용 항목은 있지만 이 성분 언급 없음
        NOT_IN_TABLE,   // 성분 대응표에 없는 성분 (나트륨, 탄수화물 등) — 검색 대상 아님
        NO_SECTION      // 설명서를 못 읽었거나 상호작용 항목을 못 찾음
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientEvidence {
        private String nutrient;         // 요청한 성분 이름 그대로
        private String matchedGroup;     // 대응표에서 찾은 성분 묶음 이름 (없으면 null)
        private Status status;
        private List<String> sentences;  // 설명서 원문 문장
    }
}
