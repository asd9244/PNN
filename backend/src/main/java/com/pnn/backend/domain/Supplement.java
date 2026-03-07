package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity // JPA 엔티티
@Table(name = "supplements") // 테이블 이름 "supplements" (건강기능식품 마스터)
@Getter @Setter
public class Supplement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // 중복 적재 방지용 고유 키
    private String prdlstReportNo; // 품목제조번호 (PRDLST_REPORT_NO)

    @Column(nullable = false)
    private String prdlstNm; // 품목명 (PRDLST_NM)

    private String bsshNm; // 업소명 (BSSH_NM)

    @Column(columnDefinition = "TEXT") // 내용이 길 수 있음
    private String primaryFnclty; // 주된 기능성 (PRIMARY_FNCLTY)

    @Column(columnDefinition = "TEXT")
    private String rawmtrlNm; // 기능 지표 성분 (RAWMTRL_NM)

    @Column(columnDefinition = "TEXT")
    private String indivRawmtrlNm; // 기능성 원재료 (INDIV_RAWMTRL_NM)

    @Column(columnDefinition = "TEXT")
    private String etcRawmtrlNm; // 기타 원재료 (ETC_RAWMTRL_NM)

    @Column(columnDefinition = "TEXT")
    private String ntkMthd; // 섭취 방법 (NTK_MTHD)

    @Column(columnDefinition = "TEXT")
    private String iftknAtntMatrCn; // 섭취 시 주의사항 (IFTKN_ATNT_MATR_CN)

    @Column(columnDefinition = "TEXT")
    private String stdrStnd; // 기준 규격 (STDR_STND)

    @Column(columnDefinition = "TEXT")
    private String capRawmtrlNm; // 캡슐 원재료 (CAP_RAWMTRL_NM)
}
