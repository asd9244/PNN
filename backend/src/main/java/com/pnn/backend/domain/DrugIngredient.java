package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 의약품 주성분 (drug_ingredients)
 * drugs_master.item_seq와 1:N 관계
 */
@Entity
@Table(name = "drug_ingredients")
@Getter
@Setter
public class DrugIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_seq", nullable = false, length = 50)
    private String itemSeq;

    @Column(name = "entp_name", columnDefinition = "TEXT")
    private String entpName;

    @Column(name = "item_name", columnDefinition = "TEXT")
    private String itemName;

    @Column(name = "ingr_name_kr", columnDefinition = "TEXT")
    private String ingrNameKr;

    @Column(name = "ingr_name_eng", columnDefinition = "TEXT")
    private String ingrNameEng;

    @Column(name = "ingr_code", length = 50)
    private String ingrCode;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "raw_qty", columnDefinition = "TEXT")
    private String rawQty;

    @Column(name = "seq_no", length = 50)
    private String seqNo;

    @Column(name = "total_seq", length = 50)
    private String totalSeq;

    @Column(name = "bizrno", length = 50)
    private String bizrno;

    @Column(name = "permit_no", length = 50)
    private String permitNo;
}
