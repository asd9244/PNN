package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity // JPA 엔티티
@Table(name = "interaction_rules") // 테이블 이름 "interaction_rules" (1차 필터용 정제 룰)
@Getter @Setter
public class InteractionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String drugIngredient; // 처방약 성분명

    @Column(nullable = false)
    private String nutrient; // 영양 성분명

    @Enumerated(EnumType.STRING) // Enum 이름을 문자열로 저장 (SAFE, CAUTION, WARNING, SYNERGY)
    @Column(nullable = false)
    private InteractionLevel level; // 상호작용 등급

    @Column(columnDefinition = "TEXT")
    private String description; // 상세 설명

    @Column(name = "description_kr", columnDefinition = "TEXT")
    private String descriptionKr; // 상세 설명 (한국어)

    @Column(columnDefinition = "TEXT")
    private String action; // 행동 지침 (복용 중단, 시간 간격 등)

    public enum InteractionLevel {
        SAFE, CAUTION, WARNING, SYNERGY
    }
}
