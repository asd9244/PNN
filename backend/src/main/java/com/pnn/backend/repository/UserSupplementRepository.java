package com.pnn.backend.repository;

import com.pnn.backend.domain.UserSupplement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserSupplementRepository extends JpaRepository<UserSupplement, Long> {
    List<UserSupplement> findByUserId(Long userId); // 특정 사용자의 영양제 목록 조회
}
