package com.pnn.backend.repository;

import com.pnn.backend.domain.AnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {
    List<AnalysisLog> findByUserId(Long userId); // 특정 사용자의 분석 이력 조회
}
