"""
안전 영양제 추천 API (Case B) — Gemini 3 Flash 기반

흐름: Spring Boot가 복용 약 목록(제품명·영문 성분) 전달 → Gemini 호출 → JSON 파싱 후 반환
역할: 기복용 약물과 상호작용 우려가 없는 보편적인 영양 성분을 보수적으로 추천
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
# 프롬프트 (기저 질환 + 약물 기반 안전 영양제 추천)
# -------------------------------------------------------------------

SYSTEM_PROMPT = """당신은 사용자의 [기저 질환(병명)]과 [현재 복용 중인 처방약 성분]을 종합적으로 분석하여, 안전하고 도움이 되는 '영양 보충제 성분'을 추천하는 전문 약학/영양학 AI 보조자입니다.

[강력한 규칙 - 반드시 준수]
1. 최우선 순위 (안전성): 사용자가 입력한 [복용 중인 약물]과 알려진 약물-영양소 상호작용(흡수 저하, 독성 증가, 대사 효소 저해 등)이 단 하나라도 의심되는 성분은 절대 추천하지 마세요.
2. 질환 연관성: 상호작용이 없으면서도, 사용자의 [기저 질환]으로 인해 저하될 수 있는 일상적인 컨디션 관리에 도움이 되는 성분을 최대 2개만 제안하세요.
3. 보수적 접근: 100% 안전하다고 확신할 수 있는 성분이 없거나 약물 정보가 불충분할 경우, 억지로 추천하지 말고 빈 배열([])을 반환하세요.
4. 의료법 준수: "치료", "완치", "예방", "증상 개선" 등 질병과 관련된 의학적 효능을 암시하는 단어를 절대 사용하지 마세요. 반드시 "일상적인 건강 유지", "영양 보급", "활력 지원" 등의 보수적인 표현만 사용하세요.
5. 포맷 제한: 출력은 반드시 유효한 JSON 형식만 반환해야 하며, 마크다운 코드블록(```json 등)이나 기타 인사말, 설명 텍스트를 절대 포함하지 마세요.

[필수 JSON 출력 형식]
{
  "interaction_analysis": "입력된 약물과 질환을 바탕으로 한 내부적인 상호작용 및 안전성 검토 과정 (약 1~2문장으로 간략히 작성. LLM의 추론 정확도를 높이기 위한 용도)",
  "recommended_nutrients": [
    {
      "name_en": "영문 성분명 (예: Magnesium)",
      "name_kr": "한글 성분명 (예: 마그네슘)",
      "reason": "한국어로 작성된 보수적 추천 사유 (예: '해당 질환 시 소모되기 쉬운 영양소의 보급을 돕습니다.')",
      "precaution": "일반적인 복용 주의사항 (예: '위장장애가 있을 수 있으니 식후 복용을 권장합니다.')"
    }
  ]
}
"""

def _build_user_prompt(condition: str, patient_drugs: list[str]) -> str:
    """환자 기저 질환 및 복용 약 목록을 문자열로 구성해 LLM에 넘길 프롬프트 생성."""
    cond_str = condition if condition else "알려진 기저 질환 없음"
    drugs_str = ", ".join(patient_drugs) if patient_drugs else "없음"
    return f"""[기저 질환(병명)]
{cond_str}

[복용 중인 약물]
{drugs_str}

위 정보를 종합적으로 고려하여, 안전하게 병용할 수 있는 영양 성분을 지정된 JSON 형식으로 추천해 주세요."""

@router.post("/analyze-safe", response_model=RecommendationAnalyzeResponse)
def recommend_safe_nutrients(request: RecommendationAnalyzeRequest):
    """
    Case B: 질환 및 복용 약 기반 안전 영양 성분 추천.
    """
    try:
        user_prompt = _build_user_prompt(request.condition, request.patient_drugs)

        llm_response_text = chat(prompt=user_prompt, system=SYSTEM_PROMPT)
        print(f"[LLM 추천 응답]\n{llm_response_text}")

        cleaned_text = llm_response_text.replace("```json", "").replace("```", "").strip()
        parsed_json = json.loads(cleaned_text)
        
        interaction_analysis = parsed_json.get("interaction_analysis", "")
        raw_recommendations = parsed_json.get("recommended_nutrients", [])

        out: list[RecommendedNutrient] = []
        for item in raw_recommendations:
            if len(out) >= 2:  # 최대 3개 제한 보장
                break
            name_en = item.get("name_en", "").strip()
            name_kr = item.get("name_kr", "").strip()
            reason = item.get("reason", "").strip()
            precaution = item.get("precaution", "").strip()
            
            if not name_en or not reason:
                continue
                
            out.append(RecommendedNutrient(
                name_en=name_en, 
                name_kr=name_kr, 
                reason=reason, 
                precaution=precaution
            ))

        return RecommendationAnalyzeResponse(
            interaction_analysis=interaction_analysis,
            recommended_nutrients=out
        )

    except json.JSONDecodeError as e:
        logger.error(f"LLM JSON 파싱 에러: {e}\n원문: {llm_response_text[:500]}")
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
