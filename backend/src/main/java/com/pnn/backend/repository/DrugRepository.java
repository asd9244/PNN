package com.pnn.backend.repository;

import com.pnn.backend.domain.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DrugRepository extends JpaRepository<Drug, Long> {

    // 품목일련번호로 의약품 조회 (공공데이터 동기화 시 사용)
    Optional<Drug> findByItemSeq(String itemSeq);

    // 품목명으로 의약품 검색 (사용자 검색용, 부분 일치)
    // Containing: SQL의 LIKE %keyword% 동작
    // List<Drug> findByItemNameContaining(String keyword);
}
