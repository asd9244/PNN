package com.pnn.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Spring Boot -> Python AI 서버로 안전 영양제 추천 분석을 요청할 때 사용하는 DTO
 */
@Getter
@Setter
@Builder
public class RecommendationAnalyzeRequestDto {
    private List<String> patient_drugs; // 환자가 복용 중인 처방약 성분 목록
    private List<String> contraindicated_nutrients; // 병용 금기 영양 성분 목록 (AI가 회피해야 할 목록)
}
