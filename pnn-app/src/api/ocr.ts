import { Platform } from 'react-native';
import * as FileSystem from 'expo-file-system/legacy';
import { BASE_URL, apiClient } from './client';
import type { Supplement } from '../store/useDrugStore';

interface SupplementOcrResponseDto {
  name: string;
  nutrients: {
    name: string;
    amount?: number;
    unit?: string;
  }[];
  error?: string;
}

function parseOcrResponse(data: SupplementOcrResponseDto, imageUri: string): Supplement {
  if (data.error) {
    throw new Error(data.error);
  }
  return {
    id: Math.random().toString(36).substring(2, 11),
    name: data.name || '알 수 없는 영양제',
    ingredients: data.nutrients || [],
    imageUrl: imageUri,
  };
}

/** 웹: blob/data URL → File 변환 후 FormData로 전송 (브라우저는 { uri, name, type } 미지원) */
async function uploadViaFetch(url: string, imageUri: string, type: string): Promise<Supplement> {
  const filename = imageUri.split('/').pop() || 'supplement.jpg';
  const sanitizedFilename = filename.includes('.') ? filename : `${filename}.jpg`;

  const imageResponse = await fetch(imageUri);
  const blob = await imageResponse.blob();
  const file = new File([blob], sanitizedFilename, { type });

  const formData = new FormData();
  formData.append('image', file);

  const response = await fetch(url, {
    method: 'POST',
    body: formData,
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Server error: ${response.status} ${errText}`);
  }

  const data: SupplementOcrResponseDto = await response.json();
  return parseOcrResponse(data, imageUri);
}

/** 네이티브(iOS/Android): expo-file-system/legacy uploadAsync 사용 */
async function uploadViaFileSystem(url: string, imageUri: string, type: string): Promise<Supplement> {
  const uploadResult = await FileSystem.uploadAsync(url, imageUri, {
    httpMethod: 'POST',
    uploadType: FileSystem.FileSystemUploadType.MULTIPART,
    fieldName: 'image',
    mimeType: type,
    headers: { Accept: 'application/json' },
  });

  if (uploadResult.status !== 200) {
    throw new Error(`Server error: ${uploadResult.status} ${uploadResult.body}`);
  }

  const data: SupplementOcrResponseDto = JSON.parse(uploadResult.body);
  return parseOcrResponse(data, imageUri);
}

export async function analyzeSupplementImage(imageUri: string): Promise<Supplement> {
  const url = `${BASE_URL}/api/supplements/ocr/extract`;
  const filename = imageUri.split('/').pop() || 'supplement.jpg';
  const match = /\.(\w+)$/.exec(filename);
  const type = match ? `image/${match[1]}` : `image/jpeg`;

  try {
    if (Platform.OS === 'web') {
      return await uploadViaFetch(url, imageUri, type);
    }
    return await uploadViaFileSystem(url, imageUri, type);
  } catch (err: any) {
    throw new Error(err.message || '영양제 성분 분석에 실패했습니다.');
  }
}

// 상호작용 비교 분석 API (Case A)
export interface InteractionCompareRequest {
  drugIds: number[];
  supplements: {
    name: string;
    nutrients: {
      name: string;
      amount?: number | null;
      unit?: string | null;
    }[];
  }[];
}

/** 문장 하나를 읽은 결과의 종류 (백엔드 EvidenceCheckResponseDto.Kind) */
export type EvidenceKind = 'CAUTION' | 'NO_EFFECT' | 'OTHER' | 'UNVERIFIED';

/** 성분별 최종 상태 (백엔드 EvidenceCheckResponseDto.Status) */
export type EvidenceStatus =
  | EvidenceKind
  | 'NOT_MENTIONED' // 설명서 상호작용 항목에 언급 없음 (안전하다는 뜻이 아님)
  | 'NOT_IN_TABLE' // 성분 대응표에 없는 성분 — 검사 대상 아님
  | 'NO_SECTION'; // 설명서를 못 읽었거나 상호작용 항목을 못 찾음

export interface EvidenceFinding {
  kind: EvidenceKind;
  sentence: string; // 설명서 원문 문장
  explanation: string | null; // 쉬운 설명
  action: string | null; // 할 일
  note: string | null; // 원문을 확인해 달라는 안내
  cached: boolean; // 저장된 결과를 썼는지
}

export interface EvidenceNutrientResult {
  nutrient: string; // 요청한 성분 이름 그대로
  matchedGroup: string | null; // 대응표의 성분 묶음 이름
  status: EvidenceStatus;
  findings: EvidenceFinding[];
}

export interface EvidenceDrugResult {
  drugId: number;
  drugName: string;
  sectionFound: boolean;
  aiCalled: boolean;
  nutrients: EvidenceNutrientResult[];
}

export interface EvidenceCheckResponse {
  drugs: EvidenceDrugResult[];
}

/** 약 설명서 원문을 근거로 한 상호작용 검사 (로컬 AI가 원문만 읽음, 같은 문장은 저장된 결과를 씀) */
export async function compareInteractions(
  requestData: InteractionCompareRequest
): Promise<EvidenceCheckResponse> {
  // 처음 보는 약은 AI가 설명서를 읽느라 약 하나에 수십 초 걸릴 수 있어 타임아웃 2분
  const { data } = await apiClient.post<EvidenceCheckResponse>(
    '/api/interaction/evidence-check',
    requestData,
    { timeout: 120000 }
  );
  return data;
}

export interface RecommendationRequest {
  drugIds: number[];
  condition?: string;
}

export interface RecommendedNutrient {
  nameEn: string;
  nameKr: string;
  reason: string;
  precaution: string;
}

export interface RecommendationResponse {
  interactionAnalysis: string;
  recommendedNutrients: RecommendedNutrient[];
}

export async function getRecommendations(
  requestData: RecommendationRequest
): Promise<RecommendationResponse> {
  // Case B LLM 추론 대비 타임아웃 2분
  const { data } = await apiClient.post<RecommendationResponse>(
    '/api/recommendations/safe-nutrients',
    requestData,
    { timeout: 120000 }
  );
  return data;
}
