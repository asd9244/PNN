package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 근거 기반 상호작용 검사 결과 (약 × 영양 성분)
 * <p>설명서 원문 문장을 근거로 하고, AI는 그 문장을 쉬운 말로 읽어 주기만 한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceCheckResponseDto {

    private List<DrugResult> drugs;

    /** 문장 하나를 읽은 결과의 종류 */
    public enum Kind {
        CAUTION,    // 함께 먹을 때 주의 (흡수 감소, 시간 간격 필요 등)
        NO_EFFECT,  // 영향 없음
        OTHER,      // 기타 영향 (예: 검사 결과에 영향)
        UNVERIFIED  // AI가 못 읽었거나 근거 구절이 원문과 맞지 않음 → 원문만 보여 줌
    }

    /** 성분별 최종 상태. 위에서부터 더 주의가 필요한 순서 */
    public enum Status {
        CAUTION, OTHER, UNVERIFIED, NO_EFFECT, // 설명서에 언급 있음 (문장들 중 가장 주의가 필요한 종류)
        NOT_MENTIONED,  // 설명서 상호작용 항목에 언급 없음 (안전하다는 뜻이 아님)
        NOT_IN_TABLE,   // 성분 대응표에 없는 성분 — 검색 대상 아님
        NO_SECTION      // 설명서를 못 읽었거나 상호작용 항목을 못 찾음
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugResult {
        private Long drugId;
        private String drugName;
        private boolean sectionFound;  // 설명서에서 상호작용 항목을 찾았는지
        private boolean aiCalled;      // 이 약에 대해 AI를 불렀는지 (근거 문장이 없으면 부르지 않음)
        private List<NutrientResult> nutrients;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientResult {
        private String nutrient;       // 요청한 성분 이름 그대로
        private String matchedGroup;   // 대응표의 성분 묶음 이름 (없으면 null)
        private Status status;
        private List<Finding> findings; // 근거 문장마다 하나
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private Kind kind;
        private String sentence;     // 설명서 원문 문장
        private String explanation;  // 쉬운 설명 (UNVERIFIED면 null)
        private String action;       // 할 일 (없으면 null)
    }
}
