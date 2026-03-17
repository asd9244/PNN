package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugPermitDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 데이터 접근 계층 (Repository)
 * drug_permit_detail 테이블 (식약처 허가 상세정보 PDF 파싱 원문) 조회 용도
 */
@Repository
public interface DrugPermitDetailRepository extends JpaRepository<DrugPermitDetail, Long> {
    
    // 품목기준코드로 단일 1:1 매핑 정보를 가져옵니다.
    Optional<DrugPermitDetail> findByItemSeq(String itemSeq);
}
