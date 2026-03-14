package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity // JPA 엔티티
@Table(name = "user_drugs") // 테이블 이름 "user_drugs"
@Getter @Setter
public class UserDrug {

    @Id // 기본키
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User 엔티티와 N:1 관계 (FK: user_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // drugs_master.item_seq (FK)
    @Column(name = "item_seq", nullable = false, length = 50)
    private String itemSeq;

    @Column(nullable = false)
    private LocalDate startDate; // 복용 시작일

    @Column(nullable = false)
    private boolean isActive = true; // 복용 중 여부 (기본값 true)
}
