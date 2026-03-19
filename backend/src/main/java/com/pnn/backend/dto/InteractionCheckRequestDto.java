package com.pnn.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Case A: 상호작용 검사 API 요청 DTO
 * <p>POST /api/interaction/check — 신규 처방약과 기복용 영양제 간 상호작용 검사 요청 시 사용</p>
 */
@Data
public class InteractionCheckRequestDto {

    /** 검사 대상 처방약 ID 목록 (drugs_master.id). DB에서 약품 정보 조회에 사용 */
    @NotEmpty(message = "drugIds 배열은 비어있을 수 없습니다.")
    private List<Long> drugIds;

    /** 기복용 영양제 목록. 각 제품별 제품명(name)과 포함 영양 성분(nutrients) 포함 */
    private List<SupplementInput> supplements;

    /** 영양제 제품 정보 (제품명 + 포함 영양 성분 목록) */
    @Data
    public static class SupplementInput {
        /** 제품명 */
        private String name;
        /** 포함 영양 성분 목록 (성분명, 함량, 단위) */
        private List<NutrientInput> nutrients;
    }

    /** 영양 성분 정보 (성분명, 함량, 단위) */
    @Data
    public static class NutrientInput {
        private String name;
        private Double amount;
        private String unit;
    }
}
