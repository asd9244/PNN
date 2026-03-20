"""
안전 영양제 추천 API (Case B) 요청/응답 스키마
"""
from pydantic import BaseModel, Field
from typing import List


class RecommendationAnalyzeRequest(BaseModel):
    """
    Spring Boot 서버로부터 수신하는 안전 영양제 추천 분석 요청 (Case B).
    """
    condition: str = Field(default="", description="사용자의 기저 질환 또는 병명 (선택)")
    patient_drugs: List[str] = Field(..., description="복용 중인 약: 제품명·영문 성분 등")


class RecommendedNutrient(BaseModel):
    """
    개별 추천 영양 성분
    """
    name_en: str = Field(..., description="영문 성분명 (예: Magnesium)")
    name_kr: str = Field(..., description="한글 성분명 (예: 마그네슘)")
    reason: str = Field(..., description="한국어로 작성된 보수적 추천 사유")
    precaution: str = Field(..., description="일반적인 복용 주의사항")


class RecommendationAnalyzeResponse(BaseModel):
    """
    Spring Boot 서버로 반환하는 안전 영양제 추천 분석 결과 (Case B)
    """
    interaction_analysis: str = Field(
        default="",
        description="입력된 약물과 질환을 바탕으로 한 내부적인 상호작용 및 안전성 검토 과정"
    )
    recommended_nutrients: List[RecommendedNutrient] = Field(
        default_factory=list,
        description="최종 추천된 안전 영양 성분 목록"
    )
