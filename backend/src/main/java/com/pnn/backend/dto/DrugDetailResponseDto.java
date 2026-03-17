package com.pnn.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 약품 상세 페이지 응답용 DTO
 * 
 * 클라이언트 화면의 탭 구조에 맞춰 5개의 영역(Header, 약품정보, 복약정보, 허가정보, DUR)으로 분리하여 데이터를 전달합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugDetailResponseDto {

    private HeaderInfo header;             // 0. 상단 고정 내용
    private DrugInfo drugInfo;             // 1. 약품정보 탭
    private MedicationInfo medicationInfo; // 2. 복약정보 탭
    private PermitInfo permitInfo;         // 3. 허가정보 탭
    private DurInfo durInfo;               // 4. DUR 탭

    // ========================================================================
    // 영역별 내부 클래스 (static 클래스로 정의하여 구조화)
    // ========================================================================

    /**
     * 0. 상단 고정 내용 (Header)
     * 탭을 변경해도 항상 상단에 노출되는 핵심 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderInfo {
        private String itemName;      // 한글약품명 (drug_permit_detail 선호, 없으면 drugs_master)
        private String mainIngrName;  // 한글주성분명 (drug_ingredients 또는 drug_permit_detail)
        private String itemEngName;   // 영어약품명 (drug_permit_detail)
    }

    /**
     * 1. 약품정보 (Drug Info Tab)
     * 약의 기본 속성 및 심평원 약가마스터 연동 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugInfo {
        private String itemImageUrl;   // 약 사진 URL (drugs_master)
        private String etcOtcCode;     // 구분 - 전문/일반 (drug_permit_detail)
        private String entpName;       // 판매사 (drug_permit_detail)
        private String consignEntp;    // 제조사 - 위탁제조업체 등 (drug_permit_detail)
        private String insurCode;      // 보험코드 (drug_price_master)
        private String rawIngredients; // 영문전체 성분 및 함량 (drug_permit_detail)
        private String mainIngrCode;   // 주성분 코드 (drug_price_master)
        private String className;      // 분류명 (drugs_master)
        private String atcCode;        // ATC코드 (drug_price_master)
    }

    /**
     * 2. 복약정보 (Medication Info Tab)
     * 일반인 대상 쉬운 복약 안내 정보 (상비약 등 일부 약에만 존재할 수 있음)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationInfo {
        private String efficacy;       // 쉬운 효능 (drug_easy_info)
        private String cautionUse;     // 쉬운 주의사항/복약안내 (drug_easy_info)
    }

    /**
     * 3. 허가정보 (Permit Info Tab)
     * 식약처 원문 텍스트 기반 전문 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermitInfo {
        private String efficacy;       // 전문적인 효능효과 (drug_permit_detail)
        private String dosage;         // 용법용량 (drug_permit_detail)
        private String caution;        // 주의사항 원문 전체 (경고, 금기 등 통합 텍스트) (drug_permit_detail)
        private String storageMethod;  // 보관 및 취급상 주의/저장방법 (drug_permit_detail)
        private String validityPeriod; // 유효기간 (drug_permit_detail)
        private String packageUnit;    // 포장단위 (drug_permit_detail)
        private String itemSeq;        // 식약처 품목기준코드 (연결 키)
    }

    /**
     * 4. DUR (Drug Utilization Review Tab)
     * 병용 및 임부 금기 성분 경고 목록
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurInfo {
        // 해당 약과 같이 먹으면 안 되는 병용금기 다른 성분들 (drug_contraindication)
        private List<String> contraindications; 
        
        // 해당 약이 가지고 있는 임부금기 등 DUR 성분 경고 (dur_rules)
        private List<String> durWarnings;       
    }
}
