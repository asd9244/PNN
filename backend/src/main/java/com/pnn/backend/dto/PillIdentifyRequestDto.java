package com.pnn.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 낱알 식별 검색 요청 DTO (물리적 외형 기반)
 * <p>
 * Controller에서 @ModelAttribute를 통해 바인딩 받습니다.
 * 최소 1개의 조건이 필요하며, Service 단에서 검증합니다.
 * </p>
 */
@Getter
@Setter
public class PillIdentifyRequestDto {

    private String printFront;  // 앞면 식별 문자 (실제 쿼리시엔 앞/뒤 OR 검색)
    private String printBack;   // 뒷면 식별 문자 (실제 쿼리시엔 앞/뒤 OR 검색)
    private String markCode;    // 식별 마크 코드 (이미지가 아닌 고유 코드)
    private String drugShape;   // 알약 모양 (예: 원형, 장방형)
    private String color;       // 색상 (예: 하양, 노랑) - 앞/뒤 OR 검색
    private String line;        // 분할선 (예: +, -) - 앞/뒤 OR 검색
    private String formulation; // 정규화된 제형 (예: 정제, 경질캡슐, 연질캡슐, 기타)

    // 이 클래스 내 검색 조건들이 모두 비어있는지 확인하는 편의 메서드
    public boolean isEmpty() {
        return (printFront == null || printFront.trim().isEmpty()) &&
               (printBack == null || printBack.trim().isEmpty()) &&
               (markCode == null || markCode.trim().isEmpty()) &&
               (drugShape == null || drugShape.trim().isEmpty()) &&
               (color == null || color.trim().isEmpty()) &&
               (line == null || line.trim().isEmpty()) &&
               (formulation == null || formulation.trim().isEmpty());
    }
}
