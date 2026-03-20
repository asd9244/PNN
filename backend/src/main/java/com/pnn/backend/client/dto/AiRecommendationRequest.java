package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Python AI 서버 /api/v1/recommendation/analyze-safe 요청 DTO (Case B)
 * <p>기저 질환과 복용 약품명·성분 목록을 AI 서버에 전달하여 안전 영양 성분 추천 요청</p>
 */
@Data
@Builder
public class AiRecommendationRequest {

    /** 사용자의 기저 질환 또는 병명 (선택) */
    @JsonProperty("condition")
    private String condition;

    /** 환자 복용 약품명·성분명 목록. AI가 이 목록을 고려해 안전 영양 성분 추천 */
    @JsonProperty("patient_drugs")
    private List<String> patientDrugs;
}
