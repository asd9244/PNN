"""
drugs_master.form_code_name 을 4분류(정제, 경질캡슐, 연질캡슐, 기타)로 일괄 치환.

- 원본 값이 아래 매핑에 없으면 그대로 유지.
- 겹침 처리: '정제'→정제, '산제'→경질캡슐, '미분류'→정제 (사용자 목록 기준 우선순위).

실행: 프로젝트 루트 또는 이 디렉터리에서
  python data/scripts/normalize_form_code_name.py
"""
from __future__ import annotations

import os
from collections import Counter

import psycopg
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")

# --- 사용자 분류 (중복·충돌은 주석의 우선순위로 단일 카테고리만 지정) ---

FORM_JEONGJE = (
    "나정",
    "정제",
    "질정",
    "다층정",
    "당의정",
    "박칼정",
    "발포정",
    "부착정",
    "서방정",
    "설하정",
    "유핵정",
    "장용정",
    "구강붕해정",
    "필름코팅정",
    "서방성다층정",
    "장용성당의정",
    "서방성필름코팅정",
    "장용성필름코팅정",
    "서방성장용필름코팅정",
    "장용성필름코팅당의정",
    "추어블정(저작정)",
    "분산정(현탁정)",
    "미분류",  # 기타 목록에도 있으나 정제(23개) 쪽에 포함 → 정제로 통일
)

FORM_HARD_CAPSULE = (
    "스팬슐",
    "장용성필름코팅캡슐제",
    "젤라틴코팅성경질캡슐제",
    "서방성캡슐제",
    "펠렛",
    "장용성캡슐제",
    # 경질 목록에 '정제'가 반복 기재됨 → 정제 카테고리로만 매핑 (여기서 제외)
    "경질캡슐제",
    "산제",  # 기타에도 있으나 경질(13개)에 포함 → 경질캡슐
    "공캡슐",
    "과립제",
    "과립제정제",
    "서방성장용성펠렛",
)

FORM_SOFT_CAPSULE = (
    "질연질캡슐제",
    "연질캡슐제",
    "액상",
    "현탁상",
)

FORM_ETC = (
    "껌제",
    "트로키제",
    "구강붕해필름",
    "정량분말분무제",
    "지지체가있는첩부제",
    "정량흡입제",
    "분말제",
    "질좌제",
    "일반",
    "흡입제",
    "캡슐",
    # '미분류', '산제' 는 위에서 정제/경질로 처리
)

# 원천에 "제형A, 제형B" 형태로 들어온 복합 문자열 (쉼표+공백 기준으로 DB에 저장된 그대로)
FORM_COMPOUND: tuple[tuple[str, str], ...] = (
    ("서방성캡슐제, 펠렛", "경질캡슐"),
    ("장용성캡슐제, 정제", "경질캡슐"),
    ("장용성캡슐제, 펠렛", "경질캡슐"),
    ("경질캡슐제, 산제", "경질캡슐"),
    ("경질캡슐제, 정제", "경질캡슐"),
    ("연질캡슐제, 액상", "연질캡슐"),
    ("경질캡슐제, 공캡슐", "경질캡슐"),
    ("경질캡슐제, 과립제", "경질캡슐"),
    ("연질캡슐제, 현탁상", "연질캡슐"),
    ("정량흡입제, 분말제", "기타"),
    ("경질캡슐제, 과립제정제", "경질캡슐"),
    ("경질캡슐제, 장용성과립제", "경질캡슐"),
    ("경질캡슐제, 서방성장용성펠렛", "경질캡슐"),
    ("질좌제, 일반", "기타"),
    ("흡입제, 미분류", "기타"),
    ("정제, 미분류", "정제"),
    ("캡슐, 미분류", "기타"),
)

UPDATE_SQL = """
UPDATE drugs_master AS d
SET form_code_name = v.new_name
FROM (VALUES
    {value_rows}
) AS v(old_name, new_name)
WHERE d.form_code_name IS NOT DISTINCT FROM v.old_name;
"""


def _value_rows_tuples() -> list[tuple[str, str]]:
    """(원본 제형명, 표준 분류명) 튜플 목록. 한 원본은 하나의 분류만."""
    rows: list[tuple[str, str]] = []
    for x in FORM_JEONGJE:
        rows.append((x, "정제"))
    for x in FORM_HARD_CAPSULE:
        rows.append((x, "경질캡슐"))
    for x in FORM_SOFT_CAPSULE:
        rows.append((x, "연질캡슐"))
    for x in FORM_ETC:
        rows.append((x, "기타"))
    rows.extend(FORM_COMPOUND)
    return rows


def main() -> None:
    tuples = _value_rows_tuples()
    old_names = [t[0] for t in tuples]
    if len(old_names) != len(set(old_names)):
        dup = [k for k, v in Counter(old_names).items() if v > 1]
        raise SystemExit(f"매핑에 중복된 원본 제형명이 있습니다: {dup}")

    # VALUES ('a', '정제'), ('b', '경질캡슐'), ...
    parts = []
    for old_name, new_name in tuples:
        escaped_old = old_name.replace("'", "''")
        escaped_new = new_name.replace("'", "''")
        parts.append(f"('{escaped_old}', '{escaped_new}')")
    sql = UPDATE_SQL.format(value_rows=",\n    ".join(parts))

    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT form_code_name, COUNT(*) FROM drugs_master
                GROUP BY form_code_name ORDER BY COUNT(*) DESC
                """
            )
            before = cur.fetchall()
            cur.execute(sql)
            updated = cur.rowcount
            conn.commit()
            cur.execute(
                """
                SELECT form_code_name, COUNT(*) FROM drugs_master
                GROUP BY form_code_name ORDER BY COUNT(*) DESC
                """
            )
            after = cur.fetchall()

    print(f"갱신된 행 수: {updated}")
    print("\n[이전] form_code_name 별 건수 (상위 일부)")
    for row in before[:25]:
        print(f"  {row[0]!r}: {row[1]}")
    if len(before) > 25:
        print(f"  ... 외 {len(before) - 25}종")
    print("\n[이후] form_code_name 별 건수")
    for row in after:
        print(f"  {row[0]!r}: {row[1]}")


if __name__ == "__main__":
    main()
