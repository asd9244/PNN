"""
Gemini 3 Flash API 연동 서비스 (Case A/B, OCR 정형화용)

- google-genai SDK 사용. 기본 모델: gemini-3-flash-preview (ListModels 기준)
- .env의 GEMINI_API_KEY 또는 config에서 API 키 로드
- get_client(): 한 번만 생성해 재사용 (싱글톤)
- chat(): 프롬프트 전송 후 응답 텍스트 반환
"""
import os
from typing import Optional

from google import genai
from app.core.config import settings

# API 키: 환경변수 우선, 없으면 settings(설정)에서 로드
_api_key = os.getenv("GEMINI_API_KEY") or settings.GEMINI_API_KEY
_client: Optional[genai.Client] = None


def get_client() -> genai.Client:
    """Gemini API 클라이언트를 한 번만 생성해 반환. 이후 호출 시 동일 인스턴스 재사용."""
    global _client
    if _client is None:
        if not _api_key:
            raise ValueError("GEMINI_API_KEY가 설정되지 않았습니다. .env에 GEMINI_API_KEY를 추가하세요.")
        _client = genai.Client(api_key=_api_key)
    return _client


def chat(prompt: str, system: Optional[str] = None, model: str = "gemini-3-flash-preview") -> str:
    """
    Gemini 3 Flash에 텍스트 요청을 보내고 응답 텍스트를 반환.
    Case A/B 상호작용 분석, OCR 정형화 등에 사용.

    Args:
        prompt: 사용자 프롬프트
        system: (선택) 시스템 프롬프트. 없으면 prompt만 전달
        model: 모델 ID (기본: gemini-3-flash-preview)

    Returns:
        모델 응답 텍스트
    """
    client = get_client()
    contents = prompt
    if system:
        # 시스템 프롬프트(규칙/역할) + 구분선 + 사용자 프롬프트(실제 질문)를 하나로 합쳐 전달
        contents = f"{system}\n\n---\n\n{prompt}"

    response = client.models.generate_content(
        model=model,
        contents=contents,
    )
    return (response.text or "").strip()
