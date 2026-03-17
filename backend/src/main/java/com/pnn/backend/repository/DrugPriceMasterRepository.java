package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugPriceMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 데이터 접근 계층 (Repository)
 * drug_price_master 테이블(심평원 약가마스터) 조회 용도
 */
@Repository
public interface DrugPriceMasterRepository extends JpaRepository<DrugPriceMaster, Long> {
    
    // 하나의 품목(item_seq)에 여러 포장단위(insur_code)가 있을 수 있으므로 List 반환
    List<DrugPriceMaster> findByItemSeq(String itemSeq);
}
