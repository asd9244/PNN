from fastapi import APIRouter, HTTPException
from app.models.recommendation_models import RecommendationAnalyzeRequest, RecommendationAnalyzeResponse, RecommendedNutrient
from app.services.ollama_llm_service import build_safe_recommendation_prompt, chat
import json
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

@router.post("/analyze-safe", response_model=RecommendationAnalyzeResponse)
def recommend_safe_nutrients(request: RecommendationAnalyzeRequest):
    """
    처방약과 금기 성분 목록을 기반으로 안전한 영양 성분 추천 (Case B)
    """
    try:
        # 1. 프롬프트 생성 (Step 4에서 만든 보수적 프롬프트)
        system_prompt, user_prompt = build_safe_recommendation_prompt(
            patient_drugs=request.patient_drugs,
            contraindicated_nutrients=request.contraindicated_nutrients
        )

        # 2. LLM 호출 (JSON 출력 보장)
        # TODO: 로컬에서 성능이 더 좋은 모델(exaone3.5 등)이 있다면 해당 모델명으로 변경 권장
        llm_response_text = chat(prompt=user_prompt, system=system_prompt, model="exaone3.5:7.8b")

        # 3. JSON 파싱
        # LLM이 markdown 블록(```json ... ```)을 붙일 경우를 대비한 간단한 클렌징
        cleaned_text = llm_response_text.replace("```json", "").replace("```", "").strip()
        parsed_json = json.loads(cleaned_text)
        
        raw_recommendations = parsed_json.get("recommended_nutrients", [])

        # 4. 사후 검증 (Post-Filter) : 환각 차단 핵심 로직
        # LLM이 금기 성분(contraindicated_nutrients)을 추천하는 치명적 실수를 걸러냄
        safe_recommendations = []
        contraindicated_lower = [c.lower().strip() for c in request.contraindicated_nutrients]

        for item in raw_recommendations:
            nutrient_name = item.get("name", "").strip()
            reason = item.get("reason_kr", "").strip()
            
            if not nutrient_name or not reason:
                continue

            # 대소문자 무시하고 금기 성분 목록과 일치/포함되는지 검사
            is_contraindicated = any(c in nutrient_name.lower() or nutrient_name.lower() in c for c in contraindicated_lower)
            
            if is_contraindicated:
                logger.warning(f"[Hallucination Alert] LLM이 금기 성분을 추천하려 했습니다: {nutrient_name}. 해당 추천을 폐기합니다.")
                continue # 금기 성분이면 결과 배열에 넣지 않고 버림(Drop)
            
            safe_recommendations.append(RecommendedNutrient(name=nutrient_name, reason_kr=reason))

        # 5. 최종 안전한 결과만 반환
        return RecommendationAnalyzeResponse(recommended_nutrients=safe_recommendations)

    except json.JSONDecodeError as e:
        logger.error(f"LLM JSON 파싱 에러: {e}\n원본 텍스트: {llm_response_text}")
        raise HTTPException(status_code=500, detail="AI 추천 결과를 처리하는 중 오류가 발생했습니다. (JSON 형식 오류)")
    except Exception as e:
        logger.error(f"AI 추천 처리 중 예상치 못한 오류: {e}")
        raise HTTPException(status_code=500, detail="AI 추천 처리 중 내부 서버 오류가 발생했습니다.")


