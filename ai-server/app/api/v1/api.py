from fastapi import APIRouter # 라우터 그룹화를 위한 클래스
from app.api.v1.endpoints import supplement_recognition, interaction, recommendation, health # 각 기능별 엔드포인트 모듈 임포트

api_router = APIRouter() # 메인 API 라우터 생성

api_router.include_router(health.router, prefix="/health", tags=["health"]) # 헬스 체크 라우터 등록
api_router.include_router(supplement_recognition.router, prefix="/supplement", tags=["supplement_recognition"]) # 영양제 인식 라우터 등록
api_router.include_router(interaction.router, prefix="/interaction", tags=["interaction"]) # 상호작용 분석 라우터 등록
api_router.include_router(recommendation.router, prefix="/recommendation", tags=["recommendation"]) # 추천 관련 라우터 등록
# api_router.include_router(supplement.router, prefix="/supplement", tags=["supplement"]) # [보류] 영양제 DB 매칭 라우터 (한국 데이터 한정 사유)
