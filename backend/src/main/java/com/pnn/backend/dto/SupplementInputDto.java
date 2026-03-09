package com.pnn.backend.dto;

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
    // 영양제 제품명 (예: "얼라이브 종합비타민")
    private String name;

    // 해당 영양제에 포함된 성분 목록
    private List<NutrientInputDto> nutrients;
}
