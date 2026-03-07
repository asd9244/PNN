package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity // JPA 엔티티 지정
@Table(name = "drugs") // 테이블 이름 "drugs"
@Getter @Setter // Lombok Getter/Setter
public class Drug {

    @Id // 기본키 (PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 관리용 ID

    @Column(unique = true, nullable = false) // 품목일련번호는 고유해야 함
    private String itemSeq; // 식약처 품목일련번호 (ITEM_SEQ)

    @Column(nullable = false)
    private String itemName; // 품목명 (ITEM_NAME)

    private String entpName; // 업체명 (ENTP_NAME)

    private String etcOtcCode; // 전문/일반 구분 코드 (ETC_OTC_CODE)

    @Column(columnDefinition = "TEXT") // 성상 설명이 길 수 있음
    private String chart; // 성상 (CHART)

    @Column(columnDefinition = "TEXT") // 원료 성분 목록이 매우 길 수 있음
    private String materialName; // 원료성분 (MATERIAL_NAME)

    @Column(columnDefinition = "TEXT") // 주성분 목록이 길 수 있음
    private String mainItemIngr; // 주성분 (MAIN_ITEM_INGR)

    @Column(columnDefinition = "TEXT") // 성분명 목록이 길 수 있음
    private String ingrName; // 성분명 (INGR_NAME)

    private String atcCode; // 국제표준코드 (ATC_CODE)

    @Column(columnDefinition = "TEXT") // 총량 표기가 길 수 있음
    private String totalContent; // 총량 (TOTAL_CONTENT)

    @Column(length = 1000) // URL 길이를 고려해 넉넉하게 잡음
    private String bigPrdtImgUrl; // 큰 제품 이미지 URL (BIG_PRDT_IMG_URL)

    // 낱알식별 정보와의 1:1 관계
    @OneToOne(mappedBy = "drug", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DrugIdentification identification;

    // 주성분 상세 정보와의 1:N 관계
    @OneToMany(mappedBy = "drug", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DrugIngredient> ingredients = new ArrayList<>();
}
