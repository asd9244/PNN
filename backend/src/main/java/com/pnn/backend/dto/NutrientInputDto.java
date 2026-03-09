package com.pnn.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영양제 성분 1개를 나타내는 공통 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class NutrientInputDto {
    // 성분명 (반드시 영문, 예: "Vitamin C")
    private String name;

    // 함량 (선택)
    private Double amount;

    // 단위 (선택, 예: "mg")
    private String unit;
}
