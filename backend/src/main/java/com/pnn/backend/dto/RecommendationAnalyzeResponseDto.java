package com.pnn.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Python AI 서버 -> Spring Boot로 반환되는 안전 영양제 추천 결과 DTO
 */
@Getter
@Setter
public class RecommendationAnalyzeResponseDto {
    private List<RecommendedNutrient> recommended_nutrients;

    @Getter
    @Setter
    public static class RecommendedNutrient {
        private String name;
        private String reason_kr;
    }
}
