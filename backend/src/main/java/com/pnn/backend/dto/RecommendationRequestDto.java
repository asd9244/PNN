package com.pnn.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Case B: 안전 영양제 추천 API 요청 DTO
 * <p>POST /api/recommendations/safe-nutrients — 기복용 처방약을 고려한 안전 영양 성분 추천 요청 시 사용</p>
 */
@Data
public class RecommendationRequestDto {

    /** 기복용 처방약 ID 목록 (drugs_master.id). DB에서 약품·성분 조회 후 AI에 전달 */
    @NotEmpty(message = "drugIds는 비어있을 수 없습니다.")
    private List<Long> drugIds;
}
