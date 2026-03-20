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

    @JsonProperty("interaction_analysis")
    private String interactionAnalysis;

    /** 추천 영양 성분 목록 */
    @JsonProperty("recommended_nutrients")
    private List<RecommendedNutrient> recommendedNutrients;

    /** 개별 추천 영양 성분 */
    @Data
    public static class RecommendedNutrient {
        @JsonProperty("name_en")
        private String nameEn;
        
        @JsonProperty("name_kr")
        private String nameKr;
        
        private String reason;
        private String precaution;
    }
}
