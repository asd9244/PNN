"""
한국의약품안전관리원_병용금기약물_20240625.csv → drug_contraindication 테이블 적재
성분명1, 성분명2 기준 매칭 (drug_ingredients와 JOIN 가능)
"""
import csv
import os
import psycopg
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")

CSV_PATH = os.path.join(os.path.dirname(__file__), "../csv_data/한국의약품안전관리원_병용금기약물_20240625.csv")

# CSV: 성분명1,성분코드1,제품코드1,제품명1,업체명1,급여구분1,성분명2,성분코드2,제품코드2,제품명2,업체명2,급여구분2,공고번호,공고일자,금기사유
DB_COLUMNS = [
    "ingr_name_1", "ingr_code_1", "product_code_1", "product_name_1", "entp_name_1", "pay_type_1",
    "ingr_name_2", "ingr_code_2", "product_code_2", "product_name_2", "entp_name_2", "pay_type_2",
    "notice_no", "notice_date", "contraind_reason",
]
PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))
INSERT_SQL = f"""
    INSERT INTO drug_contraindication ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
"""


def _clean(val):
    if val is None or (isinstance(val, str) and val.strip() == ""):
        return None
    s = val.strip() if isinstance(val, str) else str(val)
    return s.replace("\x00", "") if s else None


def _row_to_values(row):
    def v(i):
        return _clean(row[i]) if i < len(row) else None
    return [
        v(0), v(1), v(2), v(3), v(4), v(5),
        v(6), v(7), v(8), v(9), v(10), v(11),
        v(12), v(13), v(14),
    ]


def main():
    if not os.path.exists(CSV_PATH):
        print(f"파일을 찾을 수 없습니다: {CSV_PATH}")
        return

    print(f"1. CSV 읽는 중: {CSV_PATH}")
    rows = []
    skipped = 0
    with open(CSV_PATH, "r", encoding="utf-8-sig") as f:
        reader = csv.reader(f)
        header = next(reader)
        for row in reader:
            if len(row) < 15:
                skipped += 1
                continue
            ingr1 = _clean(row[0])
            ingr2 = _clean(row[6])
            reason = _clean(row[14])
            if not ingr1 or not ingr2 or not reason:
                skipped += 1
                continue
            rows.append(_row_to_values(row))

    total = len(rows)
    print(f"   -> 적재 대상 {total}건 (건너뜀: {skipped})")

    if total == 0:
        print("적재할 데이터가 없습니다.")
        return

    print("2. DB 적재 중...")
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.executemany(INSERT_SQL, rows)
        conn.commit()
    print(f"   -> 적재 완료: {total}건")


if __name__ == "__main__":
    main()
