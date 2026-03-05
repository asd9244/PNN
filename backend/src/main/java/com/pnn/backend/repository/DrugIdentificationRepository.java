package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugIdentification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugIdentificationRepository extends JpaRepository<DrugIdentification, Long> {
    // 낱알식별 정보 검색 (모양, 색상, 각인 등으로 검색하는 메서드는 나중에 추가)
}
