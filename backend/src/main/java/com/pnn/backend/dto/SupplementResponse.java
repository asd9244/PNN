package com.pnn.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplementResponse {

    @JsonProperty("I0030")
    private I0030 data; // 식품안전나라 응답 루트 키

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class I0030 {

        @JsonProperty("total_count")
        private String totalCount; // 전체 건수 (문자열 — API 원본 그대로)

        @JsonProperty("row")
        private List<Row> row; // 데이터 목록

        @JsonProperty("RESULT")
        private Result result; // 처리 결과
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        @JsonProperty("MSG")
        private String msg; // 결과 메시지

        @JsonProperty("CODE")
        private String code; // 결과 코드 (INFO-000: 정상)
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {

        @JsonProperty("PRDLST_REPORT_NO")
        private String prdlstReportNo; // 품목제조번호 (고유 키)

        @JsonProperty("PRDLST_NM")
        private String prdlstNm; // 품목명

        @JsonProperty("BSSH_NM")
        private String bsshNm; // 업소명

        @JsonProperty("PRIMARY_FNCLTY")
        private String primaryFnclty; // 주된 기능성

        @JsonProperty("RAWMTRL_NM")
        private String rawmtrlNm; // 기능 지표 성분

        @JsonProperty("INDIV_RAWMTRL_NM")
        private String indivRawmtrlNm; // 기능성 원재료

        @JsonProperty("ETC_RAWMTRL_NM")
        private String etcRawmtrlNm; // 기타 원재료

        @JsonProperty("CAP_RAWMTRL_NM")
        private String capRawmtrlNm; // 캡슐 원재료

        @JsonProperty("NTK_MTHD")
        private String ntkMthd; // 섭취 방법

        @JsonProperty("IFTKN_ATNT_MATR_CN")
        private String iftknAtntMatrCn; // 섭취 시 주의사항

        @JsonProperty("STDR_STND")
        private String stdrStnd; // 기준 규격
    }
}
