package com.pnn.backend.repository;

import com.pnn.backend.domain.Contraindication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContraindicationRepository extends JpaRepository<Contraindication, Long> {
    // 특정 성분이 포함된 병용금기 조합 조회
    // List<Contraindication> findByIngredientName1OrIngredientName2(String ingredient1, String ingredient2);
}
