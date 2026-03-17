package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Python AI 서버 /api/v1/interaction/analyze 요청 DTO (Case A)
 * <p>처방약 정보(drug) + 기복용 영양제 목록(supplements)을 AI 서버에 전달</p>
 */
@Data
@Builder
public class AiInteractionRequest {

    /** 검사 대상 처방약 (id, 이름, 영문 성분 목록) */
    private DrugInput drug;
    /** 기복용 영양제 목록 (제품명 + 포함 영양 성분) */
    private List<SupplementInput> supplements;

    /** 처방약 정보 */
    @Data
    @Builder
    public static class DrugInput {
        private String id;
        private String name;
        /** 영문 성분명 목록. AI 분석에 사용 */
        @JsonProperty("ingredients")
        private List<String> ingredients;
    }

    /** 영양 성분 정보 (성분명, 함량, 단위) */
    @Data
    @Builder
    public static class NutrientInput {
        private String name;
        private Double amount;
        private String unit;
    }

    /** 영양제 제품 정보 (제품명 + 포함 영양 성분) */
    @Data
    @Builder
    public static class SupplementInput {
        private String name;
        @JsonProperty("nutrients")
        private List<NutrientInput> nutrients;
    }
}
