package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity // 이 클래스를 JPA 엔티티로 지정 (DB 테이블과 매핑)
@Table(name = "users") // 테이블 이름을 "users"로 지정 (PostgreSQL 예약어 'user' 회피)
@Getter @Setter // Lombok: 모든 필드의 Getter/Setter 메서드 자동 생성
public class User {

    @Id // 기본키(PK) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL의 SERIAL/BIGSERIAL 사용 (Auto Increment)
    private Long id;

    @Column(nullable = false, unique = true) // NOT NULL, 유니크 제약조건 (이메일 중복 방지)
    private String email;

    @Column(nullable = false) // NOT NULL (비밀번호 필수)
    private String passwordHash; // BCrypt로 해싱된 비밀번호 저장

    @Column(nullable = false, updatable = false) // 생성일은 수정 불가
    private LocalDateTime createdAt = LocalDateTime.now(); // 객체 생성 시 현재 시간으로 초기화
}
