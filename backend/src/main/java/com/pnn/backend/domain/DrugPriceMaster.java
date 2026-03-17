package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 심평원 약가마스터 (drug_price_master)
 * 보험코드, 주성분코드, ATC코드 등의 정보를 포함합니다.
 */
@Entity
@Table(name = "drug_price_master")
@Getter
@Setter
public class DrugPriceMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_seq", length = 50)
    private String itemSeq;       // 식약처 품목기준코드 (drugs_master와 연결 고리)

    @Column(name = "insur_code", length = 50)
    private String insurCode;     // 보험코드 (제품코드)

    @Column(name = "main_ingr_code", length = 50)
    private String mainIngrCode;  // 주성분코드 (심평원 일반명코드)

    @Column(name = "atc_code", length = 50)
    private String atcCode;       // 국제 표준 ATC 코드
}
