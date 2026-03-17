package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugEasyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 데이터 접근 계층 (Repository)
 * drug_easy_info 테이블 (일반인 대상 e약은요 정보) 조회 용도
 */
@Repository
public interface DrugEasyInfoRepository extends JpaRepository<DrugEasyInfo, Long> {
    
    // 품목기준코드로 단일 매핑 정보를 가져옵니다. 
    // 상비약 등 일부 약물만 데이터가 있으므로, Optional로 감싸서 반환합니다.
    Optional<DrugEasyInfo> findByItemSeq(String itemSeq);
}
