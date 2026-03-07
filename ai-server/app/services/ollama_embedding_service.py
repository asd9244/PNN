import os
import ollama
from typing import List

# OLLAMA_HOST가 0.0.0.0이면 클라이언트 접속 실패 → 127.0.0.1로 대체
_ollama_host = os.getenv("OLLAMA_HOST", "http://127.0.0.1:11434")
if _ollama_host.startswith("0.0.0.0"):
    _ollama_host = "http://127.0.0.1:11434"
elif "://" not in _ollama_host and _ollama_host:
    _ollama_host = f"http://{_ollama_host}"
_client = ollama.Client(host=_ollama_host)

def get_embedding(text: str, model: str = "bge-m3") -> List[float]:
    """
    Ollama 로컬 BGE-M3 모델로 임베딩 벡터를 생성합니다.
    기본 모델: bge-m3 (1024차원), API 비용 없음.
    """
    text = text.replace("\n", " ")
    response = _client.embed(model=model, input=text)
    return response["embeddings"][0]
