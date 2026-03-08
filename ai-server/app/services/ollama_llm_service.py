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
    response = _client.chat(model=model, messages=messages)
    return response.get("message", {}).get("content", "").strip()
