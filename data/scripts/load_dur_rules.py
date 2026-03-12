"""
DUR유형별 성분 현황_*.csv 8종 → dur_rules 테이블 적재
병용금기, 투여기간주의, 용량주의, 임산부금기, 첨가제주의, 효능군중복주의, 노인주의, 특정연령대금기
"""
import csv
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

DB_COLUMNS = [
    "dur_seq", "dur_type", "single_complex_code", "dur_ingr_code", "dur_ingr_name_eng", "dur_ingr_name",
    "complex_drug", "related_ingr", "efficacy_class_code", "efficacy_group", "notice_date", "contraind_content",
    "dosage_form", "age_criteria", "max_duration", "max_daily_dose", "grade", "note", "status", "series_name",
    "contraind_single_complex_code", "contraind_dur_ingr_code", "contraind_dur_ingr_name_eng", "contraind_dur_ingr_name",
    "contraind_complex_drug", "contraind_related_ingr", "contraind_efficacy_class",
]
PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))
INSERT_SQL = f"""
    INSERT INTO dur_rules ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
"""


def _clean(val):
    if val is None or (isinstance(val, str) and val.strip() == ""):
        return None
    s = val.strip() if isinstance(val, str) else str(val)
    return s.replace("\x00", "") if s else None


def _row_to_values(row, dur_type):
    def v(i):
        return _clean(row[i]) if i < len(row) else None
    return [
        v(0),   # dur_seq
        dur_type,  # dur_type (파일명에서 추출)
        v(2),   # single_complex_code
        v(3),   # dur_ingr_code
        v(4),   # dur_ingr_name_eng
        v(5),   # dur_ingr_name
        v(6),   # complex_drug
        v(7),   # related_ingr
        v(8),   # efficacy_class_code
        v(9),   # efficacy_group
        v(10),  # notice_date
        v(11),  # contraind_content
        v(12),  # dosage_form
        v(13),  # age_criteria
        v(14),  # max_duration
        v(15),  # max_daily_dose
        v(16),  # grade
        v(24),  # note
        v(25),  # status
        v(26),  # series_name
        v(17),  # contraind_single_complex_code
        v(18),  # contraind_dur_ingr_code
        v(19),  # contraind_dur_ingr_name_eng
        v(20),  # contraind_dur_ingr_name
        v(21),  # contraind_complex_drug
        v(22),  # contraind_related_ingr
        v(23),  # contraind_efficacy_class
    ]


def main():
    files = sorted(glob.glob(DUR_PATTERN))
    if not files:
        print(f"파일을 찾을 수 없습니다: {DUR_PATTERN}")
        return

    # 파일명에서 DUR유형 추출: "DUR유형별 성분 현황_병용금기.csv" -> "병용금기"
    def get_dur_type(path):
        base = os.path.basename(path)
        return base.replace("DUR유형별 성분 현황_", "").replace(".csv", "")

    rows = []
    for path in files:
        dur_type = get_dur_type(path)
        print(f"  읽는 중: {os.path.basename(path)} ({dur_type})")
        with open(path, "r", encoding="utf-8-sig") as f:
            reader = csv.reader(f)
            header = next(reader)
            for row in reader:
                if len(row) < 20:
                    continue
                dur_seq = _clean(row[0])
                dur_type_row = _clean(row[1])
                if not dur_seq:
                    continue
                # 파일명 기준 dur_type 사용 (row[1]과 동일해야 함)
                rows.append(_row_to_values(row, dur_type_row or dur_type))

    total = len(rows)
    print(f"\n적재 대상: {total}건")

    if total == 0:
        print("적재할 데이터가 없습니다.")
        return

    print("DB 적재 중...")
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.executemany(INSERT_SQL, rows)
        conn.commit()
    print(f"적재 완료: {total}건")


if __name__ == "__main__":
    main()
