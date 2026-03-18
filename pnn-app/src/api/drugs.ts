import { apiClient } from './client';
import type {
  DrugSearchResponseDto,
  DrugDetailResponseDto,
  PageResponse,
} from '../types/drug';

export interface DrugSearchParams {
  itemName?: string;
  entpName?: string;
  ingredient?: string;
  page?: number;
  size?: number;
}

/**
 * PillIdentifyRequestDto와 동일한 구조
 * 낱알 식별 검색 요청 파라미터
 */
export interface PillIdentifyParams {
  printFront?: string;
  printBack?: string;
  drugShape?: string;
  color?: string;
  line?: string;
  formulation?: string;
  page?: number;
  size?: number;
}

/**
 * 약품 상세 검색 API
 * GET /api/drugs/search/detail?itemName=...&entpName=...&ingredient=...&page=0&size=20
 */
export async function searchDrugDetail(
  params: DrugSearchParams
): Promise<PageResponse<DrugSearchResponseDto>> {
  const queryParams: Record<string, string | number> = {};
  if (params.itemName) queryParams.itemName = params.itemName;
  if (params.entpName) queryParams.entpName = params.entpName;
  if (params.ingredient) queryParams.ingredient = params.ingredient;
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;

  const { data } = await apiClient.get<PageResponse<DrugSearchResponseDto>>(
    '/api/drugs/search/detail',
    { params: queryParams }
  );
  return data;
}

/**
 * 낱알 식별 검색 API
 * GET /api/drugs/search/pillIdentifier?printFront=...&printBack=...&drugShape=...&color=...&formulation=...&page=0&size=20
 */
export async function searchPillIdentifier(
  params: PillIdentifyParams
): Promise<PageResponse<DrugSearchResponseDto>> {
  const queryParams: Record<string, string | number> = {};
  if (params.printFront) queryParams.printFront = params.printFront;
  if (params.printBack) queryParams.printBack = params.printBack;
  if (params.drugShape) queryParams.drugShape = params.drugShape;
  if (params.color) queryParams.color = params.color;
  if (params.line) queryParams.line = params.line;
  if (params.formulation) queryParams.formulation = params.formulation;
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;

  const { data } = await apiClient.get<PageResponse<DrugSearchResponseDto>>(
    '/api/drugs/search/pillIdentifier',
    { params: queryParams }
  );
  return data;
}

/**
 * 약품 상세 정보 조회 API
 * GET /api/drugs/{drugId}
 */
export async function getDrugDetail(
  drugId: number
): Promise<DrugDetailResponseDto> {
  const { data } = await apiClient.get<DrugDetailResponseDto>(
    `/api/drugs/${drugId}`
  );
  return data;
}
