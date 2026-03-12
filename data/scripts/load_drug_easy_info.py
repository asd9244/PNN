"""
e약은요정보.csv → drug_easy_info 테이블 적재
품목일련번호 = item_seq (drugs_master와 JOIN 가능)
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

CSV_PATH = os.path.join(os.path.dirname(__file__), "../csv_data/e약은요정보.csv")

# CSV: 품목일련번호,제품명,업체명,효능,사용법,사용전알아야할내용,사용상주의사항,약/음식상호작용,이상반응,보관법,공개일자,수정일자,낱알이미지,사업자번호
DB_COLUMNS = [
    "item_seq", "product_name", "entp_name", "bizrno",
    "efficacy", "dosage", "before_use", "caution_use", "interaction_drug_food", "adverse_reaction", "storage",
    "publish_date", "modify_date", "pill_image_url",
]
PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))
INSERT_SQL = f"""
    INSERT INTO drug_easy_info ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
    ON CONFLICT (item_seq) DO UPDATE SET
        product_name = EXCLUDED.product_name,
        entp_name = EXCLUDED.entp_name,
        bizrno = EXCLUDED.bizrno,
        efficacy = EXCLUDED.efficacy,
        dosage = EXCLUDED.dosage,
        before_use = EXCLUDED.before_use,
        caution_use = EXCLUDED.caution_use,
        interaction_drug_food = EXCLUDED.interaction_drug_food,
        adverse_reaction = EXCLUDED.adverse_reaction,
        storage = EXCLUDED.storage,
        publish_date = EXCLUDED.publish_date,
        modify_date = EXCLUDED.modify_date,
        pill_image_url = EXCLUDED.pill_image_url
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
        v(0),   # item_seq
        v(1),   # product_name
        v(2),   # entp_name
        v(13),  # bizrno (마지막 컬럼)
        v(3),   # efficacy
        v(4),   # dosage
        v(5),   # before_use
        v(6),   # caution_use
        v(7),   # interaction_drug_food
        v(8),   # adverse_reaction
        v(9),   # storage
        v(10),  # publish_date
        v(11),  # modify_date
        v(12),  # pill_image_url
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
            if len(row) < 14:
                skipped += 1
                continue
            item_seq = _clean(row[0])
            product_name = _clean(row[1])
            if not item_seq or not product_name:
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
