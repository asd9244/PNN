from fastapi import APIRouter # 라우터 클래스 임포트

router = APIRouter() # 영양제 매칭 전용 라우터 생성

@router.post("/match") # 제품명 매칭 엔드포인트 정의
def match_supplement():
    # TODO: Product Name Matching
    return {"message": "Supplement Matching (Not Implemented)"} # 임시 반환값
