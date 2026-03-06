package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugPriceMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DrugPriceMasterRepository extends JpaRepository<DrugPriceMaster, Long> {
    Optional<DrugPriceMaster> findByStdCode(String stdCode); // 표준코드로 약가 마스터 정보 조회
}
