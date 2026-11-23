package com.pnn.backend.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Python AI 서버 /api/v1/interaction/read-evidence 응답 DTO
 * <p>문장 1개 × 성분 1개마다 AI가 읽은 종류·쉬운 설명·할 일·근거 구절</p>
 */
@Data
public class AiEvidenceReadResponse {

    private List<Result> results;

    @Data
    public static class Result {
        private int id;             // 요청한 문장 번호
        private String nutrient;    // 성분 묶음 이름
        private String kind;        // CAUTION / NO_EFFECT / OTHER
        private String quote;       // 원문에서 그대로 복사한 근거 구절 (Spring이 대조)
        private String explanation; // 쉬운 설명
        private String action;      // 할 일 (없으면 빈 문자열)
    }
}
