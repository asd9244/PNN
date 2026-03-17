import os
from pathlib import Path

from pydantic_settings import BaseSettings

# ai-server/.env 경로 (실행 위치와 무관하게 고정)
_ENV_PATH = Path(__file__).resolve().parent.parent.parent / ".env"


class Settings(BaseSettings):
    PROJECT_NAME: str = "PNN AI Server"
    API_V1_STR: str = "/api/v1"

    # GEMINI_API_KEY: https://aistudio.google.com/apikey 에서 발급
    GEMINI_API_KEY: str = ""
    DATABASE_URL: str = "postgresql://postgres:1234@localhost:5432/pnn-db"

    class Config:
        env_file = str(_ENV_PATH) if _ENV_PATH.exists() else ".env"
        case_sensitive = True

settings = Settings() # 설정 인스턴스 생성
