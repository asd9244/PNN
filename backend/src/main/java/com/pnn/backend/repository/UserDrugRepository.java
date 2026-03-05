package com.pnn.backend.repository;

import com.pnn.backend.domain.UserDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserDrugRepository extends JpaRepository<UserDrug, Long> {

    // 특정 사용자의 활성화된(복용 중인) 처방약 목록 조회
    List<UserDrug> findByUserIdAndIsActiveTrue(Long userId);

    // 특정 사용자의 전체 처방약 이력 조회
    List<UserDrug> findByUserId(Long userId);
}
