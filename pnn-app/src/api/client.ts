import axios from 'axios';
import { Platform } from 'react-native';

// EXPO_PUBLIC_API_URL이 있으면 우선 사용 (실제 기기 테스트 시 PC IP 설정용)
// 웹: localhost, Android 에뮬레이터: 10.0.2.2, iOS 시뮬레이터: localhost
const getBaseUrl = () => {
  const envUrl = process.env.EXPO_PUBLIC_API_URL;
  if (envUrl) return envUrl.replace(/\/$/, ''); // trailing slash 제거

  if (Platform.OS === 'web') return 'http://localhost:8080';
  if (Platform.OS === 'android') return 'http://10.0.2.2:8080';
  return 'http://localhost:8080'; // iOS 시뮬레이터
};

export const BASE_URL = getBaseUrl();

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

/** 네트워크 에러 시 사용자용 메시지 생성 (디버깅용 URL 포함) */
export function getNetworkErrorMessage(err: unknown): string {
  const base = '서버에 연결할 수 없습니다.';
  if (typeof err === 'object' && err !== null && 'message' in err) {
    const msg = (err as { message?: string }).message;
    if (msg?.includes('Network Error') || msg?.includes('ECONNREFUSED') || msg?.includes('timeout')) {
      return `${base}\n\n연결 URL: ${BASE_URL}\n\n※ PC와 같은 Wi-Fi인지, 백엔드가 실행 중인지 확인하세요.`;
    }
  }
  return base;
}
