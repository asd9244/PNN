package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity // JPA 엔티티 지정
@Table(name = "drug_price_master") // 테이블 이름 "drug_price_master"
@Getter @Setter // Lombok Getter/Setter 자동 생성
public class DrugPriceMaster {

    @Id // 기본키 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 식별자

    @Column(length = 500)
    private String itemName; // 한글상품명

    @Column(length = 500)
    private String entpName; // 업체명

    @Column(length = 2000)
    private String drugSpec; // 약품규격 (가장 길 수 있음)

    private String formType; // 제형구분

    private String pkgType; // 포장형태

    @Column(unique = true) // 유니크 제약조건
    private String stdCode; // 표준코드 (제품코드)

    private String permitDate; // 품목허가일자

    private String etcOtcType; // 전문일반구분

    private String representCode; // 대표코드

    private String barCode; // 바코드

    private String ingrCode; // 일반명코드 (성분코드)

    private String atcCode; // ATC코드 (국제표준)
}
