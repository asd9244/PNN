package com.pnn.backend.repository;

import com.pnn.backend.domain.DurRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * DUR 유형별 성분 현황 (dur_rules) - 제품코드 기반 조회
 */
public interface DurRuleRepository extends JpaRepository<DurRule, Long> {

    @Query("SELECT d FROM DurRule d WHERE d.productCode IN :productCodes")
    List<DurRule> findByProductCodeIn(@Param("productCodes") List<String> productCodes);
}
