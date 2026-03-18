package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DUR 유형별 성분 현황 (dur_rules) - 통합 테이블
 * 노인주의, 용량주의, 임산부금기, 투여기간주의, 특정연령대금기, 효능군중복주의
 */
@Entity
@Table(name = "dur_rules")
@Getter
@Setter
public class DurRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dur_type", nullable = false, length = 50)
    private String durType;

    @Column(name = "product_code", columnDefinition = "TEXT")
    private String productCode;

    @Column(name = "ingr_code", columnDefinition = "TEXT")
    private String ingrCode;

    @Column(name = "ingr_name", columnDefinition = "TEXT")
    private String ingrName;

    @Column(name = "warning_text", columnDefinition = "TEXT")
    private String warningText;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;
}
