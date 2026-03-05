package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity // JPA 엔티티
@Table(name = "drug_identification") // 테이블 이름 "drug_identification"
@Getter @Setter
public class DrugIdentification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Drug 엔티티와 1:1 관계 (FK: drug_id)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false) // 외래키 컬럼명 지정, 필수
    private Drug drug;

    private String drugShape; // 의약품 모양 (DRUG_SHAPE)

    private String colorClass1; // 색상 (앞) (COLOR_CLASS1)

    private String colorClass2; // 색상 (뒤) (COLOR_CLASS2)

    private String printFront; // 표시 (앞) (PRINT_FRONT)

    private String printBack; // 표시 (뒤) (PRINT_BACK)

    private String lineFront; // 분할선 (앞) (LINE_FRONT)

    private String lineBack; // 분할선 (뒤) (LINE_BACK)

    private String formCodeName; // 제형 코드명 (FORM_CODE_NAME)

    @Column(length = 1000)
    private String itemImage; // 낱알 이미지 URL (ITEM_IMAGE)

    private String className; // 분류명 (CLASS_NAME)
}
