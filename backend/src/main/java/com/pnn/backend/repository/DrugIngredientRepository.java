package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugIngredientRepository extends JpaRepository<DrugIngredient, Long> {
    // 특정 약물의 성분 목록 조회는 Drug 엔티티의 ingredients 필드로 접근 가능하지만,
    // 성분명으로 약물을 역추적할 때 필요할 수 있음
}
