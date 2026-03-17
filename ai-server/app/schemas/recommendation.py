"""
안전 영양제 추천 API (Case B) 요청/응답 스키마
"""
from pydantic import BaseModel, Field
from typing import List


class RecommendationAnalyzeRequest(BaseModel):
    """
    Spring Boot 서버로부터 수신하는 안전 영양제 추천 분석 요청 (Case B)
    """
    patient_drugs: List[str] = Field(..., description="환자가 복용 중인 처방약 성분명 목록 (영문)")
    contraindicated_nutrients: List[str] = Field(..., description="병용 금기 영양 성분명 목록 (AI가 회피해야 할 목록, 영문)")


class RecommendedNutrient(BaseModel):
    """
    개별 추천 영양 성분
    """
    name: str = Field(..., description="추천 영양 성분명 (반드시 영문, 예: Magnesium)")
    reason_kr: str = Field(..., description="추천 사유 (보수적이고 일상적인 건강 유지 목적의 설명, 한국어)")


class RecommendationAnalyzeResponse(BaseModel):
    """
    Spring Boot 서버로 반환하는 안전 영양제 추천 분석 결과 (Case B)
    """
    recommended_nutrients: List[RecommendedNutrient] = Field(
        default_factory=list,
        description="최종 추천된 안전 영양 성분 목록"
    )
