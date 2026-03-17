package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Python AI 서버 /api/v1/recommendation/analyze-safe 요청 DTO (Case B)
 * <p>기복용 약품명·성분 목록 + 금기 영양 성분(선택)을 AI 서버에 전달</p>
 */
@Data
@Builder
public class AiRecommendationRequest {

    /** 환자 복용 약품명·성분명 목록. AI가 이 목록을 고려해 안전 영양 성분 추천 */
    @JsonProperty("patient_drugs")
    private List<String> patientDrugs;

    /** 절대 추천하면 안 되는 금기 영양 성분 목록. 비어 있으면 AI가 patient_drugs에서 추론 */
    @JsonProperty("contraindicated_nutrients")
    private List<String> contraindicatedNutrients;
}
