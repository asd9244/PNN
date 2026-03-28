"""
상호작용 분석 API (Case A) — Gemini 기반

흐름: Spring Boot가 drug + supplements JSON 전달 → 이 모듈이 Gemini 호출 → JSON 파싱 후 반환
등급: LLM이 생성하는 level은 WARNING / SAFE 만 (SYNERGY 등은 정규화 시 SAFE 등으로 처리).
      처방약 영문 성분이 비어 있을 때 등 시스템 폴백 응답에서는 CAUTION 을 쓸 수 있음.
후처리: WARNING은 개수 제한 없음, SAFE는 최대 2건까지만 반환.
"""
import json
import logging
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.services.gemini_service import chat

router = APIRouter()
logger = logging.getLogger(__name__)

# -------------------------------------------------------------------
# Data Transfer Objects (DTO)
# -------------------------------------------------------------------

class DrugInput(BaseModel):
    """클라이언트(Spring)에서 전달받는 처방약 정보 DTO"""
    id: Optional[str] = None
    name: str
    ingredients: list[str] = Field(default_factory=list, description="처방약 성분명 목록 (영문)")


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
    """분석된 개별 상호작용 결과 1건"""
    nutrient: str
    contraindicated_drug_ingredient: str
    level: str  # LLM 경로: WARNING | SAFE. 폴백: CAUTION 가능
    description: str
    action_guide: str
    sources: list[str] = Field(default_factory=list)


class InteractionAnalyzeResponse(BaseModel):
    """Spring Boot로 반환하는 응답 DTO"""
    interactions: list[InteractionItem] = Field(default_factory=list)


# -------------------------------------------------------------------
# 프롬프트 (Guardrails: 보수적 해석, 정보 통제)
# -------------------------------------------------------------------

SYSTEM_PROMPT = """당신은 약물-영양제 상호작용을 분석하는 전문 AI입니다.
[강력한 규칙 - 반드시 준수]
1. 알려진 문헌/근거가 없는 상호작용은 추측하지 말고, 해당 영양-약물 쌍은 interactions에 넣지 말거나 SAFE로만 분류하세요.
2. "치료", "완치", "예방" 등 의학적 효능을 암시하는 표현을 사용하지 마세요.
3. 출력은 반드시 유효한 JSON만 반환하세요. 마크다운 코드블록이나 설명 텍스트를 붙이지 마세요.
4. level은 반드시 WARNING 또는 SAFE 둘 중 하나만 사용하세요.
5. WARNING: 흡수 저하, 상승된 위험, 병용 시 조정이 필요하다고 알려진 경우 등 실제 위험이 의심될 때만 사용하세요.
6. SAFE: 문헌상 병용 시 특별한 문제가 없다고 알려진 경우에만 사용하세요. SAFE 항목은 최대 2개까지만 넣으세요(가장 대표적인 영양 성분 위주). WARNING 항목 개수에는 제한이 없습니다.

필수 JSON 형식:
{"interactions": [
  {"nutrient": "영문 성분명", "contraindicated_drug_ingredient": "처방약 성분명", "level": "WARNING 또는 SAFE", "description": "한국어 설명", "action_guide": "한국어 행동 지침", "sources": []}
]}
"""


def _build_user_prompt(drug_name: str, drug_ingredients: list[str], supplements: list[SupplementInput]) -> str:
    """처방약 이름·성분·영양제 목록을 문자열로 구성해 LLM에 넘길 프롬프트 생성."""
    drug_ingr_str = ", ".join(drug_ingredients) if drug_ingredients else "없음"
    supp_parts = []
    for s in supplements:
        nutr_str = ", ".join(n.name for n in s.nutrients)
        supp_parts.append(f"- {s.name}: {nutr_str}")
    supp_str = "\n".join(supp_parts) if supp_parts else "없음"
    return f"""[처방약]
이름: {drug_name}
성분(영문): {drug_ingr_str}

[기복용 영양제]
{supp_str}

위 처방약과 영양제 간의 상호작용을 분석하고, 지정된 JSON 형식으로만 응답하세요.
level은 WARNING과 SAFE만 사용하세요. SAFE는 최대 2개까지만 포함하세요. 위험이 의심되지 않으면 interactions를 빈 배열로 반환해도 됩니다."""


def _normalize_level(raw: str) -> str:
    """LLM이 구등급을 반환한 경우 WARNING/SAFE로만 정규화."""
    lv = (raw or "").upper().strip()
    if lv in ("WARNING", "CAUTION", "DANGER", "RISK"):
        return "WARNING"
    if lv in ("SAFE", "SYNERGY", "OK"):
        return "SAFE"
    # 빈 문자열·알 수 없는 값은 과도한 WARNING을 피하기 위해 SAFE로 처리
    return "SAFE"


def _normalize_and_limit_safe(items: list[InteractionItem]) -> list[InteractionItem]:
    """
    WARNING은 전부 유지, SAFE는 최대 2개만 유지(앞쪽 우선).
    """
    warnings: list[InteractionItem] = []
    safes: list[InteractionItem] = []
    for it in items:
        lv = (it.level or "").upper().strip()
        if lv == "WARNING":
            warnings.append(it.model_copy(update={"level": "WARNING"}))
        elif lv == "SAFE":
            safes.append(it.model_copy(update={"level": "SAFE"}))
    safes = safes[:2]
    return warnings + safes


def _parse_llm_response(text: str) -> list[InteractionItem]:
    """LLM 응답 텍스트에서 JSON 추출 후 InteractionItem 리스트로 변환. 파싱 실패 시 빈 리스트."""
    cleaned = text.replace("```json", "").replace("```", "").strip()  # 마크다운 코드블록 제거
    try:
        data = json.loads(cleaned)
        items = data.get("interactions", [])
        result = []
        for item in items:
            if isinstance(item, dict):
                raw_level = item.get("level", "SAFE")
                norm = _normalize_level(str(raw_level))
                result.append(InteractionItem(
                    nutrient=item.get("nutrient", ""),
                    contraindicated_drug_ingredient=item.get("contraindicated_drug_ingredient", ""),
                    level=norm,
                    description=item.get("description", ""),
                    action_guide=item.get("action_guide", ""),
                    sources=item.get("sources", []),
                ))
        return _normalize_and_limit_safe(result)
    except json.JSONDecodeError as e:
        logger.warning(f"LLM JSON 파싱 실패: {e}\n원문: {text[:500]}")
        return []


# -------------------------------------------------------------------
# Controller Endpoint
# -------------------------------------------------------------------

def _print_case_a_terminal(message: str) -> None:
    """
    uvicorn은 앱 logger.info가 콘솔에 안 붙는 경우가 많아, 개발용 터미널 확인은 print로 고정.
    (표준출력 + 즉시 flush → 디버거/리로드 환경에서도 보임)
    """
    print(message, flush=True)


def _print_case_a_result(out: InteractionAnalyzeResponse, note: str = "") -> None:
    payload = {"interactions": [i.model_dump() for i in out.interactions]}
    body = json.dumps(payload, ensure_ascii=False, indent=2)
    suffix = f" {note}" if note else ""
    _print_case_a_terminal(
        f"[Case A] ===== 분석 결과{suffix} ({len(out.interactions)}건) =====\n{body}"
    )
    logger.info(
        "[Case A] 응답 완료%s | 항목 %d건",
        suffix,
        len(out.interactions),
    )


@router.post("/analyze", response_model=InteractionAnalyzeResponse)
def analyze_interaction(analyzeRequest: InteractionAnalyzeRequest):
    """
    처방약 + 영양제 상호작용 분석 (Case A).
    정상 LLM 응답: WARNING / SAFE (SAFE는 최대 2건). 성분 없음 폴백: CAUTION.
    """
    try:
        _print_case_a_terminal(
            "[Case A] ===== 분석 시작 ===== "
            f"POST /api/v1/interaction/analyze | "
            f"약={analyzeRequest.drug.name} | id={analyzeRequest.drug.id or '-'} | "
            f"영문성분 {len(analyzeRequest.drug.ingredients)}개 | "
            f"영양제 제품 {len(analyzeRequest.supplements)}개"
        )

        # DB에 영문 성분이 없으면 정밀 분석 불가 → 상담 권장 메시지 반환
        if not analyzeRequest.drug.ingredients:
            fallback_item = InteractionItem(
                nutrient="알 수 없음",
                contraindicated_drug_ingredient="알 수 없음",
                level="CAUTION",
                description=f"'{analyzeRequest.drug.name}' 의약품의 영문 주성분 정보가 DB에 적재되지 않아 정밀 검사를 수행할 수 없습니다.",
                action_guide="이 약을 복용하는 동안에는 영양제 복용 전 반드시 의사 또는 약사와 상담하시기 바랍니다.",
                sources=["PNN System Fallback"]
            )
            out = InteractionAnalyzeResponse(interactions=[fallback_item])
            _print_case_a_result(out, note="(성분 없음 폴백)")
            return out

        _print_case_a_terminal("[Case A] Gemini 호출 중…")

        # 프롬프트 구성 후 Gemini 3 Flash 호출
        user_prompt = _build_user_prompt(
            analyzeRequest.drug.name,
            analyzeRequest.drug.ingredients,
            analyzeRequest.supplements,
        )
        response_text = chat(prompt=user_prompt, system=SYSTEM_PROMPT)
        logger.debug("LLM 원문: %s", response_text[:2000] if response_text else "")
        interactions = _parse_llm_response(response_text)

        out = InteractionAnalyzeResponse(interactions=interactions)
        _print_case_a_result(out, note="(LLM)")
        return out

    except ValueError as e:
        if "GEMINI_API_KEY" in str(e):
            raise HTTPException(status_code=503, detail="AI 서비스 설정 오류. GEMINI_API_KEY를 확인하세요.")
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        logger.exception("상호작용 분석 중 오류")
        raise HTTPException(status_code=500, detail=str(e))
