package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Case B: 안전 영양 성분 추천 API 응답 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {

    private String interactionAnalysis;

    private List<RecommendedNutrient> recommendedNutrients;

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
