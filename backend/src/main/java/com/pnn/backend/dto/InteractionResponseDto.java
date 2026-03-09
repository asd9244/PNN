package com.pnn.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 상호작용 검사(Case A) 결과를 클라이언트에게 반환하는 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InteractionResponseDto {

    // 전체 검사 결과에 대한 코멘트 (예: "총 2개의 주의 사항이 있습니다.")
    private String summary;
    
    // 이 결과가 어디서 나왔는지 (예: "RULE" = DB에서 찾음, "LLM" = AI가 분석함)
    private String source;

    // 상세 상호작용 분석 결과 목록
    private List<InteractionItem> interactions = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class InteractionItem {
        // 영양제 성분명 (예: "Magnesium")
        private String nutrient;
        
        // 부딪히는 처방약 성분명 (예: "Tetracycline")
        private String contraindicatedDrugIngredient;
        
        // 위험도 (SAFE, CAUTION, WARNING, SYNERGY)
        private String level;
        
        // 상호작용에 대한 상세 설명
        private String description;
        
        // 어떻게 대처해야 하는지에 대한 가이드
        private String actionGuide;

        // 결과의 출처 문서들 (LLM이 분석했을 경우에만 값이 들어감)
        private List<String> sources = new ArrayList<>();

        @Builder
        public InteractionItem(String nutrient, String contraindicatedDrugIngredient, String level, 
                               String description, String actionGuide, List<String> sources) {
            this.nutrient = nutrient;
            this.contraindicatedDrugIngredient = contraindicatedDrugIngredient;
            this.level = level;
            this.description = description;
            this.actionGuide = actionGuide;
            this.sources = (sources != null) ? sources : new ArrayList<>();
        }
    }
}
