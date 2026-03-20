package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Case B: 안전 영양제 추천 API 응답 DTO
 * <p>AI가 기복용 처방약을 고려해 추천한 안전 영양 성분 목록을 담아 반환</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {

    private String interactionAnalysis;

    /** 추천 영양 성분 목록. 기복용 처방약과 상호작용이 적은 성분들 */
    private List<RecommendedNutrient> recommendedNutrients;

    /** 개별 추천 영양 성분 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedNutrient {
        private String nameEn;
        private String nameKr;
        private String reason;
        private String precaution;
    }
}
