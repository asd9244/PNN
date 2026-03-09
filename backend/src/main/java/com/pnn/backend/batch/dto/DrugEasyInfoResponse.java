package com.pnn.backend.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrugEasyInfoResponse {
    
    private Header header;
    private Body body;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private int pageNo;
        private int totalCount;
        private int numOfRows;
        private List<Item> items;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String itemSeq; // 품목기준코드 (PK)
        private String itemName; // 제품명
        private String entpName; // 업체명
        private String efcyQesitm; // 효능
        private String useMethodQesitm; // 사용법
        private String atpnWarnQesitm; // 경고
        private String atpnQesitm; // 주의사항
        private String intrcQesitm; // 상호작용
        private String seQesitm; // 부작용
        private String depositMethodQesitm; // 보관법
        private String itemImage; // 낱알 이미지
    }
}
