package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "drug_easy_info")
@Getter @Setter
public class DrugEasyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String itemSeq; // 품목기준코드 (PK 역할, 기존 drugs 테이블과 매칭)

    private String itemName; // 제품명
    private String entpName; // 업체명

    @Column(columnDefinition = "TEXT")
    private String efcyQesitm; // 효능

    @Column(columnDefinition = "TEXT")
    private String useMethodQesitm; // 사용법

    @Column(columnDefinition = "TEXT")
    private String atpnWarnQesitm; // 주의사항 경고

    @Column(columnDefinition = "TEXT")
    private String atpnQesitm; // 주의사항 (영양소/음식 상호작용 포함 가능)

    @Column(columnDefinition = "TEXT")
    private String intrcQesitm; // 상호작용 (핵심 타겟)

    @Column(columnDefinition = "TEXT")
    private String seQesitm; // 부작용

    @Column(columnDefinition = "TEXT")
    private String depositMethodQesitm; // 보관법

    private String itemImage; // 낱알 이미지 URL
}
