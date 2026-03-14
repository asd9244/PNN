package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 의약품 낱알식별 마스터 (drugs_master)
 * 공공데이터 의약품 낱알식별.csv 기반
 */
@Entity
@Table(name = "drugs_master")
@Getter
@Setter
public class DrugsMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_seq", unique = true, nullable = false, length = 50)
    private String itemSeq;

    @Column(name = "item_name", nullable = false, length = 500)
    private String itemName;

    @Column(name = "entp_seq", length = 50)
    private String entpSeq;

    @Column(name = "entp_name", length = 255)
    private String entpName;

    @Column(name = "item_eng_name", length = 255)
    private String itemEngName;

    @Column(name = "class_no", length = 50)
    private String classNo;

    @Column(name = "class_name", length = 255)
    private String className;

    @Column(name = "etc_otc_code", length = 50)
    private String etcOtcCode;

    @Column(name = "permit_date", length = 20)
    private String permitDate;

    @Column(name = "change_date", length = 20)
    private String changeDate;

    @Column(name = "bizrno", length = 50)
    private String bizrno;

    @Column(name = "insur_code", length = 50)
    private String insurCode;

    @Column(name = "std_code", columnDefinition = "TEXT")
    private String stdCode;

    @Column(name = "appearance", columnDefinition = "TEXT")
    private String appearance;

    @Column(name = "drug_shape", length = 100)
    private String drugShape;

    @Column(name = "color_front", length = 100)
    private String colorFront;

    @Column(name = "color_back", length = 100)
    private String colorBack;

    @Column(name = "print_front", length = 255)
    private String printFront;

    @Column(name = "print_back", length = 255)
    private String printBack;

    @Column(name = "line_front", length = 50)
    private String lineFront;

    @Column(name = "line_back", length = 50)
    private String lineBack;

    @Column(name = "form_code_name", length = 255)
    private String formCodeName;

    // 수동 매핑으로 정규화된 제형 카테고리 (예: 정제, 경질캡슐, 연질캡슐, 기타)
    @Column(name = "normal_form_name", length = 50)
    private String normalFormName;

    @Column(name = "length_long", length = 50)
    private String lengthLong;

    @Column(name = "length_short", length = 50)
    private String lengthShort;

    @Column(name = "thickness", length = 50)
    private String thickness;

    @Column(name = "item_image_url", length = 1000)
    private String itemImageUrl;

    @Column(name = "img_regist_date", length = 20)
    private String imgRegistDate;

    @Column(name = "mark_text_front", length = 255)
    private String markTextFront;

    @Column(name = "mark_text_back", length = 255)
    private String markTextBack;

    @Column(name = "mark_img_front", length = 1000)
    private String markImgFront;

    @Column(name = "mark_img_back", length = 1000)
    private String markImgBack;

    @Column(name = "mark_code_front", length = 255)
    private String markCodeFront;

    @Column(name = "mark_code_back", length = 255)
    private String markCodeBack;
}
