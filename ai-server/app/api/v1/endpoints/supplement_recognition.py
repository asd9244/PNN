from fastapi import APIRouter, UploadFile, File
import base64
from openai import OpenAI
import os
import re
import urllib.request
from urllib.error import URLError
import time
import subprocess
import requests

router = APIRouter()

MODELS_DIR = r"C:\Users\jjong\Project_\PnN\pnn-project\ai-server\app\models"

def encode_image(image_bytes: bytes):
    return base64.b64encode(image_bytes).decode('utf-8')

def is_server_running(url="http://127.0.0.1:8090/health"):
    try:
        urllib.request.urlopen(url, timeout=1)
        return True
    except URLError:
        return False

def start_paddleocr_server():
    if is_server_running():
        return None
    
    print("▶️ PaddleOCR 서버 시작 중...")
    llama_exec = os.path.join(MODELS_DIR, "llama-server.exe")
    server_command = [
        llama_exec,
        "-m", "PaddleOCR-VL-1.5.gguf",
        "--mmproj", "PaddleOCR-VL-1.5-mmproj.gguf",
        "-ngl", "99",
        "--port", "8090"
    ]
    process = subprocess.Popen(
        server_command,
        cwd=MODELS_DIR,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )
    
    # 서버 준비 대기
    start_time = time.time()
    while time.time() - start_time < 60:
        if is_server_running():
            print("✅ PaddleOCR 서버 준비 완료")
            return process
        time.sleep(1)
        
    process.terminate()
    raise Exception("PaddleOCR 서버 시동 시간 초과")

@router.post("/extract")
async def extract_nutrients(image: UploadFile = File(...)):
    # 1. 서버 실행 확인 및 자동 시작
    paddle_process = None
    try:
        if not is_server_running():
            paddle_process = start_paddleocr_server()
            
        # 2. 이미지 읽기 및 인코딩
        image_bytes = await image.read()
        base64_image = encode_image(image_bytes)

        # 3. [STEP 1] PaddleOCR 텍스트 스캔
        ocr_client = OpenAI(base_url="http://127.0.0.1:8090/v1", api_key="sk-no-key-required")
        try:
            ocr_response = ocr_client.chat.completions.create(
                model="paddleocr-vl",
                messages=[{
                    "role": "user",
                    "content": [
                        {"type": "text",
                         "text": "OCR. Please output text using plain Unicode characters directly (e.g., µ, α):"},
                        {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}}
                    ]
                }],
                temperature=0.0
            )
            raw_ocr_text = ocr_response.choices[0].message.content.strip()
        except Exception as e:
            return {"error": f"PaddleOCR 호출 실패: {e}"}

        # 4. [STEP 2] Qwen 7B 텍스트 정제
        json_client = OpenAI(base_url="http://127.0.0.1:11434/v1", api_key="sk-no-key-required")
        
        refine_prompt = f"""
        아래 영양 성분표 텍스트에서 '영양소 이름'과 '함량'만 찾아서 한 줄에 하나씩 나열해줘.
        % 정보나 기타 정보는 모두 제외하고 오직 '이름: 함량' 형식으로만 적어줘. 
        절대 요약하지 말고 모든 성분을 다 적어.

        [원본 텍스트]
        {raw_ocr_text}
        """

        try:
            refine_response = json_client.chat.completions.create(
                model="qwen2.5:7b",
                messages=[{"role": "user", "content": refine_prompt}],
                temperature=0.0,
                max_tokens=2048
            )
            refined_text = refine_response.choices[0].message.content.strip()
        except Exception as e:
            return {"error": f"Qwen 정제 호출 실패: {e}"}

        # 5. [STEP 3] Python 정규식으로 JSON 변환
        nutrition_data = {}
        lines = refined_text.split('\n')
        for line in lines:
            match = re.search(r"([^:]+?)[:\s]+([\d\.]+\s*(?:mg|mcg|µg|g|kcal|%|IU)?)", line, re.IGNORECASE)
            if match:
                key = match.group(1).strip()
                value = match.group(2).strip()
                
                amount_match = re.search(r"([\d\.]+)", value)
                unit_match = re.search(r"([a-zA-Z%µ]+)", value)
                
                nutrition_data[key] = {
                    "name": key,
                    "amount": float(amount_match.group(1)) if amount_match else None,
                    "unit": unit_match.group(1) if unit_match else ""
                }

        nutrients = list(nutrition_data.values())

        return {
            "name": image.filename,
            "nutrients": nutrients
        }
    finally:
        # FastAPI 서버가 실행되는 동안 PaddleOCR 서버는 유지하는 것이 성능에 좋음
        # 테스트 환경이나 단발성 실행 시에만 프로세스 종료
        # 여기서는 유지 전략을 사용하므로 종료 로직 생략 (필요시 lifespan에 구현)
        
        # Qwen 모델 VRAM 즉시 반환
        try:
            requests.post(
                "http://127.0.0.1:11434/api/generate",
                json={"model": "qwen2.5:7b", "keep_alive": 0},
                timeout=1
            )
        except:
            pass


