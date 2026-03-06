package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity // JPA 엔티티 지정
@Table(name = "analysis_logs") // 테이블 이름 "analysis_logs"
@Getter @Setter // Lombok Getter/Setter 자동 생성
public class AnalysisLog {

    @Id // 기본키 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 식별자

    @ManyToOne(fetch = FetchType.LAZY) // User와 N:1 관계
    @JoinColumn(name = "user_id", nullable = false) // 외래키 컬럼명
    private User user; // 요청 사용자

    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장
    @Column(nullable = false)
    private CaseType caseType; // 분석 유형 (CASE_A: 충돌검사, CASE_B: 추천)

    @JdbcTypeCode(SqlTypes.JSON) // JSON 타입 매핑
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> request; // 분석 요청 원문 (JSON)

    @JdbcTypeCode(SqlTypes.JSON) // JSON 타입 매핑
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> response; // 분석 응답 원문 (JSON)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성일시

    public enum CaseType {
        CASE_A, CASE_B // A: 충돌검사, B: 영양제 추천
    }
}
