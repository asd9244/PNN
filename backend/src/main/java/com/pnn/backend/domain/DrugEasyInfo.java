package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * e약은요정보 (drug_easy_info)
 * 소스: e약은요정보.csv (상비약 중심)
 * drugs_master, drug_permit_detail과 item_seq로 JOIN
 */
@Entity
@Table(name = "drug_easy_info")
@Getter
@Setter
public class DrugEasyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_seq", nullable = false, unique = true, length = 50)
    private String itemSeq;

    @Column(name = "product_name", nullable = false, columnDefinition = "TEXT")
    private String productName;

    @Column(name = "entp_name", length = 255)
    private String entpName;

    @Column(name = "bizrno", length = 50)
    private String bizrno;

    @Column(name = "efficacy", columnDefinition = "TEXT")
    private String efficacy;

    @Column(name = "dosage", columnDefinition = "TEXT")
    private String dosage;

    @Column(name = "before_use", columnDefinition = "TEXT")
    private String beforeUse;

    @Column(name = "caution_use", columnDefinition = "TEXT")
    private String cautionUse;

    @Column(name = "interaction_drug_food", columnDefinition = "TEXT")
    private String interactionDrugFood;

    @Column(name = "adverse_reaction", columnDefinition = "TEXT")
    private String adverseReaction;

    @Column(name = "storage", columnDefinition = "TEXT")
    private String storage;

    @Column(name = "publish_date", length = 20)
    private String publishDate;

    @Column(name = "modify_date", length = 20)
    private String modifyDate;

    @Column(name = "pill_image_url", length = 1000)
    private String pillImageUrl;
}
