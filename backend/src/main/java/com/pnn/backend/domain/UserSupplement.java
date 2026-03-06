package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity // JPA 엔티티 지정
@Table(name = "user_supplements") // 테이블 이름 "user_supplements"
@Getter @Setter // Lombok Getter/Setter 자동 생성
public class UserSupplement {

    @Id // 기본키 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 식별자

    @ManyToOne(fetch = FetchType.LAZY) // User와 N:1 관계, 지연 로딩
    @JoinColumn(name = "user_id", nullable = false) // 외래키 컬럼명
    private User user; // 소유자

    @Column(nullable = false)
    private String supplementName; // 영양제 이름

    @JdbcTypeCode(SqlTypes.JSON) // JSON 타입 매핑
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> nutrients; // OCR 결과 목록 (성분명, 함량, 단위 등)

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt = LocalDateTime.now(); // 등록일시
}
