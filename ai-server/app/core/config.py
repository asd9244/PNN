import os # OS 환경변수 접근을 위한 모듈
from pydantic_settings import BaseSettings # Pydantic 기반 설정 관리를 위한 클래스
# application.properties 설정과 같은 역할을 함.

class Settings(BaseSettings):
    PROJECT_NAME: str = "PNN AI Server" # 프로젝트 이름
    API_V1_STR: str = "/api/v1" # API 버전 접두사
    
    # 보안: .env에서 로드
    OPENAI_API_KEY: str = "" # OpenAI API 키 (OCR, LLM용)
    DATABASE_URL: str = "postgresql://postgres:1234@localhost:5432/pnn-db" # DB 연결 URL (기본값, 환경변수 우선)

    class Config:
        env_file = ".env" # .env 파일 경로 설정
        case_sensitive = True # 환경변수 대소문자 구분

settings = Settings() # 설정 인스턴스 생성
