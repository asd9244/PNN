package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Case A: 상호작용 검사 API 응답 DTO
 * <p>AI 분석 결과로, 처방약 성분과 영양제 성분 간 상호작용 목록을 담아 반환</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionCheckResponseDto {

    /** 상호작용 분석 결과 목록. 각 항목은 영양 성분 ↔ 처방약 성분 쌍에 대한 등급·설명·행동 가이드 포함 */
    private List<InteractionItem> interactions;

    /** 개별 상호작용 항목 (영양 성분 ↔ 처방약 성분 쌍) */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractionItem {
        /** 상호작용 대상 처방약 이름 */
        private String drugName;
        /** 영양 성분명 */
        private String nutrient;
        /** 상호작용이 있는 처방약 성분명 */
        private String contraindicatedDrugIngredient;
        /**
         * 상호작용 등급. LLM 분석 결과는 주로 WARNING / SAFE.
         * 성분 데이터 부족·AI 서버 오류 등 시스템 폴백에서는 CAUTION이 올 수 있음.
         */
        private String level;
        /** 상호작용 설명 (한국어) */
        private String description;
        /** 사용자 행동 가이드 (예: "2시간 간격 두고 복용") */
        private String actionGuide;
        /** 참고 문헌/출처 목록 */
        private List<String> sources;
    }
}
