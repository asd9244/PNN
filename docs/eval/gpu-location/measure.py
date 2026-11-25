"""
GPU 모델(사진 인식 PaddleOCR-VL + 정리·근거 읽기·추천 gemma4:e4b) 속도·동시 사용 측정.

AI 서버(기본 http://127.0.0.1:8000)에 직접 요청한다. 백엔드의 캐시를 거치지 않으므로
매번 모델이 실제로 돈 시간이다.

사용법 (ai-server 가상환경의 python):
  python measure.py --photos <사진 폴더> --sentences <문장 JSON> --out <결과 JSON>

문장 JSON: [{"sentence": "...", "nutrients": ["칼슘", ...]}, ...]
  만드는 법: evidence_reading_cache 표의 문장을 문장별로 묶어서 내보냄
  (README.md의 명령 참고). 결과에는 걸린 시간·성공 여부·개수만 남기고 사진·문장 내용은 남기지 않는다.
"""
import argparse
import json
import subprocess
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import requests

SENTENCES_PER_REQUEST = 3  # 약 하나에 근거 문장이 1~3개 나오는 경우가 많아 3개씩 묶음
RECOMMEND_INPUTS = [
    ["씬지로이드정0.1밀리그램(레보티록신나트륨수화물)"],
    ["쿠파린정5밀리그램(와파린나트륨)"],
    ["포사맥스70밀리그램정(알렌드론산나트륨수화물)", "씬지로이드정0.1밀리그램(레보티록신나트륨수화물)"],
]


class GpuMemoryWatcher:
    """1초마다 nvidia-smi로 GPU 메모리 사용량을 읽어 최댓값을 기록."""

    def __init__(self):
        self.max_mib = 0
        self._stop = threading.Event()
        self._thread = threading.Thread(target=self._run, daemon=True)

    def _run(self):
        while not self._stop.is_set():
            try:
                out = subprocess.run(
                    ["nvidia-smi", "--query-gpu=memory.used", "--format=csv,noheader,nounits"],
                    capture_output=True, text=True, timeout=5).stdout
                self.max_mib = max(self.max_mib, int(out.strip().splitlines()[0]))
            except Exception:
                pass
            self._stop.wait(1)

    def start(self):
        self._thread.start()

    def stop(self):
        self._stop.set()
        self._thread.join()


def call_photo(base, path: Path):
    start = time.perf_counter()
    with open(path, "rb") as f:
        r = requests.post(f"{base}/api/v1/supplement/extract",
                          files={"image": (path.name, f, "image/jpeg")}, timeout=600)
    sec = time.perf_counter() - start
    body = r.json() if r.ok else {}
    ok = r.ok and "error" not in body
    return {"sec": round(sec, 1), "ok": ok, "count": len(body.get("nutrients", [])) if ok else 0}


def call_evidence(base, items):
    payload = {"drug_name": "측정용", "items": [
        {"id": i + 1, "sentence": it["sentence"], "nutrients": it["nutrients"]} for i, it in enumerate(items)]}
    start = time.perf_counter()
    r = requests.post(f"{base}/api/v1/interaction/read-evidence", json=payload, timeout=600)
    sec = time.perf_counter() - start
    asked = sum(len(it["nutrients"]) for it in items)
    got = len(r.json().get("results", [])) if r.ok else 0
    return {"sec": round(sec, 1), "ok": r.ok and got > 0, "asked": asked, "got": got}


def call_recommend(base, drugs):
    start = time.perf_counter()
    r = requests.post(f"{base}/api/v1/recommendation/analyze-safe",
                      json={"condition": "", "patient_drugs": drugs}, timeout=600)
    sec = time.perf_counter() - start
    got = len(r.json().get("recommended_nutrients", [])) if r.ok else 0
    return {"sec": round(sec, 1), "ok": r.ok and got > 0, "got": got}


def run_together(jobs):
    """jobs: [(이름, 함수, 인자)] 를 동시에 보내고, 각각 끝난 시간을 돌려줌."""
    with ThreadPoolExecutor(max_workers=len(jobs)) as pool:
        futures = [(name, pool.submit(fn, *args)) for name, fn, args in jobs]
        return [{"job": name, **f.result()} for name, f in futures]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://127.0.0.1:8000")
    ap.add_argument("--photos", required=True)
    ap.add_argument("--sentences", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    photos = sorted(Path(args.photos).glob("*.jpg"))
    sentences = json.loads(Path(args.sentences).read_text(encoding="utf-8-sig"))  # PowerShell로 만든 파일의 BOM 허용
    evidence_requests = [sentences[i:i + SENTENCES_PER_REQUEST]
                         for i in range(0, len(sentences), SENTENCES_PER_REQUEST)]
    base = args.base
    result = {"photos": len(photos), "evidence_requests": len(evidence_requests)}

    watcher = GpuMemoryWatcher()
    watcher.start()

    print("준비: 모델을 메모리에 올림 (시간 기록 안 함)", flush=True)
    call_photo(base, photos[0])
    call_evidence(base, evidence_requests[0])
    call_recommend(base, RECOMMEND_INPUTS[0])

    print("1) 혼자 쓸 때: 사진", flush=True)
    result["solo_photo"] = [call_photo(base, p) for p in photos]
    print("1) 혼자 쓸 때: 근거 읽기", flush=True)
    result["solo_evidence"] = [call_evidence(base, req) for req in evidence_requests]
    print("1) 혼자 쓸 때: 추천", flush=True)
    result["solo_recommend"] = [call_recommend(base, d) for d in RECOMMEND_INPUTS]

    print("2) 동시에 보낼 때", flush=True)
    together = {}
    for n in (2, 3):
        together[f"photo_x{n}"] = [run_together(
            [(f"사진{k + 1}", call_photo, (base, photos[(r * n + k) % len(photos)])) for k in range(n)])
            for r in range(2)]
        together[f"evidence_x{n}"] = [run_together(
            [(f"근거{k + 1}", call_evidence, (base, evidence_requests[(r * n + k) % len(evidence_requests)]))
             for k in range(n)]) for r in range(2)]
    together["photo_and_evidence"] = [run_together([
        ("사진", call_photo, (base, photos[r])),
        ("근거", call_evidence, (base, evidence_requests[r]))]) for r in range(2)]
    result["together"] = together

    watcher.stop()
    result["gpu_memory_max_mib"] = watcher.max_mib
    Path(args.out).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"끝. 결과: {args.out}", flush=True)


if __name__ == "__main__":
    main()
