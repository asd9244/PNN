"""
상호작용 분석 API — RAG + LLM
"""
import json
import re
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional

from app.services.ollama_embedding_service import get_embedding
from app.services.vector_store import search_embeddings
from app.services.ollama_llm_service import chat

router = APIRouter()

# RAG 검색 시 사용할 source (drug_easy_info + ingredient_efficacy)
RAG_SOURCES = ["drug_easy_info", "ingredient_efficacy"]


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
    drug_ingredient: str
    level: str  # SAFE | CAUTION | WARNING | SYNERGY
    description: str
    action: str
    sources: list[str] = Field(default_factory=list)


class InteractionAnalyzeResponse(BaseModel):
    interactions: list[InteractionItem] = Field(default_factory=list)
    rag_context_count: int = 0


def _build_rag_query(drug: DrugInput, supplements: list[SupplementInput]) -> str:
    """RAG 검색용 쿼리 구성"""
    parts = []
    if drug.ingredients:
        parts.append("처방약 성분: " + ", ".join(drug.ingredients))
    nutrient_names = []
    for sup in supplements:
        for n in sup.nutrients:
            nutrient_names.append(n.name)
    if nutrient_names:
        parts.append("영양제 성분: " + ", ".join(nutrient_names))
    if not parts:
        parts.append(f"약품 {drug.name}과 영양제 상호작용")
    return " ".join(parts) + " 상호작용 주의사항"


def _parse_llm_response(text: str) -> list[dict]:
    """LLM 응답에서 interactions 추출 (JSON 또는 유사 형식)"""
    # JSON 블록 추출 시도
    json_match = re.search(r"\[[\s\S]*?\]", text)
    if json_match:
        try:
            data = json.loads(json_match.group())
            if isinstance(data, list):
                return data
        except json.JSONDecodeError:
            pass
    # 마크다운 테이블 등 파싱 실패 시 빈 목록
    return []


@router.post("/analyze", response_model=InteractionAnalyzeResponse)
def analyze_interaction(req: InteractionAnalyzeRequest):
    """
    처방약 + 영양제 상호작용 분석 (RAG + LLM).
    knowledge_embeddings에서 drug_easy_info, ingredient_efficacy 검색 후 LLM 분석.
    """
    try:
        # 1. RAG 쿼리 구성 및 검색
        query = _build_rag_query(req.drug, req.supplements)
        query_embedding = get_embedding(query)
        rag_results = search_embeddings(
            query_embedding,
            sources=RAG_SOURCES,
            top_k=10,
            min_similarity=0.3,
        )

        # 2. 컨텍스트 구성
        context_parts = []
        for i, r in enumerate(rag_results[:8], 1):
            context_parts.append(f"[문서{i}] (출처:{r['source']})\n{r['content']}")

        context = "\n\n".join(context_parts) if context_parts else "관련 문서 없음"

        # 3. LLM 프롬프트
        drug_desc = f"처방약: {req.drug.name}"
        if req.drug.ingredients:
            drug_desc += f" (성분: {', '.join(req.drug.ingredients)})"

        supp_desc = []
        for s in req.supplements:
            nstr = ", ".join(n.name for n in s.nutrients) if s.nutrients else "성분 미상"
            supp_desc.append(f"- {s.name}: {nstr}")
        supp_text = "\n".join(supp_desc) if supp_desc else "영양제 정보 없음"

        system = """당신은 약물-영양제 상호작용 전문가입니다.
제공된 참고 문서를 바탕으로, 처방약 성분과 영양제 성분 간 상호작용을 분석하세요.
반드시 JSON 배열 형식으로만 답변하세요. 다른 설명 없이 배열만 출력합니다.

형식: [{"nutrient":"영양성분명","drug_ingredient":"처방약성분명","level":"SAFE|CAUTION|WARNING|SYNERGY","description":"설명","action":"권고사항","sources":["출처1"]}]
관련 상호작용이 없으면 빈 배열 []을 반환하세요."""

        prompt = f"""## 참고 문서
{context}

## 분석 대상
{drug_desc}

영양제:
{supp_text}

## 요청
위 참고 문서를 바탕으로 처방약과 영양제 간 상호작용을 분석하고, JSON 배열만 출력하세요."""

        # 4. LLM 호출
        llm_response = chat(prompt, system=system)

        # 5. 응답 파싱
        interactions_raw = _parse_llm_response(llm_response)
        interactions = []
        for item in interactions_raw:
            if isinstance(item, dict):
                interactions.append(
                    InteractionItem(
                        nutrient=item.get("nutrient", ""),
                        drug_ingredient=item.get("drug_ingredient", ""),
                        level=item.get("level", "CAUTION"),
                        description=item.get("description", ""),
                        action=item.get("action", ""),
                        sources=item.get("sources", []),
                    )
                )

        return InteractionAnalyzeResponse(
            interactions=interactions,
            rag_context_count=len(rag_results),
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
