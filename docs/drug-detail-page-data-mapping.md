# 약품 상세 페이지 데이터 매핑 정의서 (UI ↔ DB)

본 문서는 약품 상세 페이지의 UI 구성안(탭 구조)을 바탕으로, 현재 구축된 데이터베이스(PostgreSQL)의 어떤 테이블과 컬럼에서 데이터를 가져올 수 있는지 매핑한 문서입니다.
현재 구축되지 않은 데이터는 빈칸(데이터 없음)으로 표기하였습니다.

---

## 0. 상단 고정 내용 (Header)

*(탭을 변경해도 항상 유지되는 기본 약품 정보)*


| UI 항목      | 매핑 DB 테이블 및 컬럼                  | 비고 / 상태                                                        |
| ---------- | ------------------------------- | -------------------------------------------------------------- |
| **한글약품명**  | `drugs_master.item_name` 또는 `drug_permit_detail.item_name` | ⭕ (상세 정보는 가급적 `drug_permit_detail` 활용) |
| **한글주성분명** | `drug_ingredients.ingr_name_kr` 또는 `drug_permit_detail.main_ingr_name` | ⭕ (파이프 구분자로 여러 성분 표기 가능) |
| **영어약품명**  | `drug_permit_detail.item_eng_name` | ⭕ (`drugs_master`는 영문명 누락률 14%, `drug_permit_detail`은 6%로 더 정확함) |
| **용량**     | -                               | ❌ 별도 단위 컬럼 없음 (보통 `item_name`에 "타이레놀정500밀리그램" 처럼 약품명에 포함되어 있음) |


---

## 1. 약품정보 (Drug Info Tab)


| UI 항목           | 매핑 DB 테이블 및 컬럼                     | 비고 / 상태                                      |
| --------------- | ---------------------------------- | -------------------------------------------- |
| **약 사진**        | `drugs_master.item_image_url`      | ⭕ (DrugSearchResponseDto의 `itemImageUrl` 연동) |
| **구분 (전문)**     | `drug_permit_detail.etc_otc_code`  | ⭕ (전문/일반 의약품 구분명)                             |
| **판매사**         | `drug_permit_detail.entp_name`     | ⭕ (허가 업체명)                                      |
| **제조사**         | `drug_permit_detail.consign_entp`  | ⭕ (위탁제조업체. 없을 경우 `entp_name` 사용)               |
| **보험코드**        | `drug_price_master.insur_code`     | ⭕ (심평원 약가마스터 기준 `제품코드`. 1품목 당 다수 존재 가능)         |
| **영문전체 성분(함량)** | `drug_permit_detail.raw_ingredients` | ⭕ (함량, 단위 등을 포함하는 파이프 `|` 구분 텍스트)             |
| **주성분 코드**      | `drug_price_master.main_ingr_code` | ⭕ (심평원 기준 일반명/성분명 코드. `drug_ingredients`의 식약처 코드 대신 사용) |
| **분류명(분류코드)**   | `drugs_master.class_name` (`class_no`) | ⭕ (`drugs_master`에만 존재)                       |
| **BIT 약효분류**    | -                                  | ❌ 데이터 없음                                     |
| **ATC코드**       | `drug_price_master.atc_code`       | ⭕ (약가마스터 기준 국제표준코드)                             |


---

## 2. 복약정보 (Medication Info Tab)

*(주로 환자 친화적인 `drug_easy_info` [e약은요정보] 기반)*


| UI 항목       | 매핑 DB 테이블 및 컬럼               | 비고 / 상태                                                          |
| ----------- | ---------------------------- | ---------------------------------------------------------------- |
| **효능**      | `drug_easy_info.efficacy`    | ⭕ (일반인 대상 쉬운 효능)                                                 |
| **복약안내**    | `drug_easy_info.caution_use` | ⭕ (일반인 대상 쉬운 주의사항)                                               |
| **픽토그램(?)** | -                            | ❌ 데이터 없음 (클라이언트 단에서 '졸음', '금주' 등 복약안내 키워드를 추출하여 자체 UI 아이콘 매핑 권장) |


---

## 3. 허가정보 (Permit Info Tab)

*(전문적인 `drug_permit_detail` 기반, 식약처 PDF 파싱 원문)*


| UI 항목            | 매핑 DB 테이블 및 컬럼                     | 비고 / 상태                                                                    |
| ---------------- | ---------------------------------- | -------------------------------------------------------------------------- |
| **효능효과**         | `drug_permit_detail.efficacy`      | ⭕ 전문적인 효능                                                                  |
| **용법용량**         | `drug_permit_detail.dosage`        | ⭕                                                                          |
| **경고**           | `drug_permit_detail.caution` 내 텍스트 | ⚠️ 원본 데이터가 통짜 텍스트이므로 세부 항목이 쪼개져 있지 않음. `caution` 컬럼을 통으로 제공해야 함 |
| **금기**           | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **신중투여**         | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **이상반응**         | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **일반적 주의**       | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **상호작용**         | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **임부에 대한 투여**    | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **수유부에 대한 투여**   | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **과량투여 및 처치**    | `drug_permit_detail.caution` 내 텍스트 | ⚠️ (동일)                                                                    |
| **보관 및 취급상의 주의** | `drug_permit_detail.storage_method` / `caution` | ⭕ (저장방법 컬럼에 일부 존재, 세부 주의는 caution 포함)                                  |
| **기타**           | `drug_permit_detail.validity_period`, `package_unit` 등 | ⭕ 유효기간, 포장단위 등 추가 정보 제공 가능                                       |
| **저장방법**         | `drug_permit_detail.storage_method` | ⭕                                                                          |
| **식약처 품목기준코드**   | `drug_permit_detail.item_seq` 또는 `drugs_master.item_seq` | ⭕ (모든 테이블을 연결하는 핵심 키값)                                                     |


---


## 4. DUR (Drug Utilization Review Tab)

*(안전한 약물 복용을 위한 병용/임부 금기 등)*


| UI 항목    | 매핑 DB 테이블 및 컬럼                                               | 비고 / 상태                   |
| -------- | ------------------------------------------------------------ | ------------------------- |
| **병용금기** | `drug_contraindication` 전체 `dur_rules` (`dur_type` = '병용금기') | ⭕ (해당 약의 주성분과 매칭하여 결과 노출) |
| **임부금기** | `dur_rules` (`dur_type` = '임부금기')                            | ⭕ (해당 약의 주성분과 매칭하여 결과 노출) |


