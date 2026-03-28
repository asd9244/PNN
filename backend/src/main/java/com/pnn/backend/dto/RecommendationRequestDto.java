package com.pnn.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Case B: POST /api/recommendations/safe-nutrients 요청 */
@Data
public class RecommendationRequestDto {

    /** 기복용 처방약 ID 목록 (drugs_master.id). DB에서 약품·성분 조회 후 AI에 전달 */
    @NotEmpty(message = "drugIds는 비어있을 수 없습니다.")
    private List<Long> drugIds;

    /** 사용자의 기저 질환 또는 병명 텍스트 (선택 입력) */
    private String condition;
}
