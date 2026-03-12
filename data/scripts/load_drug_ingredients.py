"""
의약품 제품 허가정보_의약품 상세조회(주성분).csv → drug_ingredients 테이블 적재 스크립트
품목기준코드(item_seq) = drugs_master.item_seq FK → drugs_master에 없는 품목은 건너뜀
"""
import csv
import os
import psycopg
from dotenv import load_dotenv

# --- 설정 ---
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")

CSV_PATH = os.path.join(os.path.dirname(__file__), "../csv_data/의약품 제품 허가정보_의약품 상세조회(주성분).csv")

# CSV 컬럼 순서: 업소명(0),품목명(1),성분명한글(2),사업자번호(3),업허가번호(4),순번(5),성분영문명(6),성분코드(7),단위(8),품목기준코드(9),원료분량(10),총량순번(11)
DB_COLUMNS = ["item_seq", "entp_name", "item_name", "ingr_name_kr", "ingr_name_eng", "ingr_code", "unit", "raw_qty", "seq_no", "total_seq", "bizrno", "permit_no"]
N_COLS = 12

PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))
INSERT_SQL = f"""
    INSERT INTO drug_ingredients ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
"""


def _clean(val):
    if val is None or (isinstance(val, str) and val.strip() == ""):
        return None
    return val.strip() if isinstance(val, str) else val


def main():
    if not os.path.exists(CSV_PATH):
        print(f"파일을 찾을 수 없습니다: {CSV_PATH}")
        return

    print(f"1. drugs_master에 존재하는 item_seq 목록 조회 중...")
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT item_seq FROM drugs_master")
            valid_item_seqs = {row[0] for row in cur.fetchall()}
    print(f"   -> {len(valid_item_seqs)}개 품목 존재")

    print(f"2. CSV 읽는 중: {CSV_PATH}")
    rows = []
    skipped_null = 0
    skipped_cols = 0
    skipped_fk = 0
    with open(CSV_PATH, "r", encoding="utf-8-sig") as f:
        reader = csv.reader(f)
        header = next(reader)
        for row in reader:
            if len(row) < N_COLS:
                skipped_cols += 1
                continue
            item_seq = _clean(row[9])
            if not item_seq:
                skipped_null += 1
                continue
            if item_seq not in valid_item_seqs:
                skipped_fk += 1
                continue
            # DB 컬럼 순서: item_seq, entp_name, item_name, ingr_name_kr, ingr_name_eng, ingr_code, unit, raw_qty, seq_no, total_seq, bizrno, permit_no
            values = [
                _clean(row[9]),   # item_seq
                _clean(row[0]),   # entp_name
                _clean(row[1]),   # item_name
                _clean(row[2]),   # ingr_name_kr
                _clean(row[6]),   # ingr_name_eng
                _clean(row[7]),   # ingr_code
                _clean(row[8]),   # unit
                _clean(row[10]),  # raw_qty
                _clean(row[5]),   # seq_no
                _clean(row[11]),  # total_seq
                _clean(row[3]),   # bizrno
                _clean(row[4]),   # permit_no
            ]
            rows.append(values)

    total = len(rows)
    print(f"   -> 적재 대상 {total}건 (item_seq 누락: {skipped_null}, 컬럼 부족: {skipped_cols}, drugs_master 미존재: {skipped_fk}건 건너뜀)")

    if total == 0:
        print("적재할 데이터가 없습니다.")
        return

    print("3. DB 적재 중...")
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.executemany(INSERT_SQL, rows)
        conn.commit()

    print(f"   -> 적재 완료: {total}건")


if __name__ == "__main__":
    main()
