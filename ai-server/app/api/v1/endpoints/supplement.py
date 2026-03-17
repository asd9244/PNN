from fastapi import APIRouter # 라우터 클래스 임포트

router = APIRouter() # 영양제 매칭 전용 라우터 생성

"""
[임시 보류 (2026-03-17)]
현재 영양제 데이터베이스가 한국 영양제 데이터에 국한되어 있어, 
글로벌(해외직구 등) 영양제 검색/매칭 시 활용도가 제한적입니다.
따라서 이 기능(제품명 검색/매칭)은 기본 핵심 기능(OCR 성분 추출 등)을 모두 완성한 후, 
추후 고도화 단계에서 재검토 및 구현하기 위해 주석 처리해 둡니다.

@router.post("/match") # 제품명 매칭 엔드포인트 정의
def match_supplement():
    # TODO: Product Name Matching
    return {"message": "Supplement Matching (Not Implemented)"} # 임시 반환값
"""
