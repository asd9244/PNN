package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 검색 및 낱알식별 결과 리스트용 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DrugSearchResponseDto {
    
    private Long drugId;          // 상세 페이지 이동용 식별자 (drugs_master.id)
    private String itemSeq;       // 품목기준코드
    private String itemName;      // 한글 약품명
    private String entpName;      // 제조사
    private String className;     // 분류명 (예: 해열.진통.소염제)
    private String itemImageUrl;  // 약품 썸네일 이미지 URL
}
