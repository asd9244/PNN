"""
안전 영양제 추천 API (Case B) — Gemini 3 Flash 기반

흐름: Spring Boot가 patient_drugs(약품명·성분) 전달 → Gemini 호출 → JSON 파싱 후 추천 목록 반환
역할: 기복용 처방약과 상호작용이 적고, 보충에 도움이 되는 영양 성분을 추천
Guardrails: 금기 성분은 절대 추천하지 않음. Post-Filter로 환각 차단.
"""
import json
import logging

from fastapi import APIRouter, HTTPException

from app.schemas.recommendation import (
    RecommendationAnalyzeRequest,
    RecommendationAnalyzeResponse,
    RecommendedNutrient,
)
from app.services.gemini_service import chat

router = APIRouter()
logger = logging.getLogger(__name__)

# -------------------------------------------------------------------
# 프롬프트 (Guardrails: 금기 성분 절대 추천 금지)
# -------------------------------------------------------------------

SYSTEM_PROMPT = """당신은 영양제 및 약물 상호작용을 분석하는 전문 AI 어시스턴트입니다.
당신의 임무는 환자가 현재 복용 중인 약물을 고려하여, 안전한 영양 성분 2가지를 추천하는 것입니다.

[강력한 추천 규칙 - 반드시 준수할 것]
1. [금기 영양 성분] 목록에 포함된 성분은 절대 추천하지 마십시오. 이는 환자의 안전과 직결됩니다.
2. 추천하는 영양 성분은 [현재 복용 중인 약물]과 알려진 상호작용이 없어야 합니다.
3. 추천 사유 작성 시 '치료', '완치', '예방'과 같은 의학적 효능을 암시하는 단어를 절대 사용하지 마십시오. 대신 '보편적인 건강 유지에 도움을 줄 수 있음'과 같이 매우 보수적으로 작성해야 합니다.
4. 당신의 출력은 마크다운 코드 블록(```json)이나 다른 설명 텍스트 없이, 반드시 올바른 JSON 형식(VALID JSON format)이어야 합니다.
5. 성분명(name)은 반드시 영문(예: Magnesium, Vitamin D)으로 작성하고, 추천 사유(reason_kr)는 한국어로 작성하십시오.

필수 JSON 출력 형식:
{
  "recommended_nutrients": [
    {
      "name": "영문 성분명",
      "reason_kr": "한국어로 작성된 보수적이고 일상적인 추천 사유"
    }
  ]
}
"""


def _build_user_prompt(patient_drugs: list[str], contraindicated_nutrients: list[str]) -> str:
    """환자 복용 약품·금기 영양제 목록을 문자열로 구성해 LLM에 넘길 프롬프트 생성."""
    drugs_str = ", ".join(patient_drugs) if patient_drugs else "없음"
    contra_str = ", ".join(contraindicated_nutrients) if contraindicated_nutrients else "없음"
    return (
        f"[현재 복용 중인 약물 (Patient Drugs)]\n{drugs_str}\n\n"
        f"[금기 영양 성분 (MUST NOT RECOMMEND)]\n{contra_str}\n\n"
        "위 규칙을 엄격히 준수하여, 환자에게 안전하고 보편적인 영양 성분 2가지를 지정된 JSON 형식으로 추천해 주십시오."
    )


@router.post("/analyze-safe", response_model=RecommendationAnalyzeResponse)
def recommend_safe_nutrients(request: RecommendationAnalyzeRequest):
    """
    처방약과 금기 성분 목록을 기반으로 안전한 영양 성분 추천 (Case B).
    Gemini 3 Flash 사용. 환각 방지 Post-Filter 적용.
    """
    try:
        # 1. 프롬프트 생성
        user_prompt = _build_user_prompt(
            patient_drugs=request.patient_drugs,
            contraindicated_nutrients=request.contraindicated_nutrients,
        )

        # 2. 프롬프트 구성 후 Gemini 3 Flash 호출
        llm_response_text = chat(prompt=user_prompt, system=SYSTEM_PROMPT)

        # 3. LLM 응답에서 JSON 추출 후 파싱 (마크다운 코드블록 제거)
        cleaned_text = llm_response_text.replace("```json", "").replace("```", "").strip()
        parsed_json = json.loads(cleaned_text)
        raw_recommendations = parsed_json.get("recommended_nutrients", [])

        # 4. Post-Filter: 환각 차단 — 금기 성분을 추천한 경우 폐기
        safe_recommendations = []
        contraindicated_lower = [c.lower().strip() for c in request.contraindicated_nutrients]

        for item in raw_recommendations:
            nutrient_name = item.get("name", "").strip()
            reason = item.get("reason_kr", "").strip()

            if not nutrient_name or not reason:
                continue

            is_contraindicated = any(
                c in nutrient_name.lower() or nutrient_name.lower() in c
                for c in contraindicated_lower
            )
            if is_contraindicated:
                logger.warning(
                    "[Hallucination Alert] LLM이 금기 성분을 추천하려 했습니다: %s. 해당 추천을 폐기합니다.",
                    nutrient_name,
                )
                continue

            safe_recommendations.append(RecommendedNutrient(name=nutrient_name, reason_kr=reason))

        return RecommendationAnalyzeResponse(recommended_nutrients=safe_recommendations)

    except json.JSONDecodeError as e:
        logger.error("LLM JSON 파싱 에러: %s", e)
        raise HTTPException(
            status_code=500,
            detail="AI 추천 결과를 처리하는 중 오류가 발생했습니다. (JSON 형식 오류)",
        )
    except ValueError as e:
        if "GEMINI_API_KEY" in str(e):
            raise HTTPException(status_code=503, detail="AI 서비스 설정 오류. GEMINI_API_KEY를 확인하세요.")
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        logger.exception("AI 추천 처리 중 예상치 못한 오류")
        raise HTTPException(status_code=500, detail="AI 추천 처리 중 내부 서버 오류가 발생했습니다.")
