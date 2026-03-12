"""
의약품 낱알식별.csv → drugs_master 테이블 적재 스크립트
한글 컬럼명을 영문 DB 컬럼으로 자동 매핑하여 bulk insert 수행
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

CSV_PATH = os.path.join(os.path.dirname(__file__), "../csv_data/의약품 낱알식별.csv")

# CSV 한글 헤더 → DB 영문 컬럼 매핑 (순서 유지)
COLUMN_MAP = [
    ("품목일련번호", "item_seq"),
    ("품목명", "item_name"),
    ("업소일련번호", "entp_seq"),
    ("업소명", "entp_name"),
    ("성상", "appearance"),
    ("큰제품이미지", "item_image_url"),
    ("표시앞", "print_front"),
    ("표시뒤", "print_back"),
    ("의약품제형", "drug_shape"),
    ("색상앞", "color_front"),
    ("색상뒤", "color_back"),
    ("분할선앞", "line_front"),
    ("분할선뒤", "line_back"),
    ("크기장축", "length_long"),
    ("크기단축", "length_short"),
    ("크기두께", "thickness"),
    ("이미지생성일자(약학정보원)", "img_regist_date"),
    ("분류번호", "class_no"),
    ("분류명", "class_name"),
    ("전문일반구분", "etc_otc_code"),
    ("품목허가일자", "permit_date"),
    ("제형코드명", "form_code_name"),
    ("표기내용앞", "mark_text_front"),
    ("표기내용뒤", "mark_text_back"),
    ("표기이미지앞", "mark_img_front"),
    ("표기이미지뒤", "mark_img_back"),
    ("표기코드앞", "mark_code_front"),
    ("표기코드뒤", "mark_code_back"),
    ("변경일자", "change_date"),
    ("사업자번호", "bizrno"),
    ("품목영문명", "item_eng_name"),
    ("보험코드", "insur_code"),
    ("표준코드", "std_code"),
]

DB_COLUMNS = [col[1] for col in COLUMN_MAP]
CSV_HEADERS = [col[0] for col in COLUMN_MAP]
PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))
INSERT_SQL = f"""
    INSERT INTO drugs_master ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
    ON CONFLICT (item_seq) DO NOTHING
"""


def _clean(val):
    """빈 문자열을 None으로, 앞뒤 공백 제거"""
    if val is None or (isinstance(val, str) and val.strip() == ""):
        return None
    return val.strip() if isinstance(val, str) else val


def main():
    if not os.path.exists(CSV_PATH):
        print(f"파일을 찾을 수 없습니다: {CSV_PATH}")
        return

    print(f"1. CSV 읽는 중: {CSV_PATH}")
    rows = []
    skipped_null = 0
    skipped_cols = 0
    with open(CSV_PATH, "r", encoding="utf-8-sig") as f:  # BOM 제거
        reader = csv.reader(f)
        header = next(reader)
        n_cols = len(CSV_HEADERS)
        for line_no, row in enumerate(reader, start=2):
            if len(row) < n_cols:
                skipped_cols += 1
                continue
            values = [_clean(row[i]) if i < len(row) else None for i in range(n_cols)]
            if not values[0]:  # item_seq NOT NULL
                skipped_null += 1
                continue
            rows.append(values)

    total = len(rows)
    print(f"   -> 총 {total}건 로드 완료 (item_seq 누락: {skipped_null}건, 컬럼 부족: {skipped_cols}건 건너뜀)")

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
