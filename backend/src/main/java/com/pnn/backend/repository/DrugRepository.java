package com.pnn.backend.repository;

import com.pnn.backend.domain.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrugRepository extends JpaRepository<Drug, Long> {

    // 품목일련번호로 의약품 조회 (공공데이터 동기화 시 사용)
    Optional<Drug> findByItemSeq(String itemSeq);

    // 품목명으로 의약품 검색 (사용자 검색용, 부분 일치)
    // Containing: SQL의 LIKE %keyword% 동작
    // List<Drug> findByItemNameContaining(String keyword);

    // 복수 약물 조회 (Case B 추천 로직용). ingredients를 JOIN FETCH로 함께 로딩하여 N+1 방지
    @Query("SELECT DISTINCT d FROM Drug d LEFT JOIN FETCH d.ingredients WHERE d.id IN :drugIds")
    List<Drug> findByIdInWithIngredients(@Param("drugIds") List<Long> drugIds);

    // 단일 약물 조회 (Case A 충돌 검사용). ingredients를 함께 로딩
    @Query("SELECT d FROM Drug d LEFT JOIN FETCH d.ingredients WHERE d.id = :id")
    Optional<Drug> findByIdWithIngredients(@Param("id") Long id);
}
