package com.pnn.backend.repository;

import com.pnn.backend.domain.Supplement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplementRepository extends JpaRepository<Supplement, Long> {
    // 영양제 이름으로 검색
    // Optional<Supplement> findByPrdlstNm(String prdlstNm);
}
