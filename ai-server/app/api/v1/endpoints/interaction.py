"""
상호작용 분석 API — interaction_rules 미매칭 시 fallback
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional

router = APIRouter()


class DrugInput(BaseModel):
    id: Optional[str] = None
    name: str
    ingredients: list[str] = Field(default_factory=list, description="처방약 성분명 목록")


class NutrientInput(BaseModel):
    name: str
    amount: Optional[float] = None
    unit: Optional[str] = None


class SupplementInput(BaseModel):
    name: str
    nutrients: list[NutrientInput] = Field(default_factory=list)


class InteractionAnalyzeRequest(BaseModel):
    drug: DrugInput
    supplements: list[SupplementInput] = Field(default_factory=list)


class InteractionItem(BaseModel):
    nutrient: str
    contraindicated_drug_ingredient: str
    level: str  # SAFE | CAUTION | WARNING | SYNERGY
    description: str
    action_guide: str
    sources: list[str] = Field(default_factory=list)


class InteractionAnalyzeResponse(BaseModel):
    interactions: list[InteractionItem] = Field(default_factory=list)
    rag_context_count: int = 0


@router.post("/analyze", response_model=InteractionAnalyzeResponse)
def analyze_interaction(req: InteractionAnalyzeRequest):
    """
    처방약 + 영양제 상호작용 분석 (fallback).
    RAG/knowledge_embeddings 제거됨. interaction_rules 미매칭 시 빈 결과 반환.
    의사/약사 상담 권장.
    """
    try:
        # 2026-03: drug_easy_info, ingredient_efficacy, knowledge_embeddings 제거
        # 2차 분석용 RAG 없음. interaction_rules 1차 필터만 사용.
        return InteractionAnalyzeResponse(
            interactions=[],
            rag_context_count=0,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
