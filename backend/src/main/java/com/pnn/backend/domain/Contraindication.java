package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity // JPA 엔티티
@Table(name = "contraindications") // 테이블 이름 "contraindications" (병용금기 원본)
@Getter @Setter
public class Contraindication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 성분 1 정보
    private String ingredientName1;
    private String ingredientCode1;
    private String productCode1;
    private String productName1;
    private String companyName1;

    // 성분 2 정보
    private String ingredientName2;
    private String ingredientCode2;
    private String productCode2;
    private String productName2;
    private String companyName2;

    private String noticeNo; // 공고번호
    private String noticeDate; // 공고일자

    @Column(columnDefinition = "TEXT")
    private String reason; // 금기 사유 (길 수 있음)
}
