"""
DUR유형별 성분 현황_*.csv 6종 → dur_rules 통합 테이블 적재
노인주의, 용량주의, 임산부금기, 투여기간주의, 특정연령대금기, 효능군중복주의
(병용금기: drug_contraindication 사용, 첨가제주의: 제외)
"""
import csv
import json
import os
import glob
import psycopg
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")

CSV_DIR = os.path.join(os.path.dirname(__file__), "../csv_data")
DUR_PATTERN = os.path.join(CSV_DIR, "DUR유형별 성분 현황_*.csv")

# 적재 대상 6개 유형 (병용금기, 첨가제주의 제외)
ALLOWED_TYPES = {"노인주의", "용량주의", "임산부금기", "투여기간주의", "특정연령대금기", "효능군중복주의"}

INSERT_SQL = """
    INSERT INTO dur_rules (dur_type, product_code, ingr_code, ingr_name, warning_text, raw_data)
    VALUES (%s, %s, %s, %s, %s, %s::jsonb)
"""


def _clean(val):
    if val is None or (isinstance(val, str) and val.strip() == ""):
        return None
    s = val.strip() if isinstance(val, str) else str(val)
    return s.replace("\x00", "") if s else None


def _get_product_code(row, headers):
    """제품코드 컬럼명 변형 대응"""
    for key in ("제품코드", "product_code"):
        if key in row:
            return _clean(row[key])
    return None


def _get_ingr_name(row):
    for key in ("성분명", "ingr_name"):
        if key in row:
            return _clean(row[key])
    return None


def _get_ingr_code(row):
    for key in ("성분코드", "ingr_code"):
        if key in row:
            return _clean(row[key])
    return None


def _build_warning_text(dur_type, row):
    """파일 유형별 warning_text 생성"""
    if dur_type == "노인주의":
        return _clean(row.get("약품상세정보"))
    if dur_type == "용량주의":
        max_dose = _clean(row.get("1일최대투여량"))
        std = _clean(row.get("1일최대 투여기준량"))
        if max_dose and std:
            return f"1일 최대 {max_dose} (기준량 {std}) 초과 주의"
        return max_dose or std
    if dur_type == "임산부금기":
        return _clean(row.get("상세정보"))
    if dur_type == "투여기간주의":
        days = _clean(row.get("최대투여기간일수"))
        if days:
            return f"{days}일 초과 투여 주의"
        return days
    if dur_type == "특정연령대금기":
        return _clean(row.get("상세정보"))
    if dur_type == "효능군중복주의":
        return _clean(row.get("효능군"))
    return None


def _process_file(path, dur_type):
    """단일 CSV 파일 처리 → (dur_type, product_code, ingr_code, ingr_name, warning_text, raw_data) 리스트"""
    rows = []
    with open(path, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        headers = reader.fieldnames or []
        for row in reader:
            product_code = _get_product_code(row, headers)
            if not product_code:
                continue
            warning_text = _build_warning_text(dur_type, row)
            ingr_name = _get_ingr_name(row)
            ingr_code = _get_ingr_code(row)
            raw_data = json.dumps(row, ensure_ascii=False)
            rows.append((dur_type, product_code, ingr_code, ingr_name, warning_text, raw_data))
    return rows


def main():
    files = sorted(glob.glob(DUR_PATTERN))
    if not files:
        print(f"파일을 찾을 수 없습니다: {DUR_PATTERN}")
        return

    def get_dur_type(path):
        base = os.path.basename(path)
        return base.replace("DUR유형별 성분 현황_", "").replace(".csv", "")

    all_rows = []
    for path in files:
        dur_type = get_dur_type(path)
        if dur_type not in ALLOWED_TYPES:
            print(f"  건너뜀: {os.path.basename(path)} ({dur_type} - 적재 대상 아님)")
            continue
        print(f"  읽는 중: {os.path.basename(path)} ({dur_type})")
        rows = _process_file(path, dur_type)
        all_rows.extend(rows)
        print(f"    → {len(rows)}건")

    total = len(all_rows)
    print(f"\n적재 대상: {total}건")

    if total == 0:
        print("적재할 데이터가 없습니다.")
        return

    print("DB 적재 중...")
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.executemany(INSERT_SQL, all_rows)
        conn.commit()
    print(f"적재 완료: {total}건")


if __name__ == "__main__":
    main()
