"""
Ollama LLM 호출 (상호작용 분석 등)
"""
import os
import ollama
import json
from typing import Optional

_ollama_host = os.getenv("OLLAMA_HOST", "http://127.0.0.1:11434")
if _ollama_host.startswith("0.0.0.0"):
    _ollama_host = "http://127.0.0.1:11434"
elif "://" not in _ollama_host and _ollama_host:
    _ollama_host = f"http://{_ollama_host}"
_client = ollama.Client(host=_ollama_host)


def chat(prompt: str, model: str = "llama3.2", system: Optional[str] = None) -> str:
    """
    Ollama 채팅 API 호출.
    Returns: assistant 응답 텍스트
    """
    messages = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})
    messages = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})
    response = _client.chat(model=model, messages=messages)
    return response.get("message", {}).get("content", "").strip()

def build_safe_recommendation_prompt(patient_drugs: list[str], contraindicated_nutrients: list[str]) -> tuple[str, str]:
    """
    Case B (안전 영양제 추천) 프롬프트 생성.
    환각을 제어하고 금기 성분을 철저히 배제하기 위한 보수적 프롬프트 구조.
    Returns: (system_prompt, user_prompt)
    """
    system_prompt = (
        "당신은 영양제 및 약물 상호작용을 분석하는 전문 AI 어시스턴트입니다. "
        "당신의 임무는 환자가 현재 복용 중인 약물을 고려하여, 안전한 영양 성분 2가지를 추천하는 것입니다.\n"
        "\n"
        "[강력한 추천 규칙 - 반드시 준수할 것]\n"
        "1. [금기 영양 성분] 목록에 포함된 성분은 절대 추천하지 마십시오. 이는 환자의 안전과 직결됩니다.\n"
        "2. 추천하는 영양 성분은 [현재 복용 중인 약물]과 알려진 상호작용이 없어야 합니다.\n"
        "3. 추천 사유 작성 시 '치료', '완치', '예방'과 같은 의학적 효능을 암시하는 단어를 절대 사용하지 마십시오. 대신 '보편적인 건강 유지에 도움을 줄 수 있음'과 같이 매우 보수적으로 작성해야 합니다.\n"
        "4. 당신의 출력은 마크다운 코드 블록(```json)이나 다른 설명 텍스트 없이, 반드시 올바른 JSON 형식(VALID JSON format)이어야 합니다.\n"
        "5. 성분명(name)은 반드시 영문(예: Magnesium, Vitamin D)으로 작성하고, 추천 사유(reason_kr)는 한국어로 작성하십시오.\n"
        "\n"
        "필수 JSON 출력 형식:\n"
        "{\n"
        "  \"recommended_nutrients\": [\n"
        "    {\n"
        "      \"name\": \"영문 성분명\",\n"
        "      \"reason_kr\": \"한국어로 작성된 보수적이고 일상적인 추천 사유 (예: 이 성분은 현재 복용 중인 약물과 뚜렷한 상호작용이 없어 보편적인 건강 유지에 도움을 줄 수 있습니다.)\"\n"
        "    }\n"
        "  ]\n"
        "}"
    )

    user_prompt = (
        f"[현재 복용 중인 약물 (Patient Drugs)]\n"
        f"{', '.join(patient_drugs) if patient_drugs else '없음'}\n\n"
        f"[금기 영양 성분 (MUST NOT RECOMMEND)]\n"
        f"{', '.join(contraindicated_nutrients) if contraindicated_nutrients else '없음'}\n\n"
        "위 규칙을 엄격히 준수하여, 환자에게 안전하고 보편적인 영양 성분 2가지를 지정된 JSON 형식으로 추천해 주십시오."
    )

    return system_prompt, user_prompt
