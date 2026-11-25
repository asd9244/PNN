from fastapi import APIRouter, UploadFile, File
import base64
from openai import OpenAI
import os
import re
import urllib.request
from urllib.error import URLError
import time
import subprocess
import threading

from app.core.config import settings
from app.services.ollama_llm_service import chat

router = APIRouter()

# 정리 단계: 같은 입력에 같은 답 / 긴 성분표도 잘리지 않게 / 답 길이 상한
REFINE_OPTIONS = {"temperature": 0, "num_ctx": 8192, "num_predict": 2048}

# OCR 출력 상한. 정상 성분표는 1,500토큰 안쪽이지만, 같은 글자를 끝없이 반복하는 사진이 오면
# 한도가 없을 때 OCR 서버가 몇십 분 붙잡혀 다른 요청까지 멈춘다 (이 한도면 약 14초 안에 끝남)
OCR_MAX_TOKENS = 4096

MODELS_DIR = r"C:\Users\jjong\Project_\PnN\pnn-project\ai-server\app\models"

# 사진 여러 장이 동시에 와도 OCR 서버는 한 번만 켜지게 (켜는 동안 다른 요청은 기다렸다가 켜진 서버를 씀)
_OCR_START_LOCK = threading.Lock()

# 라벨의 아래 첨자 숫자(B₆, B₁₂, D₃)를 보통 숫자로 — 그대로 두면 상호작용 검사가 B6·B12로 알아보지 못함
SUBSCRIPT_DIGITS = str.maketrans("₀₁₂₃₄₅₆₇₈₉", "0123456789")
# 함량: "1,000 mg"처럼 쉼표 있는 숫자도 한 덩어리로 + 바로 뒤 단위
AMOUNT = re.compile(r"(\d[\d,]*(?:\.\d+)?)\s*([a-zA-Z%µμ]+)?")


def _split_name_value(line: str):
    """'이름: 함량' 줄을 이름과 함량 부분으로 나눔. 함량이 숫자로 시작하지 않으면 (None, None)
    (예: 첨가물 줄 "Magnesium Stearate: <2%"는 1회 함량이 아니므로 뺌)."""
    if ":" in line:
        # 첫 ':' 앞이 이름 — 이름 속 숫자("riboflavin 5'-phosphate", "Vitamin B12")를 함량으로 읽지 않게
        name, value = line.split(":", 1)
    else:
        # 정리 모델이 형식을 안 지킨 줄: 예전처럼 이름 뒤 첫 숫자부터 함량으로 봄
        m = re.search(r"(.+?)\s+(\d.*)", line)
        if not m:
            return None, None
        name, value = m.group(1), m.group(2)
    name, value = name.strip(), value.strip()
    if not name or not AMOUNT.match(value):
        return None, None
    return name, value


def parse_nutrients(refined_text: str) -> list[dict]:
    """정리 모델이 쓴 '이름: 함량' 줄들 → [{name, amount, unit}] (같은 이름은 하나만)."""
    nutrition_data = {}
    for line in refined_text.translate(SUBSCRIPT_DIGITS).split("\n"):
        name, value = _split_name_value(line)
        if name is None:
            continue
        m = AMOUNT.match(value)
        nutrition_data[name] = {
            "name": name,
            "amount": float(m.group(1).replace(",", "")),  # 쉼표를 지우고 숫자로 (1,000 → 1000)
            "unit": m.group(2) or "",
        }
    return list(nutrition_data.values())


def encode_image(image_bytes: bytes):
    return base64.b64encode(image_bytes).decode('utf-8')

def is_server_running(url="http://127.0.0.1:8090/health"):
    try:
        urllib.request.urlopen(url, timeout=1)
        return True
    except URLError:
        return False

def start_paddleocr_server():
    with _OCR_START_LOCK:
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

# async def가 아닌 def: FastAPI가 따로 돌려서, OCR·정리 모델을 기다리는 동안에도 서버가 다른 요청의 결과를 돌려줄 수 있음
# (async def 안에서 이렇게 기다리면 그동안 서버 전체가 멈춤)
@router.post("/extract")
def extract_nutrients(image: UploadFile = File(...)):
    # 1. 서버 실행 확인 및 자동 시작
    paddle_process = None
    try:
        if not is_server_running():
            paddle_process = start_paddleocr_server()
            
        # 2. 이미지 읽기 및 인코딩
        image_bytes = image.file.read()
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
                temperature=0.0,
                max_tokens=OCR_MAX_TOKENS
            )
            raw_ocr_text = ocr_response.choices[0].message.content.strip()
            if ocr_response.choices[0].finish_reason == "length":
                print(f"⚠️ OCR 출력이 상한({OCR_MAX_TOKENS}토큰)에 닿아 잘림 — 같은 글자 반복 가능성", flush=True)
            print("\n" + "=" * 60)
            print("[Paddle OCR 모델 분석 결과]")
            print("=" * 60)
            print(raw_ocr_text)
            print("=" * 60 + "\n")
        except Exception as e:
            return {"error": f"PaddleOCR 호출 실패: {e}"}

        # 4. [STEP 2] 로컬 모델(gemma)로 텍스트 정제 — 줄 정리만 하면 되므로 '생각' 단계를 끔 (켜면 4배 느림)
        refine_prompt = f"""
        아래 영양 성분표 텍스트에서 '영양소 이름'과 '함량'만 찾아서 한 줄에 하나씩 나열해줘.
        % 정보나 기타 정보는 모두 제외하고 오직 '이름: 함량' 형식으로만 적어줘. 
        절대 요약하지 말고 모든 성분을 다 적어.

        [원본 텍스트]
        {raw_ocr_text}
        """

        try:
            refined_text = chat(prompt=refine_prompt, model=settings.OCR_REFINE_MODEL,
                                options=REFINE_OPTIONS, think=False)
            print("\n" + "=" * 60)
            print(f"[정리 모델({settings.OCR_REFINE_MODEL}) 결과]")
            print("=" * 60)
            print(refined_text)
            print("=" * 60 + "\n")
        except Exception as e:
            return {"error": f"정리 모델 호출 실패: {e}"}

        # 5. [STEP 3] '이름: 함량' 줄을 성분 목록(JSON)으로
        nutrients = parse_nutrients(refined_text)

        return {
            "name": image.filename,
            "nutrients": nutrients
        }
    finally:
        # FastAPI 서버가 실행되는 동안 PaddleOCR 서버는 유지하는 것이 성능에 좋음
        # 테스트 환경이나 단발성 실행 시에만 프로세스 종료
        # 여기서는 유지 전략을 사용하므로 종료 로직 생략 (필요시 lifespan에 구현)
        # 정리 모델(gemma)은 상호작용 근거 읽기와 같이 쓰므로 메모리에서 내리지 않음 (예전 qwen은 매번 내렸음)
        pass


