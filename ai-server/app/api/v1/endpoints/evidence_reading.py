"""
설명서 근거 문장 읽기 API (Case A, 근거 기반) — 로컬 LLM(Ollama, 기본 gemma4:e4b)

흐름: Spring이 약 설명서(식약처 주의사항)에서 찾은 원문 문장과 그 문장에 나온 영양 성분을 보냄
      → 로컬 모델이 문장 뜻을 읽고 성분마다 종류(CAUTION / NO_EFFECT / OTHER), 쉬운 설명, 할 일, 근거 구절을 답함
      → Spring이 근거 구절이 원문에 실제로 있는지 대조함 (이 모듈은 대조하지 않음)
기존 /analyze(기억으로 판단)와 달리, 주어진 문장 밖의 지식을 쓰지 않게 한다.
"""
import json
import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.core.config import settings
from app.services.ollama_llm_service import chat

router = APIRouter()
logger = logging.getLogger(__name__)

KINDS = {"CAUTION", "NO_EFFECT", "OTHER"}  # 모델이 고를 수 있는 종류
LLM_OPTIONS = {"temperature": 0, "num_ctx": 8192}  # 같은 입력에 같은 답 / 기본 대화 길이(짧음)로는 긴 답이 잘림


class EvidenceItem(BaseModel):
    """설명서 원문 문장 1개와 그 문장에 나온 영양 성분들"""
    id: int
    sentence: str
    nutrients: list[str] = Field(default_factory=list)


class EvidenceReadRequest(BaseModel):
    """Spring → Python 요청: 약 하나에 대한 근거 문장 목록"""
    drug_name: str
    items: list[EvidenceItem] = Field(default_factory=list)


class EvidenceReadResult(BaseModel):
    """문장 1개 × 성분 1개에 대한 읽기 결과"""
    id: int
    nutrient: str
    kind: str
    quote: str = ""
    explanation: str = ""
    action: str = ""


class EvidenceReadResponse(BaseModel):
    results: list[EvidenceReadResult] = Field(default_factory=list)


SYSTEM_PROMPT = """당신은 의약품 설명서(식약처 허가 '사용상의 주의사항')의 문장을 일반인에게 풀어 설명하는 보조자입니다.
[반드시 지킬 규칙]
1. 주어진 설명서 문장에 적힌 내용만 사용하세요. 문장에 없는 효과·위험·시간·용량을 덧붙이지 말고, 알고 있는 지식으로 보충하지 마세요.
2. 문장(id)마다, 그 문장의 '성분' 목록에 있는 영양 성분 각각에 대해 결과를 하나씩 만드세요.
3. kind는 셋 중 하나입니다.
   - CAUTION: 그 영양 성분을 약과 함께 먹을 때 피하거나 주의하라는 내용 (흡수 감소, 효과 변화, 시간 간격 필요 등)
   - NO_EFFECT: 그 영양 성분이 약에 영향이 없거나 의미 있는 변화가 없었다는 내용
   - OTHER: 위 둘이 아닌 영향 (예: 검사 결과에 영향을 줌)
   한 문장에 주의와 영향 없음이 섞여 있으면, 그 영양 성분을 보충제나 강화식품으로 먹는 경우에 해당하는 부분을 기준으로 고르세요.
4. quote: 판단의 근거가 된 부분을 문장에서 글자 그대로 복사하세요. 고치거나 줄이지 마세요. (가능하면 15~80자)
5. explanation: 일반인이 이해할 수 있는 한국어 한 문장. "치료", "예방" 같은 효능 표현은 쓰지 마세요.
6. action: 문장에 적힌 행동 지침만 짧게 (예: "최소 4시간 간격을 두고 복용"). 문장에 행동 지침이 없으면 빈 문자열 "".
7. 출력은 유효한 JSON만 반환하세요. 마크다운 코드블록이나 설명 텍스트를 붙이지 마세요.

출력 형식:
{"results": [{"id": 1, "nutrient": "철분", "kind": "CAUTION", "quote": "문장에서 그대로 복사한 부분", "explanation": "쉬운 설명", "action": "할 일 또는 빈 문자열"}]}
"""


def _build_user_prompt(req: EvidenceReadRequest) -> str:
    """약 이름과 (id, 성분, 문장) 목록을 프롬프트 문자열로 구성."""
    lines = [f"(id={it.id}, 성분: {', '.join(it.nutrients)}) {it.sentence}" for it in req.items]
    return f"[약]\n{req.drug_name}\n\n[설명서 문장]\n" + "\n".join(lines) + "\n\n위 형식의 JSON으로만 답하세요."


def _parse(text: str) -> list[EvidenceReadResult]:
    """모델 응답에서 JSON을 꺼내 결과 목록으로 변환. 형식이 틀린 항목은 버림 (Spring이 원문만 보여 줌)."""
    cleaned = (text or "").replace("```json", "").replace("```", "").strip()  # 마크다운 코드블록 제거
    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError as e:
        logger.warning("근거 읽기 JSON 파싱 실패: %s / 원문: %s", e, cleaned[:500])
        return []
    results = []
    for raw in data.get("results", []):
        if not isinstance(raw, dict):
            continue
        kind = str(raw.get("kind", "")).upper().strip()
        if kind not in KINDS:
            continue  # 정해진 종류가 아니면 버림
        try:
            results.append(EvidenceReadResult(
                id=int(raw.get("id")),
                nutrient=str(raw.get("nutrient", "")),
                kind=kind,
                quote=str(raw.get("quote", "")),
                explanation=str(raw.get("explanation", "")),
                action=str(raw.get("action", "") or ""),
            ))
        except (TypeError, ValueError):
            continue  # id가 숫자가 아니면 버림
    return results


@router.post("/read-evidence", response_model=EvidenceReadResponse)
def read_evidence(req: EvidenceReadRequest):
    """약 하나의 근거 문장들을 로컬 모델로 한 번에 읽는다 (약당 호출 1번)."""
    if not req.items:
        return EvidenceReadResponse()
    try:
        print(f"[Evidence] {settings.EVIDENCE_MODEL} 호출 | 약={req.drug_name} | 문장 {len(req.items)}개", flush=True)
        text = chat(prompt=_build_user_prompt(req), model=settings.EVIDENCE_MODEL, system=SYSTEM_PROMPT,
                    options=LLM_OPTIONS, format="json")
        out = EvidenceReadResponse(results=_parse(text))
        print(f"[Evidence] 결과 {len(out.results)}건\n" + json.dumps(out.model_dump(), ensure_ascii=False, indent=2), flush=True)
        return out
    except Exception as e:
        logger.exception("근거 읽기 중 오류")
        raise HTTPException(status_code=500, detail=str(e))
