package com.pnn.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 약품 상세 검색 요청 DTO (이름, 제조사, 성분명 기반)
 * <p>
 * Controller에서 @ModelAttribute를 통해 바인딩 받습니다.
 * 최소 1개의 조건이 필요하며, Service 단에서 검증합니다.
 * </p>
 */
@Getter
@Setter
public class DrugSearchRequestDto {

    private String itemName;   // 약품명
    private String entpName;   // 제조사(업체)명
    private String ingredient; // 성분명 (한글/영문 모두 검색)
    
    // 이 클래스 내 필드들이 모두 비어있는지 확인하는 편의 메서드
    public boolean isEmpty() {
        return (itemName == null || itemName.trim().isEmpty()) &&
               (entpName == null || entpName.trim().isEmpty()) &&
               (ingredient == null || ingredient.trim().isEmpty());
    }
}
