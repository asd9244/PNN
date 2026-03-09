package com.pnn.backend.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true) // 정의되지 않은 필드가 있어도 무시하고 진행
public class DrugIdentificationResponse {

    private Header header; // 응답 헤더 (결과 코드 등)
    private Body body; // 응답 바디 (실제 데이터)

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
        private List<Item> items; // 의약품 목록
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("ITEM_SEQ") // JSON 키 "ITEM_SEQ"를 이 필드에 매핑
        private String itemSeq; // 품목일련번호 (고유 식별자)

        @JsonProperty("ITEM_NAME")
        private String itemName; // 품목명

        @JsonProperty("ENTP_NAME")
        private String entpName; // 업체명

        @JsonProperty("CHART")
        private String chart; // 성상 (약의 모양, 색상 등 설명)

        @JsonProperty("ITEM_IMAGE")
        private String itemImage; // 낱알 이미지 URL

        @JsonProperty("PRINT_FRONT")
        private String printFront; // 글자 표기 (앞)

        @JsonProperty("PRINT_BACK")
        private String printBack; // 글자 표기 (뒤)

        @JsonProperty("DRUG_SHAPE")
        private String drugShape; // 의약품 모양

        @JsonProperty("COLOR_CLASS1")
        private String colorClass1; // 색상 (앞)

        @JsonProperty("COLOR_CLASS2")
        private String colorClass2; // 색상 (뒤)

        @JsonProperty("LINE_FRONT")
        private String lineFront; // 분할선 (앞)

        @JsonProperty("LINE_BACK")
        private String lineBack; // 분할선 (뒤)

        @JsonProperty("FORM_CODE_NAME")
        private String formCodeName; // 제형 코드명 (정제, 캡슐 등)

        @JsonProperty("CLASS_NAME")
        private String className; // 분류명 (전문/일반 구분 등)
    }
}
