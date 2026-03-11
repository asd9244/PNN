package com.pnn.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 기복용 처방약 기반 영양제 추천 요청(Case B) 정보를 담는 DTO.
 * 클라이언트로부터 분석 대상이 될 처방약 ID 목록을 전달받습니다.
 */
@Getter
@Setter
public class RecommendationRequestDto {
    @NotEmpty(message = "기복용 약물 목록(drugIds)이 최소 1개 이상 필요합니다.")
    private List<Long> drugIds; // 복용 중인 처방약들의 PK 목록
}
