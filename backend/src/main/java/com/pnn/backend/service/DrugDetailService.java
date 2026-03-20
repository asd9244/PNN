package com.pnn.backend.service;

import com.pnn.backend.domain.*;
import com.pnn.backend.dto.DrugDetailResponseDto;
import com.pnn.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 비즈니스 계층 (Service)
 * 약품 상세 정보를 구성하기 위해 여러 테이블의 데이터를 취합하고,
 * 성분 기반으로 DUR(병용금기/임부금기 등) 경고를 추출하여 하나의 DTO로 반환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 이 서비스는 DB를 읽기만 하므로 성능 최적화를 위해 읽기 전용 트랜잭션을 적용합니다.
public class DrugDetailService {

        private final DrugsMasterRepository drugsMasterRepository;
        private final DrugPermitDetailRepository drugPermitDetailRepository;
        private final DrugEasyInfoRepository drugEasyInfoRepository;
        private final DrugPriceMasterRepository drugPriceMasterRepository;
        private final DrugIngredientRepository drugIngredientRepository;
        private final DurRuleRepository durRuleRepository;
        private final DrugContraindicationRepository drugContraindicationRepository;

        /**
         * 약품 고유 ID를 받아 상세 정보 통합 DTO를 반환합니다.
         */
        public DrugDetailResponseDto getDrugDetail(Long drugId) {
                log.info("약품 상세 정보 조회 시작 - drugId: {}", drugId);

                // 1. 기준이 되는 drugs_master 조회 (item_seq 확보)
                DrugsMaster master = drugsMasterRepository.findById(drugId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "해당 약품을 찾을 수 없습니다. (drugId: " + drugId + ")"));

                String itemSeq = master.getItemSeq();

                // 2. item_seq를 기반으로 연관 데이터 조회
                // 2-1. 식약처 허가 상세정보 (없으면 null 반환)
                DrugPermitDetail permit = drugPermitDetailRepository.findByItemSeq(itemSeq).orElse(null);

                // 2-2. e약은요 쉬운 복약정보 (없으면 null 반환)
                DrugEasyInfo easyInfo = drugEasyInfoRepository.findByItemSeq(itemSeq).orElse(null);

                // 2-3. 심평원 약가마스터 (포장단위별로 여러 개일 수 있으나 대표 1개만 추출, 없으면 null 반환)
                List<DrugPriceMaster> prices = drugPriceMasterRepository.findByItemSeq(itemSeq);
                DrugPriceMaster price = prices.isEmpty() ? null : prices.get(0);

                // 2-4. 해당 약의 주성분 목록 조회
                List<DrugIngredient> ingredients = drugIngredientRepository.findByItemSeq(itemSeq);

                // 2-5. 한글 주성분명 조합 (예: "아세트아미노펜 | 이부프로펜")
                String combinedKorIngredients = ingredients.stream()
                                .map(DrugIngredient::getIngrNameKr)
                                .filter(name -> name != null && !name.trim().isEmpty())
                                .collect(Collectors.joining(" | "));

                // 3. DUR 경고 수집 (제품코드 기반 dur_rules)
                List<String> insurCodes = prices.stream()
                                .map(DrugPriceMaster::getInsurCode)
                                .filter(c -> c != null && !c.trim().isEmpty())
                                .distinct()
                                .toList();
                List<DrugDetailResponseDto.DurWarningItem> durWarnings = List.of();
                if (!insurCodes.isEmpty()) {
                        var durRules = durRuleRepository.findByProductCodeIn(insurCodes);
                        durWarnings = durRules.stream()
                                        .map(r -> DrugDetailResponseDto.DurWarningItem.builder()
                                                        .durType(r.getDurType())
                                                        .ingrName(r.getIngrName())
                                                        .warningText(r.getWarningText())
                                                        .build())
                                        .filter(item -> item.getDurType() != null || item.getWarningText() != null)
                                        .toList();
                }

                // 4. 병용금기 수집 (성분명 기반 drug_contraindication)
                List<String> ingrNamesEng = ingredients.stream()
                                .map(DrugIngredient::getIngrNameEng)
                                .filter(n -> n != null && !n.trim().isEmpty())
                                .distinct()
                                .toList();
                // 대소문자 구분 없이 매칭: Repository가 LOWER(TRIM(...))로 비교하므로 소문자로 전달
                List<String> ingrNamesEngLower = ingrNamesEng.stream()
                                .map(n -> n.trim().toLowerCase())
                                .distinct()
                                .toList();
                List<DrugDetailResponseDto.ContraindicationItem> contraindications = List.of();
                if (!ingrNamesEngLower.isEmpty()) {
                        var contraList = drugContraindicationRepository.findByDrugIngredientIn(ingrNamesEngLower);
                        Set<String> seenKeys = new LinkedHashSet<>();
                        contraindications = contraList.stream()
                                        .map(dc -> DrugDetailResponseDto.ContraindicationItem.builder()
                                                        .ingrName1(dc.getIngrName1())
                                                        .ingrName2(dc.getIngrName2())
                                                        .contraindReason(dc.getContraindReason())
                                                        .build())
                                        .filter(item -> {
                                                String key = (item.getIngrName1() != null ? item.getIngrName1() : "") + "|"
                                                                + (item.getIngrName2() != null ? item.getIngrName2() : "") + "|"
                                                                + (item.getContraindReason() != null ? item.getContraindReason() : "");
                                                return seenKeys.add(key);
                                        })
                                        .toList();
                }

                // 5. DurInfo 조립 및 최종 DTO 반환
                DrugDetailResponseDto.DurInfo durInfo = DrugDetailResponseDto.DurInfo.builder()
                                .contraindications(contraindications)
                                .durWarnings(durWarnings)
                                .build();

                // 6. 영문 성분명 목록 (상세·앱 요약용)
                List<String> ingredientNamesEng = ingredients.stream()
                                .map(DrugIngredient::getIngrNameEng)
                                .filter(n -> n != null && !n.trim().isEmpty())
                                .map(String::trim)
                                .distinct()
                                .toList();
                if (ingredientNamesEng.isEmpty() && permit != null && permit.getIngrNameEng() != null
                                && !permit.getIngrNameEng().isBlank()) {
                        ingredientNamesEng = List.of(permit.getIngrNameEng().trim());
                }

                // 7. 수집한 모든 데이터를 DrugDetailResponseDto로 변환(조립)
                return buildResponseDto(master, permit, easyInfo, price, combinedKorIngredients, durInfo,
                                ingredientNamesEng);
        }

        /**
         * (내부 도우미 메서드) 최종 응답 DTO를 조립합니다.
         */
        private DrugDetailResponseDto buildResponseDto(
                        DrugsMaster master, DrugPermitDetail permit, DrugEasyInfo easyInfo, DrugPriceMaster price,
                        String combinedKorIngredients, DrugDetailResponseDto.DurInfo durInfo,
                        List<String> ingredientNamesEng) {

                // --- 0. HeaderInfo 영역 (기본적으로 permit을 우선시하고 없으면 master 사용) ---
                String headerItemName = (permit != null && permit.getItemName() != null) ? permit.getItemName()
                                : master.getItemName();
                // 주성분은 drug_ingredients에서 조합한 문자열을 우선 사용하고, 없으면 permit의 main_ingr_name 사용
                String headerMainIngr = !combinedKorIngredients.isEmpty() ? combinedKorIngredients
                                : (permit != null ? permit.getMainIngrName() : null);

                DrugDetailResponseDto.HeaderInfo header = DrugDetailResponseDto.HeaderInfo.builder()
                                .itemName(headerItemName)
                                .mainIngrName(headerMainIngr)
                                .itemEngName(permit != null ? permit.getItemEngName() : null)
                                .build();

                // --- 1. DrugInfo 영역 ---
                List<Map<String, Object>> parsed = permit != null ? permit.getParsedIngredients() : null;
                List<Map<String, Object>> parsedToUse = (parsed != null && !parsed.isEmpty())
                                ? new ArrayList<>(parsed) : null;
                DrugDetailResponseDto.DrugInfo drugInfo = DrugDetailResponseDto.DrugInfo.builder()
                                .itemImageUrl(master.getItemImageUrl())
                                .etcOtcCode(permit != null ? permit.getEtcOtcCode() : null)
                                .entpName(permit != null ? permit.getEntpName() : null)
                                .consignEntp(permit != null ? permit.getConsignEntp() : null)
                                .insurCode(price != null ? price.getInsurCode() : null)
                                .parsedIngredients(parsedToUse)
                                .rawIngredients(permit != null ? permit.getRawIngredients() : null)
                                .mainIngrCode(price != null ? price.getMainIngrCode() : null)
                                .className(master.getClassName())
                                .atcCode(price != null ? price.getAtcCode() : null)
                                .ingredientNamesEng(ingredientNamesEng != null && !ingredientNamesEng.isEmpty()
                                                ? ingredientNamesEng
                                                : null)
                                .build();

                // --- 2. MedicationInfo 영역 (쉬운 복약 정보) ---
                DrugDetailResponseDto.MedicationInfo medicationInfo = DrugDetailResponseDto.MedicationInfo.builder()
                                .efficacy(easyInfo != null ? easyInfo.getEfficacy() : null)
                                .cautionUse(easyInfo != null ? easyInfo.getCautionUse() : null)
                                .build();

                // --- 3. PermitInfo 영역 (식약처 전문 허가 정보) ---
                DrugDetailResponseDto.PermitInfo permitInfo = DrugDetailResponseDto.PermitInfo.builder()
                                .efficacy(permit != null ? permit.getEfficacy() : null)
                                .dosage(permit != null ? permit.getDosage() : null)
                                .caution(permit != null ? permit.getCaution() : null)
                                .storageMethod(permit != null ? permit.getStorageMethod() : null)
                                .validityPeriod(permit != null ? permit.getValidityPeriod() : null)
                                .packageUnit(permit != null ? permit.getPackageUnit() : null)
                                .itemSeq(master.getItemSeq())
                                .build();

                // --- 4. DurInfo 영역 (DUR 경고 + 병용금기, getDrugDetail에서 조립됨) ---

                // 최종 DTO 묶어서 반환
                return DrugDetailResponseDto.builder()
                                .header(header)
                                .drugInfo(drugInfo)
                                .medicationInfo(medicationInfo)
                                .permitInfo(permitInfo)
                                .durInfo(durInfo)
                                .build();
        }
}
