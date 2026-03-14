package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DUR 유형별 성분 현황 (dur_rules)
 * 병용금기, 투여기간주의, 용량주의, 임산부금기 등
 */
@Entity
@Table(name = "dur_rules")
@Getter
@Setter
public class DurgRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dur_seq", nullable = false, length = 50)
    private String durSeq;

    @Column(name = "dur_type", nullable = false, length = 50)
    private String durType;

    @Column(name = "single_complex_code", length = 20)
    private String singleComplexCode;

    @Column(name = "dur_ingr_code", length = 50)
    private String durIngrCode;

    @Column(name = "dur_ingr_name_eng", length = 255)
    private String durIngrNameEng;

    @Column(name = "dur_ingr_name", length = 255)
    private String durIngrName;

    @Column(name = "complex_drug", columnDefinition = "TEXT")
    private String complexDrug;

    @Column(name = "related_ingr", columnDefinition = "TEXT")
    private String relatedIngr;

    @Column(name = "efficacy_class_code", columnDefinition = "TEXT")
    private String efficacyClassCode;

    @Column(name = "efficacy_group", columnDefinition = "TEXT")
    private String efficacyGroup;

    @Column(name = "notice_date", length = 20)
    private String noticeDate;

    @Column(name = "contraind_content", columnDefinition = "TEXT")
    private String contraindContent;

    @Column(name = "dosage_form", columnDefinition = "TEXT")
    private String dosageForm;

    @Column(name = "age_criteria", columnDefinition = "TEXT")
    private String ageCriteria;

    @Column(name = "max_duration", columnDefinition = "TEXT")
    private String maxDuration;

    @Column(name = "max_daily_dose", columnDefinition = "TEXT")
    private String maxDailyDose;

    @Column(name = "grade", columnDefinition = "TEXT")
    private String grade;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "series_name", columnDefinition = "TEXT")
    private String seriesName;

    @Column(name = "contraind_single_complex_code", length = 20)
    private String contraindSingleComplexCode;

    @Column(name = "contraind_dur_ingr_code", length = 50)
    private String contraindDurIngrCode;

    @Column(name = "contraind_dur_ingr_name_eng", length = 255)
    private String contraindDurIngrNameEng;

    @Column(name = "contraind_dur_ingr_name", length = 255)
    private String contraindDurIngrName;

    @Column(name = "contraind_complex_drug", columnDefinition = "TEXT")
    private String contraindComplexDrug;

    @Column(name = "contraind_related_ingr", columnDefinition = "TEXT")
    private String contraindRelatedIngr;

    @Column(name = "contraind_efficacy_class", columnDefinition = "TEXT")
    private String contraindEfficacyClass;
}
