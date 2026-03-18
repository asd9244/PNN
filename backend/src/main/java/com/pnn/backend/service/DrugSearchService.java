package com.pnn.backend.service;

import com.pnn.backend.dto.DrugSearchRequestDto;
import com.pnn.backend.dto.DrugSearchResponseDto;
import com.pnn.backend.dto.PillIdentifyRequestDto;
import com.pnn.backend.repository.DrugsMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약품 검색 및 낱알 식별을 담당하는 비즈니스 로직(Service) 클래스.
 * Controller로부터 넘겨받은 요청을 검증하고, Repository를 호출하여 결과를 반환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 데이터 변경(insert, update) 없이 조회만 하므로 readOnly = true로 설정 (성능 최적화)
public class DrugSearchService {

    private final DrugsMasterRepository drugsMasterRepository;

    /**
     * 약품 상세 검색 서비스 로직
     */
    public Page<DrugSearchResponseDto> searchDrugDetail(DrugSearchRequestDto request, Pageable pageable) {
        
        // 비즈니스 룰 검증: 최소 1개 이상의 검색 조건이 입력되었는지 확인
        if (request.isEmpty()) {
            throw new IllegalArgumentException("최소 한 가지 이상의 검색 조건(제품명, 제조사, 성분명)을 입력해주세요.");
        }

        // 로그를 남겨 디버깅 시 어떤 조건으로 검색되었는지 확인하기 쉽게 합니다.
        log.info("약품 상세 검색 요청 - itemName: {}, entpName: {}, ingredient: {}",
                request.getItemName(), request.getEntpName(), request.getIngredient());

        // Repository의 QueryDSL 동적 쿼리 호출
        return drugsMasterRepository.searchDrugDetail(request, pageable);
    }

    /**
     * 낱알 식별 검색 서비스 로직
     */
    public Page<DrugSearchResponseDto> searchPillIdentifier(PillIdentifyRequestDto request, Pageable pageable) {
        
        // 비즈니스 룰 검증: 최소 1개 이상의 식별 조건이 입력되었는지 확인
        if (request.isEmpty()) {
            throw new IllegalArgumentException("최소 한 가지 이상의 식별 조건(각인, 모양, 색상 등)을 입력해주세요.");
        }

        log.info("낱알 식별 검색 요청 - printFront: {}, printBack: {}, shape: {}, color: {}, line: {}, formulation: {}",
                request.getPrintFront(), request.getPrintBack(),
                request.getDrugShape(), request.getColor(), request.getLine(), request.getFormulation());

        // Repository의 QueryDSL 동적 쿼리 호출
        return drugsMasterRepository.searchPillIdentifier(request, pageable);
    }
}
