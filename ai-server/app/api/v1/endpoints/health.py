from fastapi import APIRouter # 라우터 클래스 임포트

router = APIRouter() # 헬스 체크 전용 라우터 생성

@router.get("") # GET /health 엔드포인트 정의
def health_check():
    return {"status": "ok", "server": "PNN AI Server"} # Spring에서 확인할 응답
