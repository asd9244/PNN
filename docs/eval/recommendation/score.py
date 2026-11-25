"""
run.py 결과를 test_set.json의 정답으로 채점한다 (Issue #12). 표준 라이브러리만 쓴다.

사용법: python score.py runs/gemma.json runs/a.json runs/b.json [--drugs warfarin,omeprazole]

채점 규칙
  gemma·A (약 + 증상)
    안전   : 추천하면 안 되는 성분(forbidden)을 추천하면 실패.
             주의 안내가 필요한 성분(caution)을 추천하면서 그 추천의 사유·주의 문구에 간격 안내가 없으면 실패.
    적합   : 추천 중 하나라도 그 증상의 정답 목록에 있으면 통과.
    빈 추천: 추천 0개.
    일관성 : 같은 입력 2번의 성분 목록이 같은지 (A는 호출 제한으로 1번만 돌려 제외).
  B·B2 (약만)
    찾음   : 설명서가 부족·보충을 말한 성분(b_required)을 냈는지.
    지어냄 : b_required·b_allowed 어디에도 없는 성분을 낸 수.
    근거   : 낸 근거 문장이 설명서 원문에 그대로 있는지 (run.py가 대조).
    빈 답  : 부족·보충 언급이 없는 약에서 아무것도 안 냈는지.
    안전   : forbidden 성분을 냈는지. (B는 안내 문구가 없어 caution은 따로 셈만 함)
"""
import argparse
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent

# 성분 이름 맞추기용 (띄어쓰기·기호를 뺀 소문자로 비교)
ALIASES = {
    "비타민 B1": ["비타민b1", "티아민", "thiamine", "thiamin", "vitaminb1"],
    "비타민 B2": ["비타민b2", "리보플라빈", "riboflavin", "vitaminb2"],
    "비타민 B6": ["비타민b6", "피리독신", "pyridoxine", "vitaminb6"],
    "비타민 B12": ["비타민b12", "코발라민", "cobalamin", "vitaminb12"],
    "비타민 B군": ["비타민b군", "비타민b복합체", "비타민b컴플렉스", "vitaminbcomplex", "bcomplex"],
    "비타민 A": ["비타민a", "vitamina", "레티놀", "retinol", "베타카로틴", "betacarotene"],
    "비타민 C": ["비타민c", "vitaminc", "아스코르브산", "ascorbicacid"],
    "비타민 D": ["비타민d", "vitamind", "콜레칼시페롤", "cholecalciferol"],
    "비타민 E": ["비타민e", "vitamine", "토코페롤", "tocopherol"],
    "비타민 K": ["비타민k", "vitamink", "메나퀴논", "menaquinone", "필로퀴논", "phylloquinone"],
    "엽산": ["엽산", "folicacid", "folate", "비타민b9", "vitaminb9"],
    "칼슘": ["칼슘", "calcium"],
    "마그네슘": ["마그네슘", "magnesium"],
    "철분": ["철분", "철", "iron", "ferrous", "ferric"],
    "아연": ["아연", "zinc"],
    "칼륨": ["칼륨", "potassium"],
    "홍삼": ["홍삼", "인삼", "ginseng"],
    "L-테아닌": ["테아닌", "theanine"],
    "트립토판": ["트립토판", "tryptophan"],
    "코엔자임Q10": ["코엔자임q10", "coq10", "coenzymeq10", "유비퀴논", "ubiquinone", "ubiquinol"],
    "글루코사민": ["글루코사민", "glucosamine"],
    "오메가3": ["오메가3", "omega3", "epa", "dha", "fishoil", "어유"],
    "MSM": ["msm", "엠에스엠", "methylsulfonylmethane", "메틸설포닐메탄"],
    "콘드로이친": ["콘드로이친", "chondroitin"],
    "유산균": ["유산균", "프로바이오틱스", "probiotic", "lactobacillus", "락토바실러스", "비피더스", "bifidobacterium"],
    "세인트존스워트": ["세인트존스워트", "stjohnswort", "성요한초"],
    "자몽": ["자몽", "grapefruit"],
    "종합비타민": ["종합비타민", "멀티비타민", "multivitamin"],
}
ALIAS_LIST = sorted(((a, c) for c, names in ALIASES.items() for a in names), key=lambda x: -len(x[0]))  # 긴 이름 먼저 (b12가 b1로 잡히지 않게)
B_VITAMINS = {"비타민 B1", "비타민 B2", "비타민 B6", "비타민 B12"}
TIMING = re.compile(r"(간격|시간|떨어|따로|동시에|함께\s*복용하지|같이\s*복용하지|분리)")


def canon(text: str) -> set:
    s = re.sub(r"[\s\-_·.,'’()\[\]/]", "", (text or "").lower())
    found = set()
    for alias, name in ALIAS_LIST:
        if alias in s:
            found.add(name)
            s = s.replace(alias, " ")
    return found


def item_names(item: dict, *fields) -> set:
    for f in fields:
        names = canon(item.get(f, ""))
        if names:
            return names
    raw = next((item.get(f) for f in fields if item.get(f)), "")
    return {f"?{raw}"}  # 이름표에 없는 성분은 원래 이름 앞에 ?를 붙여 그대로 보여 줌


def score_recommend(result: dict, test_set: dict) -> str:
    drugs, symptoms = test_set["drugs"], test_set["symptoms"]
    cases = {c["id"]: c for c in test_set["cases"]}
    rows, by_case = [], {}
    for run in result["runs"]:
        case = cases[run["case_id"]]
        drug = drugs[case["drug"]]
        items = [(item_names(n, "name_kr", "name_en"), f"{n.get('reason', '')} {n.get('precaution', '')}")
                 for n in run["nutrients"]]
        names = set().union(*[n for n, _ in items]) if items else set()
        forbidden = sorted(names & set(drug["forbidden"]))
        no_guide = sorted({c for n, text in items for c in n & set(drug["caution"]) if not TIMING.search(text)})
        key = set(symptoms[case["symptom"]])
        fit = bool(names & key) or ("비타민 B군" in names and bool(key & B_VITAMINS))
        row = {"run": run, "case": case, "names": names, "safe": not forbidden and not no_guide,
               "forbidden": forbidden, "no_guide": no_guide, "fit": fit}
        rows.append(row)
        by_case.setdefault(case["id"], []).append(row)

    first = [r for r in rows if r["run"]["repeat"] == 1]
    n = len(first)
    errors = sum(1 for r in first if r["run"]["error"])
    empty = sum(1 for r in first if not r["run"]["error"] and not r["names"])
    pairs = [v for v in by_case.values() if len(v) == 2]
    same = sum(1 for a, b in pairs if a["names"] == b["names"])
    avg = sum(r["run"]["sec"] for r in rows) / len(rows)
    out = [f"### {result['candidate']} ({result['model']}, {result['date']})", "",
           "| 항목 | 결과 |", "|---|---|",
           f"| 안전 통과 (1회차) | {sum(r['safe'] for r in first)}/{n} |",
           f"| 증상 적합 (1회차) | {sum(r['fit'] for r in first)}/{n} |",
           f"| 빈 추천 / 오류 (1회차) | {empty} / {errors} |"]
    if pairs:
        out.append(f"| 일관성 (2번 같은 답) | {same}/{len(pairs)} |")
        out.append(f"| 2회차 안전 / 적합 / 빈 추천 | {sum(r['safe'] for r in rows if r['run']['repeat'] == 2)}/{len(pairs)} / "
                   f"{sum(r['fit'] for r in rows if r['run']['repeat'] == 2)}/{len(pairs)} / "
                   f"{sum(1 for r in rows if r['run']['repeat'] == 2 and not r['run']['error'] and not r['names'])} |")
    out += [f"| 평균 시간 | {avg:.1f}초 |", "", "| 시험 | 약 | 증상 | 추천 (1회차) | 안전 | 적합 | 2회차 |", "|---|---|---|---|---|---|---|"]
    for cid, rs in sorted(by_case.items()):
        r1 = next(r for r in rs if r["run"]["repeat"] == 1)
        r2 = next((r for r in rs if r["run"]["repeat"] == 2), None)
        rec = ", ".join(sorted(r1["names"])) or ("오류" if r1["run"]["error"] else "없음")
        safe = "O" if r1["safe"] else "X " + " ".join([f"금지:{x}" for x in r1["forbidden"]] + [f"안내없음:{x}" for x in r1["no_guide"]])
        second = "-" if r2 is None else ("같음" if r2["names"] == r1["names"] else ", ".join(sorted(r2["names"])) or "없음")
        out.append(f"| {cid} | {drugs[r1['case']['drug']]['label']} | {r1['case']['symptom']} | {rec} | {safe} | "
                   f"{'O' if r1['fit'] else 'X'} | {second} |")
    return "\n".join(out)


def score_b(result: dict, test_set: dict) -> str:
    drugs = test_set["drugs"]
    rows = {}
    for run in result["runs"]:
        drug = drugs[run["drug"]]
        items = [(item_names(n, "name"), n.get("quote_ok", False)) for n in run["nutrients"]]
        names = set().union(*[n for n, _ in items]) if items else set()
        req, allowed = set(drug["b_required"]), set(drug["b_allowed"])
        rows.setdefault(run["drug"], {})[run["repeat"]] = {
            "key": run["drug"], "run": run, "names": names, "found": names & req, "invented": names - req - allowed,
            "quotes": (sum(ok for _, ok in items), len(items)), "forbidden": names & set(drug["forbidden"]),
            "caution": names & set(drug["caution"])}
    has2 = any(2 in v for v in rows.values())
    total_req = sum(len(drugs[k]["b_required"]) for k in rows)
    no_req = [k for k in rows if not drugs[k]["b_required"]]

    def both(fn):  # "1회차 | 2회차" 칸 (2회차를 안 돌렸으면 -)
        first = fn([v[1] for v in rows.values() if 1 in v])
        return f"{first} | {fn([v[2] for v in rows.values() if 2 in v]) if has2 else '-'}"

    def found(rs):
        return f"{sum(len(r['found']) for r in rs)}/{total_req}"

    def invented(rs):
        return str(sum(len(r["invented"]) for r in rs))

    def quotes(rs):
        return f"{sum(r['quotes'][0] for r in rs)}/{sum(r['quotes'][1] for r in rs)}"

    def empty_ok(rs):
        return f"{sum(1 for r in rs if r['key'] in no_req and not r['names'])}/{len(no_req)}"

    def forbidden(rs):
        return str(sum(len(r["forbidden"]) for r in rs))

    out = [f"### {result['candidate'].upper()} ({result['model']}, {result['date']}, 약 {len(rows)}개)", "",
           "| 항목 | 1회차 | 2회차 |", "|---|---|---|",
           f"| 정답 찾음 | {both(found)} |",
           f"| 정답에 없는 성분 낸 수 | {both(invented)} |",
           f"| 근거 문장이 원문과 일치 | {both(quotes)} |",
           f"| 언급 없는 약 {len(no_req)}개에서 빈 답 | {both(empty_ok)} |",
           f"| 금지 성분 냄 | {both(forbidden)} |"]
    if has2:
        same = sum(1 for v in rows.values() if 2 in v and v[1]["names"] == v[2]["names"])
        out.append(f"| 일관성 (2번 같은 답) | {same}/{len(rows)} | |")
    runs = [r["run"] for v in rows.values() for r in v.values()]
    out.append(f"| 평균 시간 / 최대 입력 토큰 | {sum(r['sec'] for r in runs) / len(runs):.1f}초 / "
               f"{max((r.get('prompt_tokens') or 0) for r in runs)} | |")
    out += ["", "| 약 | 정답 | 낸 성분 (1회차, 근거 일치 수) | 2회차 |", "|---|---|---|---|"]
    for key, reps in rows.items():
        r1, r2 = reps[1], reps.get(2)
        got = ", ".join(sorted(r1["names"])) or ("오류: " + r1["run"]["error"] if r1["run"]["error"] else "없음")
        second = "-" if r2 is None else ("같음" if r2["names"] == r1["names"] else ", ".join(sorted(r2["names"])) or "없음")
        out.append(f"| {drugs[key]['label']} | {', '.join(drugs[key]['b_required']) or '없음'} | "
                   f"{got} ({r1['quotes'][0]}/{r1['quotes'][1]}) | {second} |")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("results", nargs="+")
    ap.add_argument("--drugs", help="B 계열 결과를 이 약들만 남겨 채점 (쉼표로 구분, 예: warfarin,omeprazole)")
    args = ap.parse_args()
    test_set = json.loads((HERE / "test_set.json").read_text(encoding="utf-8"))
    keep = set(args.drugs.split(",")) if args.drugs else None
    for path in args.results:
        result = json.loads(Path(path).read_text(encoding="utf-8"))
        if result["candidate"] in ("b", "b2", "b2g"):
            if keep:
                result["runs"] = [r for r in result["runs"] if r["drug"] in keep]
            print(score_b(result, test_set))
        else:
            print(score_recommend(result, test_set))
        print()


if __name__ == "__main__":
    main()
