package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity // JPA 엔티티
@Table(name = "drug_ingredients") // 테이블 이름 "drug_ingredients"
@Getter @Setter
public class DrugIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Drug 엔티티와 N:1 관계 (FK: drug_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false) // 외래키 컬럼명 지정
    private Drug drug;

    private String mtralCode; // 원료 코드 (MTRAL_CODE)

    private String mtralNm; // 원료명 (MTRAL_NM)

    private String qnt; // 분량 (QNT)

    private String ingdUnitCd; // 단위 코드 (INGD_UNIT_CD)

    private String mainIngrEng; // 주성분 영문명 (MAIN_INGR_ENG)
}
