package com.pnn.backend.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "성분명(name)은 필수입니다")
    private String name; // 영문, 예: "Vitamin C"

    private Double amount; // 함량 (선택)

    private String unit; // 단위 (선택, 예: "mg")
}
