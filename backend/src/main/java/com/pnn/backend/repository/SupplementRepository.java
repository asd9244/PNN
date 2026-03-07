package com.pnn.backend.repository;

import com.pnn.backend.domain.Supplement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplementRepository extends JpaRepository<Supplement, Long> {

    boolean existsByPrdlstReportNo(String prdlstReportNo); // 중복 적재 방지용
}
