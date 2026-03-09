package com.pnn.backend.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true) // 예상 외 필드 무시
public class DrugIngredientResponse {

    private Header header; // 응답 헤더
    private Body body; // 응답 바디

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode; // 결과 코드 (00: 성공)
        private String resultMsg; // 결과 메시지
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private int pageNo; // 현재 페이지
        private int totalCount; // 전체 건수
        private int numOfRows; // 페이지당 건수
        private List<Item> items; // 주성분 목록
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("ITEM_SEQ")
        private String itemSeq; // 품목일련번호 (Drug 매칭 키)

        @JsonProperty("MTRAL_CODE")
        private String mtralCode; // 원료 코드

        @JsonProperty("MTRAL_NM")
        private String mtralNm; // 원료명

        @JsonProperty("QNT")
        private String qnt; // 분량

        @JsonProperty("INGD_UNIT_CD")
        private String ingdUnitCd; // 단위 코드

        @JsonProperty("MAIN_INGR_ENG")
        private String mainIngrEng; // 주성분 영문명
    }
}
