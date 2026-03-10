package com.pnn.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 영양제 제품 1개(제품명과 포함된 성분 목록)를 나타내는 공통 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class SupplementInputDto {

    private String name; // 영양제 제품명 (예: "얼라이브 종합비타민")

    @NotNull(message = "nutrients는 필수입니다")
    @NotEmpty(message = "nutrients는 비어 있을 수 없습니다")
    @Valid
    private List<NutrientInputDto> nutrients;
}
