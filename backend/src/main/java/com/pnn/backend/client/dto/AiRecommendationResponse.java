package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** Python AI POST /api/v1/recommendation/analyze-safe 응답 (Case B) */
@Data
public class AiRecommendationResponse {

    @JsonProperty("interaction_analysis")
    private String interactionAnalysis;

    @JsonProperty("recommended_nutrients")
    private List<RecommendedNutrient> recommendedNutrients;

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
