/**
 * DrugSearchResponseDto와 동일한 구조
 * 백엔드 API 응답과 1:1 매핑
 */
export interface DrugSearchResponseDto {
  drugId: number;
  itemSeq: string;
  itemName: string;
  entpName: string;
  className: string;
  itemImageUrl: string | null;
}

/**
 * Spring Page 응답 구조
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * DrugDetailResponseDto - 약품 상세 페이지 응답
 * 백엔드 DrugDetailResponseDto와 1:1 매핑
 */
export interface DrugDetailResponseDto {
  header: DrugDetailHeaderInfo | null;
  drugInfo: DrugDetailDrugInfo | null;
  medicationInfo: DrugDetailMedicationInfo | null;
  permitInfo: DrugDetailPermitInfo | null;
  durInfo: DrugDetailDurInfo | null;
}

export interface DrugDetailHeaderInfo {
  itemName: string;
  mainIngrName: string;
  itemEngName: string;
}

/** 정제된 성분 항목 (parsed_ingredients 배열 요소) */
export interface ParsedIngredient {
  name: string;
  standard: string;
  amount: number | string | null;
  unit: string;
  note: string | null;
}

export interface DrugDetailDrugInfo {
  itemImageUrl: string;
  etcOtcCode: string;
  entpName: string;
  consignEntp: string;
  insurCode: string;
  parsedIngredients?: ParsedIngredient[] | null;
  rawIngredients: string;
  mainIngrCode: string;
  className: string;
  atcCode: string;
}

export interface DrugDetailMedicationInfo {
  efficacy: string;
  cautionUse: string;
}

export interface DrugDetailPermitInfo {
  efficacy: string;
  dosage: string;
  caution: string;
  storageMethod: string;
  validityPeriod: string;
  packageUnit: string;
  itemSeq: string;
}

export interface DrugDetailDurInfo {
  contraindications: ContraindicationItem[];
  durWarnings: DurWarningItem[];
}

/** 병용금기 1건 */
export interface ContraindicationItem {
  ingrName1: string;
  ingrName2: string;
  contraindReason: string;
}

/** DUR 경고 1건 (노인주의, 용량주의, 임산부금기 등) */
export interface DurWarningItem {
  durType: string;
  ingrName: string;
  warningText: string;
}
