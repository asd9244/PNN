package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 병용금기약물 (drug_contraindication)
 * 성분명1+성분명2 조합으로 매칭
 */
@Entity
@Table(name = "drug_contraindication")
@Getter
@Setter
public class DrugContraindication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingr_name_1", nullable = false, length = 255)
    private String ingrName1;

    @Column(name = "ingr_code_1", length = 50)
    private String ingrCode1;

    @Column(name = "product_code_1", length = 50)
    private String productCode1;

    @Column(name = "product_name_1", columnDefinition = "TEXT")
    private String productName1;

    @Column(name = "entp_name_1", length = 255)
    private String entpName1;

    @Column(name = "pay_type_1", length = 50)
    private String payType1;

    @Column(name = "ingr_name_2", nullable = false, length = 255)
    private String ingrName2;

    @Column(name = "ingr_code_2", length = 50)
    private String ingrCode2;

    @Column(name = "product_code_2", length = 50)
    private String productCode2;

    @Column(name = "product_name_2", columnDefinition = "TEXT")
    private String productName2;

    @Column(name = "entp_name_2", length = 255)
    private String entpName2;

    @Column(name = "pay_type_2", length = 50)
    private String payType2;

    @Column(name = "notice_no", length = 50)
    private String noticeNo;

    @Column(name = "notice_date", length = 20)
    private String noticeDate;

    @Column(name = "contraind_reason", nullable = false, columnDefinition = "TEXT")
    private String contraindReason;
}
