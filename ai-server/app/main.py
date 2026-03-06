from fastapi import FastAPI # FastAPI 프레임워크 임포트
from app.api.v1.api import api_router # API 라우터 설정 임포트
from app.core.config import settings # 프로젝트 설정 임포트

app = FastAPI(
    title=settings.PROJECT_NAME, # 프로젝트 이름 설정
    openapi_url=f"{settings.API_V1_STR}/openapi.json" # OpenAPI 문서 경로 설정
)

app.include_router(api_router, prefix=settings.API_V1_STR) # API 라우터 등록 및 접두사 설정

@app.get("/") # 루트 경로 핸들러
def root():
    return {"message": "PNN AI Server is running"} # 서버 실행 확인 메시지 반환
