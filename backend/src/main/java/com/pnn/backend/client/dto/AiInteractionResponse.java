package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Python AI 서버 /api/v1/interaction/analyze 응답 DTO (Case A)
 * <p>영양 성분 ↔ 처방약 성분 쌍별 상호작용 등급·설명·행동 가이드</p>
 */
@Data
public class AiInteractionResponse {

    /** 상호작용 분석 결과 목록 */
    private List<InteractionItem> interactions;

    /** 개별 상호작용 항목 */
    @Data
    public static class InteractionItem {
        private String nutrient;
        @JsonProperty("contraindicated_drug_ingredient")
        private String contraindicatedDrugIngredient;
        /** SAFE | CAUTION | WARNING | SYNERGY */
        private String level;
        private String description;
        @JsonProperty("action_guide")
        private String actionGuide;
        private List<String> sources;
    }
}
