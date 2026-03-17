package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 영양제 OCR 응답 DTO (Python AI 서버 /api/v1/supplement/extract 응답)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementOcrResponseDto {

    private String name;
    private List<NutrientItem> nutrients;
    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientItem {
        private String name;
        private Double amount;
        private String unit;
    }
}
