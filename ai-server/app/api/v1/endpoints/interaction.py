"""
상호작용 분석 API — interaction_rules 미매칭 시 fallback
(Spring Boot의 Controller + DTO 역할을 수행하는 모듈입니다)
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional

router = APIRouter()

# -------------------------------------------------------------------
# Data Transfer Objects (DTO)
# - Java Spring Boot의 DTO 클래스들과 동일한 역할을 하는 객체들입니다.
# - Pydantic의 BaseModel을 상속받아 JSON 직렬화/역직렬화 및 검증을 수행합니다.
# -------------------------------------------------------------------

class DrugInput(BaseModel):
    """클라이언트(Spring)에서 전달받는 처방약 정보 DTO"""
    id: Optional[str] = None
    name: str
    ingredients: list[str] = Field(default_factory=list, description="처방약 성분명 목록")


class NutrientInput(BaseModel):
    """개별 영양 성분 정보 DTO (예: Vitamin C, 500mg)"""
    name: str
    amount: Optional[float] = None
    unit: Optional[str] = None


class SupplementInput(BaseModel):
    """영양제 1개 제품 정보 DTO (제품명 + 포함된 성분 목록)"""
    name: str
    nutrients: list[NutrientInput] = Field(default_factory=list)


class InteractionAnalyzeRequest(BaseModel):
    """Spring Boot가 POST 요청 Body로 보내는 전체 요청 DTO"""
    drug: DrugInput
    supplements: list[SupplementInput] = Field(default_factory=list)


class InteractionItem(BaseModel):
    """분석된 개별 상호작용 결과 1건을 나타내는 DTO"""
    nutrient: str
    contraindicated_drug_ingredient: str
    level: str  # SAFE | CAUTION | WARNING | SYNERGY
    description: str
    action_guide: str
    sources: list[str] = Field(default_factory=list)


class InteractionAnalyzeResponse(BaseModel):
    """Spring Boot로 최종 반환하는 응답 DTO"""
    interactions: list[InteractionItem] = Field(default_factory=list)

# -------------------------------------------------------------------
# Controller Endpoint (Spring의 @PostMapping("/analyze"))
# -------------------------------------------------------------------

@router.post("/analyze", response_model=InteractionAnalyzeResponse)
def analyze_interaction(analyzeRequest: InteractionAnalyzeRequest):
    """
    처방약 + 영양제 상호작용 분석 (fallback).
    RAG/knowledge_embeddings 제거됨. interaction_rules 미매칭 시 빈 결과 반환.
    의사/약사 상담 권장.
    """
    try:
        # 2026-03: drug_easy_info, ingredient_efficacy, knowledge_embeddings 제거
        # 2차 분석용 RAG 없음. interaction_rules 1차 필터만 사용.

        # main_ingr_eng (영문 성분) 데이터가 없는 약품 처리 (Fallback)
        if not analyzeRequest.drug.ingredients:
            fallback_item = InteractionItem(
                nutrient="알 수 없음",
                contraindicated_drug_ingredient="알 수 없음",
                level="CAUTION",
                description=f"'{analyzeRequest.drug.name}' 의약품의 영문 주성분 정보가 DB에 적재되지 않아 정밀 검사를 수행할 수 없습니다.",
                action_guide="이 약을 복용하는 동안에는 영양제 복용 전 반드시 의사 또는 약사와 상담하시기 바랍니다.",
                sources=["PNN System Fallback"]
            )
            return InteractionAnalyzeResponse(
                interactions=[fallback_item]
            )

        # 영문 성분은 있지만 매칭되는 규칙이 없는 경우
        return InteractionAnalyzeResponse(
            interactions=[]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
