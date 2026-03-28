"""Case B 안전 영양제 추천 요청/응답 스키마."""

from pydantic import BaseModel, Field


class RecommendationAnalyzeRequest(BaseModel):
    """Spring에서 전달하는 분석 요청."""

    condition: str = Field(default="", description="기저 질환 또는 병명 (선택)")
    patient_drugs: list[str] = Field(..., description="복용 약: 제품명·영문 성분 등")


class RecommendedNutrient(BaseModel):
    name_en: str = Field(..., description="영문 성분명")
    name_kr: str = Field(..., description="한글 성분명")
    reason: str = Field(..., description="추천 사유")
    precaution: str = Field(..., description="복용 주의")


class RecommendationAnalyzeResponse(BaseModel):
    interaction_analysis: str = Field(
        default="",
        description="내부 상호작용·안전성 검토 요약",
    )
    recommended_nutrients: list[RecommendedNutrient] = Field(
        default_factory=list,
        description="추천 영양 성분 목록",
    )
