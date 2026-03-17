package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Python AI 서버 /api/v1/recommendation/analyze-safe 응답 DTO (Case B)
 * <p>기복용 처방약과 상호작용이 적은 추천 영양 성분 목록</p>
 */
@Data
public class AiRecommendationResponse {

    /** 추천 영양 성분 목록 */
    @JsonProperty("recommended_nutrients")
    private List<RecommendedNutrient> recommendedNutrients;

    /** 개별 추천 영양 성분 */
    @Data
    public static class RecommendedNutrient {
        /** 영양 성분명 (영문) */
        private String name;
        /** 추천 사유 (한국어) */
        @JsonProperty("reason_kr")
        private String reasonKr;
    }
}
