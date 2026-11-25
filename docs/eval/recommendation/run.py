"""
추천 후보를 시험 세트(test_set.json)로 돌려 답을 그대로 저장한다 (Issue #12). 채점은 score.py.

후보
  gemma : 지금 추천 그대로 (gemma4:e4b, 약 + 증상)                  → 시험 20개 × 2번
  a     : 지금 추천과 같은 질문·같은 처리, 모델만 Gemini             → a_case_ids 5개 × 1번
          (하루 호출 제한이 있어 실패해도 다시 부르지 않음)
  b     : gemma가 약 설명서(caution) 전체를 읽고 부족·보충 성분과
          근거 문장을 냄 (증상 없음)                                 → 약 10개 × 2번
  b2    : B와 같은 일을 상호작용 검사 방식으로. 코드가 설명서 전체에서 16개 성분이 나온 문장을 찾고
          (상호작용 검사의 성분 표현, 단 피 속 수치 문장을 지우는 규칙은 뺌),
          gemma는 그 문장마다 부족·보충에 해당하는지만 고름        → 약 10개 × 2번
  b2g   : B2와 같은 입력·프롬프트, 모델만 Gemini                  → b2_gemini_drugs 5개 × 1번 (호출 제한)

사용법 (ai-server 가상환경의 python, Ollama·DB가 켜져 있어야 함):
  python run.py --only gemma --out runs/gemma.json
"""
import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2] / "ai-server"))

from fastapi import HTTPException  # noqa: E402
from app.api.v1.endpoints import recommendation as rec  # noqa: E402
from app.core.config import settings  # noqa: E402
from app.schemas.recommendation import RecommendationAnalyzeRequest  # noqa: E402
from app.services.ollama_llm_service import _client as ollama_client  # noqa: E402

GEMINI_MODEL = "gemini-3-flash-preview"
B_MODEL = "gemma4:e4b"
B_OPTIONS = {"temperature": 0, "num_ctx": 32768}  # 설명서 전체(시험 약 중 가장 긴 것 약 1만7천 자)가 들어가도록
B2_OPTIONS = {"temperature": 0, "num_ctx": 16384}  # 찾은 문장만 들어가므로 B보다 짧음

B_SYSTEM = """당신은 의약품 설명서(식약처 허가 '사용상의 주의사항')를 읽는 보조자입니다.
[할 일]
주어진 설명서 전체에서, 이 약을 복용하는 사람에게 어떤 영양소가 부족해질 수 있다고(결핍, 수치 저하, 흡수 장애 등) 적혀 있거나, 어떤 영양소의 보충·섭취가 필요하다고 적혀 있는 곳을 찾으세요.
[규칙]
1. 설명서에 적힌 내용만 사용하세요. 알고 있는 지식으로 보충하지 마세요.
2. 영양소마다 근거가 된 부분을 설명서에서 끊김 없이 이어진 한 부분 그대로 복사하세요(quote). 고치거나 줄이지 말고 120자 이내로 하세요.
3. 해당하는 영양소가 없으면 빈 목록을 반환하세요.
4. 출력은 유효한 JSON만 반환하세요. 마크다운 코드블록이나 설명 텍스트를 붙이지 마세요.

출력 형식:
{"nutrients": [{"name": "영양소 이름", "quote": "설명서에서 그대로 복사한 부분"}]}
"""


# B2: 상호작용 검사(backend NutrientGroup)의 성분 표현. 피 속 수치 문장을 지우는 규칙(noise)은 쓰지 않고,
# 칼륨도 수치 이야기를 빼려고 좁혀 둔 표현 대신 "칼륨" 전체로 찾는다.
MENTIONS = {name: re.compile(p, re.IGNORECASE) for name, p in {
    "철분": r"철분|철염|철제제|철(을|이)?\s*함유|철\s*보충제|철\s*ㆍ",
    "칼슘": r"칼슘",
    "마그네슘": r"마그네슘",
    "아연": r"아연",
    "칼륨": r"칼륨",
    "엽산": r"엽산",
    "비타민 A": r"비타민\s*A|레티놀",
    "비타민 B6": r"비타민\s*B\s*6|피리독신",
    "비타민 B12": r"비타민\s*B\s*12|코발라민",
    "비타민 C": r"비타민\s*C|아스코르브산",
    "비타민 D": r"비타민\s*D|콜레칼시페롤|에르고칼시페롤",
    "비타민 E": r"비타민\s*E|토코페롤",
    "비타민 K": r"비타민\s*K",
    "비오틴": r"비오틴",
    "오메가3": r"오메가\s*-?\s*3|(?<![가-힣])어유|피쉬\s*오일",
    "세인트존스워트": r"세인트\s*존스\s*워트|성\s*요한|St\.?\s*John|hypericum|히페리쿰",
}.items()}
# 문장 나누기는 backend EvidenceSentenceFinder와 같다: 번호("1)", "(2)", "③") 앞에서 나누고,
# 500자 넘는 덩어리는 "…다." 뒤에서 다시 나눠 성분이 나온 문장만, 그래도 길면 언급 앞 150자·뒤 250자만 남김
ITEM_START = re.compile(r"\s(?=(?:\(?\d{1,2}\)|[①-⑳])\s?\S)")
SENTENCE_END = re.compile(r"(?<=다\.)\s+")

B2_SYSTEM = """당신은 의약품 설명서(식약처 허가 '사용상의 주의사항')의 문장을 읽는 보조자입니다.
[할 일]
문장(id)마다, 그 문장의 '성분' 목록에 있는 성분 각각에 대해 kind를 고르세요.
- SUPPLEMENT: 이 약을 복용하는 사람에게 그 성분이 부족해질 수 있다고(결핍, 수치 저하, 흡수 장애 등) 적혀 있거나, 그 성분의 보충·섭취가 필요하다고 적혀 있음
- OTHER: 그 밖의 경우
[규칙]
1. 주어진 문장에 적힌 내용만 사용하세요. 알고 있는 지식으로 보충하지 마세요.
2. 출력은 유효한 JSON만 반환하세요. 마크다운 코드블록이나 설명 텍스트를 붙이지 마세요.

출력 형식:
{"results": [{"id": 1, "nutrient": "성분 이름", "kind": "SUPPLEMENT"}]}
"""


def mentioned(text: str) -> list[str]:
    return [name for name, pattern in MENTIONS.items() if pattern.search(text)]


def trim_around(text: str, names: list[str]) -> str:
    if len(text) <= 400:
        return text
    pos = min(m.start() for n in names for m in [MENTIONS[n].search(text)] if m)
    return text[max(0, pos - 150):pos + 250]


def find_sentences(label: str) -> list[dict]:
    """설명서 전체에서 16개 성분이 나온 문장과 그 성분들을 찾는다 (AI 없음)."""
    items = list(dict.fromkeys(i.strip() for i in ITEM_START.split(label) if i.strip()))  # 순서 유지 + 중복 제거
    found = []
    for item in items:
        if not mentioned(item):
            continue
        if len(item) <= 500:
            found.append(item)
            continue
        parts = [s.strip() for s in SENTENCE_END.split(item) if mentioned(s)]
        found += [trim_around(s, mentioned(s)) for s in (parts or [item])]
    return [{"id": i + 1, "sentence": s, "nutrients": mentioned(s)} for i, s in enumerate(dict.fromkeys(found))]


def gemma_json(system: str, user: str):
    response = ollama_client.chat(model=B_MODEL, options=B2_OPTIONS, format="json",
                                  messages=[{"role": "system", "content": system}, {"role": "user", "content": user}])
    return response.get("message", {}).get("content", ""), response.get("prompt_eval_count")


def gemini_json(system: str, user: str):
    from google.genai import types
    from app.services.gemini_service import get_client
    config = types.GenerateContentConfig(system_instruction=system, temperature=0, response_mime_type="application/json")
    response = get_client().models.generate_content(model=GEMINI_MODEL, contents=user, config=config)
    usage = response.usage_metadata
    return (response.text or "").strip(), usage.prompt_token_count if usage else None


def read_sentences(drug: dict, label: str, ask=gemma_json) -> dict:
    """B2: 찾은 문장만 모델에게 주고, 문장·성분마다 부족·보충(SUPPLEMENT)인지 고르게 한다."""
    items = find_sentences(label)
    result = {"sec": 0.0, "prompt_tokens": None, "error": None, "nutrients": [], "sentences": len(items)}
    if not items:
        return result
    lines = [f"(id={it['id']}, 성분: {', '.join(it['nutrients'])}) {it['sentence']}" for it in items]
    user = f"[약]\n{drug['patient_drugs'][0]}\n\n[설명서 문장]\n" + "\n".join(lines) + "\n\n위 형식의 JSON으로만 답하세요."
    start = time.perf_counter()
    try:
        text, result["prompt_tokens"] = ask(B2_SYSTEM, user)
    except Exception as e:  # Gemini 호출 제한 등 — 다시 부르지 않고 기록만
        return {**result, "sec": round(time.perf_counter() - start, 1), "error": hide_key(f"{type(e).__name__}: {str(e)[:300]}")}
    result["sec"] = round(time.perf_counter() - start, 1)
    result["raw"] = text
    try:
        answers = json.loads(text).get("results", [])
    except (json.JSONDecodeError, AttributeError):
        return {**result, "error": "JSON 아님"}
    by_id = {it["id"]: it for it in items}
    whole = squeeze(label)
    for a in answers:
        if not isinstance(a, dict) or str(a.get("kind", "")).upper() != "SUPPLEMENT":
            continue
        item = by_id.get(a.get("id"))
        asked = {squeeze(n).lower(): n for n in item["nutrients"]} if item else {}
        name = asked.get(squeeze(str(a.get("nutrient", ""))).lower())  # 띄어쓰기 차이("비타민K")는 같은 이름으로
        if name is None:
            continue  # 묻지 않은 문장·성분이면 버림
        result["nutrients"].append({"name": name, "quote": item["sentence"],
                                    "quote_ok": squeeze(item["sentence"]) in whole})
    return result


def hide_key(text: str) -> str:
    key = settings.GEMINI_API_KEY
    return text.replace(key, "***") if key else text


def gemini_chat(prompt, model=None, system=None, options=None, format=None, think=None):
    """recommendation.chat 자리에 끼워 넣는 Gemini 호출 (같은 시스템 프롬프트·온도·JSON 형식)."""
    from google.genai import types
    from app.services.gemini_service import get_client
    config = types.GenerateContentConfig(
        system_instruction=system,
        temperature=(options or {}).get("temperature"),
        response_mime_type="application/json" if format == "json" else None,
    )
    response = get_client().models.generate_content(model=GEMINI_MODEL, contents=prompt, config=config)
    return (response.text or "").strip()


def recommend(case: dict, drug: dict) -> dict:
    """지금 추천 함수를 그대로 부른다 (rec.chat이 gemma 또는 Gemini)."""
    request = RecommendationAnalyzeRequest(condition=case["symptom"], patient_drugs=drug["patient_drugs"])
    start = time.perf_counter()
    try:
        out = rec.recommend_safe_nutrients(request)
        return {"sec": round(time.perf_counter() - start, 1), "error": None,
                "analysis": out.interaction_analysis,
                "nutrients": [n.model_dump() for n in out.recommended_nutrients]}
    except HTTPException as e:
        cause = e.__cause__
        return {"sec": round(time.perf_counter() - start, 1), "nutrients": [],
                "error": hide_key(f"{e.detail} / {type(cause).__name__}: {str(cause)[:300]}")}


def label_texts(item_seqs: list[str]) -> dict:
    """DB에서 설명서 글(drug_permit_detail.caution)을 가져온다."""
    seqs = ",".join(f"'{s}'" for s in item_seqs)
    sql = f"select json_object_agg(item_seq, caution) from drug_permit_detail where item_seq in ({seqs})"
    out = subprocess.run(["docker", "exec", "pnn-postgres", "psql", "-U", "postgres", "-d", "pnn-db", "-At", "-c", sql],
                         capture_output=True, text=True, encoding="utf-8", check=True).stdout
    texts = json.loads(out)
    for seq, text in texts.items():
        if not text or text.startswith("http"):
            raise SystemExit(f"{seq}: 설명서가 아직 글로 바뀌지 않았음 (PDF 주소)")
    return texts


def squeeze(text: str) -> str:
    return re.sub(r"\s+", "", text or "")


def read_label(drug: dict, label: str) -> dict:
    """B: 설명서 전체를 읽고 부족·보충 성분과 근거 문장을 받는다. 근거 문장이 원문에 있는지 대조한다."""
    user = f"[약]\n{drug['patient_drugs'][0]}\n\n[설명서]\n{label}\n\n위 형식의 JSON으로만 답하세요."
    start = time.perf_counter()
    response = ollama_client.chat(model=B_MODEL, options=B_OPTIONS, format="json",
                                  messages=[{"role": "system", "content": B_SYSTEM}, {"role": "user", "content": user}])
    sec = round(time.perf_counter() - start, 1)
    text = response.get("message", {}).get("content", "")
    prompt_tokens = response.get("prompt_eval_count")
    result = {"sec": sec, "prompt_tokens": prompt_tokens, "error": None, "nutrients": []}
    try:
        items = json.loads(text).get("nutrients", [])
    except json.JSONDecodeError:
        return {**result, "error": "JSON 아님", "raw": text[:500]}
    whole = squeeze(label)
    for item in items:
        if isinstance(item, dict):
            quote = str(item.get("quote", ""))
            result["nutrients"].append({"name": str(item.get("name", "")), "quote": quote,
                                        "quote_ok": bool(squeeze(quote)) and squeeze(quote) in whole})
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", choices=["gemma", "a", "b", "b2", "b2g"], required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    test_set = json.loads((HERE / "test_set.json").read_text(encoding="utf-8"))
    drugs, cases = test_set["drugs"], test_set["cases"]
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    result = {"candidate": args.only, "date": time.strftime("%Y-%m-%d %H:%M"), "runs": []}

    def save():  # 중간에 멈춰도 그때까지 받은 답은 남도록 매번 저장
        out_path.write_text(json.dumps(result, ensure_ascii=False, indent=1), encoding="utf-8")

    if args.only == "gemma":
        result["model"] = settings.RECOMMENDATION_MODEL
        for repeat in (1, 2):
            for case in cases:
                print(f"gemma {repeat}회차 시험 {case['id']}", flush=True)
                result["runs"].append({"case_id": case["id"], "repeat": repeat, **recommend(case, drugs[case["drug"]])})
                save()
    elif args.only == "a":
        rec.chat = gemini_chat
        result["model"] = GEMINI_MODEL
        for case in [c for c in cases if c["id"] in test_set["a_case_ids"]]:
            print(f"A 시험 {case['id']} (Gemini 호출 1회)", flush=True)
            result["runs"].append({"case_id": case["id"], "repeat": 1, **recommend(case, drugs[case["drug"]])})
            save()
    elif args.only == "b2g":
        result["model"] = GEMINI_MODEL
        chosen = {k: drugs[k] for k in test_set["b2_gemini_drugs"]}
        texts = label_texts([d["item_seq"] for d in chosen.values()])
        for key, drug in chosen.items():  # 호출 제한: 약마다 한 번씩만, 실패해도 다시 부르지 않음
            print(f"b2g {key} (Gemini 호출 1회)", flush=True)
            result["runs"].append({"drug": key, "repeat": 1, **read_sentences(drug, texts[drug["item_seq"]], gemini_json)})
            save()
    else:
        reader = read_label if args.only == "b" else read_sentences
        result["model"] = B_MODEL
        result["options"] = B_OPTIONS if args.only == "b" else B2_OPTIONS
        texts = label_texts([d["item_seq"] for d in drugs.values()])
        for repeat in (1, 2):
            for key, drug in drugs.items():
                print(f"{args.only} {repeat}회차 {key}", flush=True)
                result["runs"].append({"drug": key, "repeat": repeat, **reader(drug, texts[drug["item_seq"]])})
                save()
    print(f"끝. 결과: {out_path}", flush=True)


if __name__ == "__main__":
    main()
