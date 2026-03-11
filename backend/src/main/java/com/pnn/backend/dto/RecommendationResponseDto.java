package com.pnn.backend.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * 기복용 처방약 기반 영양제 추천 결과(Case B)를 클라이언트에게 반환하는 DTO.
 * 피해야 할 영양 성분(warnings)과 추천하는 안전한 영양 성분(recommendations)을 포함합니다.
 */
@Getter
@Builder
public class RecommendationResponseDto {
    private List<WarningItem> warnings; // 병용 금기/주의 영양 성분 목록 (DB 1차 필터 결과)
    private List<RecommendationItem> recommendations; // AI가 추천하는 안전한 영양 성분 목록

    /**
     * 금기 영양 성분 정보
     */
    @Getter
    @Builder
    public static class WarningItem {
        private String nutrient; // 금기 영양 성분명 (예: Yohimbine)
        private String reason; // 위험 사유
        private String actionGuide; // 사용자 행동 지침
    }

    /**
     * 추천 영양 성분 정보
     */
    @Getter
    @Builder
    public static class RecommendationItem {
        private String nutrient; // 추천 영양 성분명 (예: Magnesium)
        private String reason; // 추천 사유 (AI 생성)
        private String purchaseLink; // 제휴 커머스(예: 아이허브) 검색 URL 딥링크
    }
}
