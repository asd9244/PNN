package com.pnn.backend.repository;

import com.pnn.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<Entity, ID_Type>: 기본 CRUD 메서드를 제공하는 인터페이스
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 이메일로 사용자 찾기 (로그인 시 사용)
    Optional<User> findByEmail(String email);

    // 이메일 중복 체크용
    boolean existsByEmail(String email);
}
