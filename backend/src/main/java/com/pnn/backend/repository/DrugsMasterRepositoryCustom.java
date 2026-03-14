package com.pnn.backend.repository;

import com.pnn.backend.dto.DrugSearchRequestDto;
import com.pnn.backend.dto.DrugSearchResponseDto;
import com.pnn.backend.dto.PillIdentifyRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * QueryDSL을 이용한 동적 쿼리 전용 커스텀 Repository 인터페이스
 * <p>
 * Spring Data JPA의 기본 기능만으로는 복잡한 IF-ELSE 조건(동적 쿼리)을 처리하기 어려우므로,
 * 이 인터페이스를 만들고 구현체(Impl)에서 QueryDSL을 사용해 실제 쿼리를 작성합니다.
 * </p>
 */
public interface DrugsMasterRepositoryCustom {

    /**
     * 약품 상세 검색 (이름, 제조사, 성분명 기반)
     * 
     * @param request 검색 조건 (제품명, 제조사, 성분명)
     * @param pageable 페이징 정보 (몇 번째 페이지, 몇 개씩 가져올지)
     * @return 검색된 약품 목록 (간소화된 DTO 형태)과 페이징 메타데이터
     */
    Page<DrugSearchResponseDto> searchDrugDetail(DrugSearchRequestDto request, Pageable pageable);

    /**
     * 낱알 식별 검색 (모양, 색상, 각인 등 물리적 특성 기반)
     * 
     * @param request 낱알 식별 검색 조건
     * @param pageable 페이징 정보
     * @return 검색된 약품 목록
     */
    Page<DrugSearchResponseDto> searchPillIdentifier(PillIdentifyRequestDto request, Pageable pageable);
}
