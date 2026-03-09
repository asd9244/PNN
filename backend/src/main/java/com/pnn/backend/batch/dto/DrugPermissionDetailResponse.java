package com.pnn.backend.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true) // 예상 외 필드 무시
public class DrugPermissionDetailResponse {

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
        private int pageNo; // 현재 페이지 번호
        private int totalCount; // 전체 데이터 수
        private int numOfRows; // 한 페이지당 데이터 수
        private List<Item> items; // 의약품 허가 상세 목록
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("ITEM_SEQ")
        private String itemSeq; // 품목일련번호 (drugs 테이블 매칭 키)

        @JsonProperty("ITEM_NAME")
        private String itemName; // 품목명

        @JsonProperty("ENTP_NAME")
        private String entpName; // 업체명

        @JsonProperty("ETC_OTC_CODE")
        private String etcOtcCode; // 전문/일반 구분 코드

        @JsonProperty("MATERIAL_NAME")
        private String materialName; // 원료성분 (매우 긴 텍스트 가능)

        @JsonProperty("MAIN_ITEM_INGR")
        private String mainItemIngr; // 주성분

        @JsonProperty("INGR_NAME")
        private String ingrName; // 성분명

        @JsonProperty("ATC_CODE")
        private String atcCode; // 국제표준코드 (ATC)

        @JsonProperty("TOTAL_CONTENT")
        private String totalContent; // 총량

        @JsonProperty("BIG_PRDT_IMG_URL")
        private String bigPrdtImgUrl; // 제품 이미지 URL

        @JsonProperty("CHART")
        private String chart; // 성상
    }
}
