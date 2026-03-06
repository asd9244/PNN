from fastapi import APIRouter, UploadFile, File # 파일 업로드를 위한 클래스 임포트

router = APIRouter() # OCR 전용 라우터 생성

@router.post("/extract") # OCR 추출 엔드포인트 정의
async def extract_nutrients(image: UploadFile = File(...)): # 이미지 파일 수신
    # TODO: Google Vision API + GPT-4o 연결
    return {"message": "OCR Extraction (Not Implemented)"} # 임시 반환값
