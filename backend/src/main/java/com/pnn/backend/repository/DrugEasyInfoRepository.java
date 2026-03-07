package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugEasyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrugEasyInfoRepository extends JpaRepository<DrugEasyInfo, Long> {
    
    // 품목기준코드로 검색
    Optional<DrugEasyInfo> findByItemSeq(String itemSeq);

    // 품목기준코드로 존재 여부 확인
    boolean existsByItemSeq(String itemSeq);
}
