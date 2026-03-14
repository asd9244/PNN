package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 의약품 제품허가 상세정보 (drug_permit_detail)
 * 소스: 의약품 제품허가 상세정보.csv
 * 품목일련번호(item_seq) = drugs_master.item_seq 와 JOIN 가능
 */
@Entity
@Table(name = "drug_permit_detail")
@Getter
@Setter
public class DrugPermitDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_seq", nullable = false, length = 50)
    private String itemSeq;

    @Column(name = "item_name", nullable = false, columnDefinition = "TEXT")
    private String itemName;

    @Column(name = "item_eng_name", columnDefinition = "TEXT")
    private String itemEngName;

    @Column(name = "entp_name", columnDefinition = "TEXT")
    private String entpName;

    @Column(name = "entp_eng_name", columnDefinition = "TEXT")
    private String entpEngName;

    @Column(name = "bizrno", length = 50)
    private String bizrno;

    @Column(name = "permit_no", length = 50)
    private String permitNo;

    @Column(name = "permit_type", length = 50)
    private String permitType;

    @Column(name = "cancel_status", length = 50)
    private String cancelStatus;

    @Column(name = "cancel_date", length = 20)
    private String cancelDate;

    @Column(name = "change_date", length = 20)
    private String changeDate;

    @Column(name = "permit_date", length = 20)
    private String permitDate;

    @Column(name = "etc_otc_code", length = 50)
    private String etcOtcCode;

    @Column(name = "narcotic_class", columnDefinition = "TEXT")
    private String narcoticClass;

    @Column(name = "finished_raw_type", columnDefinition = "TEXT")
    private String finishedRawType;

    @Column(name = "new_drug_yn", length = 10)
    private String newDrugYn;

    @Column(name = "biz_type", columnDefinition = "TEXT")
    private String bizType;

    @Column(name = "rare_drug_yn", length = 10)
    private String rareDrugYn;

    @Column(name = "consign_entp", columnDefinition = "TEXT")
    private String consignEntp;

    @Column(name = "raw_ingredients", columnDefinition = "TEXT")
    private String rawIngredients;

    @Column(name = "ingr_name_eng", columnDefinition = "TEXT")
    private String ingrNameEng;

    @Column(name = "main_ingr_name", columnDefinition = "TEXT")
    private String mainIngrName;

    @Column(name = "additive_name", columnDefinition = "TEXT")
    private String additiveName;

    @Column(name = "atc_code", length = 50)
    private String atcCode;

    @Column(name = "total_qty", columnDefinition = "TEXT")
    private String totalQty;

    @Column(name = "efficacy", columnDefinition = "TEXT")
    private String efficacy;

    @Column(name = "dosage", columnDefinition = "TEXT")
    private String dosage;

    @Column(name = "caution", columnDefinition = "TEXT")
    private String caution;

    @Column(name = "change_history", columnDefinition = "TEXT")
    private String changeHistory;

    @Column(name = "appearance", columnDefinition = "TEXT")
    private String appearance;

    @Column(name = "storage_method", columnDefinition = "TEXT")
    private String storageMethod;

    @Column(name = "validity_period", columnDefinition = "TEXT")
    private String validityPeriod;

    @Column(name = "package_unit", columnDefinition = "TEXT")
    private String packageUnit;

    @Column(name = "attach_doc", columnDefinition = "TEXT")
    private String attachDoc;

    @Column(name = "reexam_target", columnDefinition = "TEXT")
    private String reexamTarget;

    @Column(name = "reexam_period", columnDefinition = "TEXT")
    private String reexamPeriod;

    @Column(name = "std_code", columnDefinition = "TEXT")
    private String stdCode;

    @Column(name = "insur_code", columnDefinition = "TEXT")
    private String insurCode;
}
