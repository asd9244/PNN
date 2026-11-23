package com.pnn.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Python AI 서버 /api/v1/interaction/read-evidence 요청 DTO
 * <p>약 하나의 설명서 근거 문장 목록. 같은 문장에 성분이 여러 개 나오면 한 번만 담는다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvidenceReadRequest {

    @JsonProperty("drug_name")
    private String drugName;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private int id;                // 문장 번호 (응답과 짝 맞추기용)
        private String sentence;       // 설명서 원문 문장
        private List<String> nutrients; // 이 문장에 나온 성분 묶음 이름 (예: 철분, 칼슘)
    }
}
