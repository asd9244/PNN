from openai import OpenAI
import base64
import json
import re
import subprocess
import time
import urllib.request
from urllib.error import URLError
import requests
import os

# ==========================================
# [설정] 경로 세팅 (사용자 환경에 맞춤)
# ==========================================
MODELS_DIR = r"C:\Users\jjong\Project_\PnN\pnn-project\ai-server\app\models"
IMAGE_PATH = r"C:\Users\jjong\Project_\PnN\pnn-project\ai-server\app\pnn-test-img\영양제03_성분.jpg"


def encode_image(image_path):
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')


def wait_for_server(url, timeout=60):
    print(f" PaddleOCR 서버 시동 중... (VRAM 적재 대기, 최대 {timeout}초)")
    start_time = time.time()
    while time.time() - start_time < timeout:
        try:
            urllib.request.urlopen(url)
            print("PaddleOCR 서버 준비 완료 (포트 8081)\n")
            return True
        except URLError:
            time.sleep(1)
    return False


# ==========================================
# [실행] 메인 로직 시작
# ==========================================
base64_image = encode_image(IMAGE_PATH)
llama_process = None

try:
    # 1. PaddleOCR 서버 자동 실행 (절대 경로 명시)
    llama_exec = os.path.join(MODELS_DIR, "llama-server.exe")

    server_command = [
        llama_exec,  # '.\' 대신 완벽한 절대 경로 사용
        "-m", "PaddleOCR-VL-1.5.gguf",
        "--mmproj", "PaddleOCR-VL-1.5-mmproj.gguf",
        "-ngl", "99",
        "--port", "8081"
    ]

    llama_process = subprocess.Popen(
        server_command,
        cwd=MODELS_DIR,  # 모델(.gguf) 파일들을 찾기 위해 작업 폴더는 유지
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )

    if not wait_for_server("http://127.0.0.1:8081/health"):
        raise Exception("서버 시동 시간 초과 (Timeout). 모델을 로드하지 못했습니다.")

    # 2. [STEP 1] PaddleOCR 스캐닝
    print("▶️ [STEP 1] PaddleOCR 텍스트 스캔 중...")
    ocr_client = OpenAI(base_url="http://127.0.0.1:8081/v1", api_key="sk-no-key-required")
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

    print("\n" + "=" * 40)
    print("[PaddleOCR 원본 추출 텍스트]")
    print(raw_ocr_text)
    print("=" * 40 + "\n")

    # 3. [STEP 2] Qwen 7B 텍스트 정제
    print("▶️ [STEP 2] Qwen 7B 텍스트 정제 중 (누락 방지 모드)...")
    json_client = OpenAI(base_url="http://127.0.0.1:11434/v1", api_key="sk-no-key-required")

    refine_prompt = f"""
    아래 영양 성분표 텍스트에서 '영양소 이름'과 '함량'만 찾아서 한 줄에 하나씩 나열해줘.
    % 정보나 기타 정보는 모두 제외하고 오직 '이름: 함량' 형식으로만 적어줘. 
    절대 요약하지 말고 모든 성분을 다 적어.

    [원본 텍스트]
    {raw_ocr_text}
    """

    refine_response = json_client.chat.completions.create(
        model="qwen2.5:7b",
        messages=[{"role": "user", "content": refine_prompt}],
        temperature=0.0,
        max_tokens=2048
    )

    refined_text = refine_response.choices[0].message.content.strip()

    # 4. [STEP 3] Python 정규식으로 JSON 변환
    print("▶️ [STEP 3] 데이터를 최종 JSON으로 변환 중...")
    nutrition_data = {}

    lines = refined_text.split('\n')
    for line in lines:
        match = re.search(r"([^:]+?)[:\s]+([\d\.]+\s*(?:mg|mcg|µg|g|kcal|%|IU)?)", line, re.IGNORECASE)
        if match:
            key = match.group(1).strip()
            value = match.group(2).strip()
            nutrition_data[key] = value

    print("\n[ 파이프라인 최종 결과]")
    print(json.dumps(nutrition_data, indent=2, ensure_ascii=False))

finally:
    # 5-1. Ollama 모델 즉시 퇴근시키기 (VRAM 반환)
    print("\n Qwen 7B 모델을 VRAM에서 즉시 해제합니다...")
    try:
        requests.post(
            "http://127.0.0.1:11434/api/generate",
            json={"model": "qwen2.5:7b", "keep_alive": 0} # 🌟 유지 시간을 0으로 덮어씌워서 즉시 종료
        )
        print("✅ Qwen 7B 해제 완료.")
    except Exception as e:
        print(f"⚠️ Ollama 해제 중 오류 (무시해도 됨): {e}")

    # 5-2. PaddleOCR 서버 종료 (기존과 동일)
    if llama_process:
        print("PaddleOCR 서버를 종료합니다...")
        llama_process.terminate()
        llama_process.wait()
        print("✅ PaddleOCR 종료 완료.")