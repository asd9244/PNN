"""
의약품 제품허가 상세정보.csv → drug_permit_detail 테이블 적재 스크립트
품목일련번호(item_seq) 기준, drugs_master와 무관하게 전체 적재 (낱알식별에 없는 품목 포함)
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

CSV_PATH = os.path.join(os.path.dirname(__file__), "../csv_data/의약품 제품허가 상세정보.csv")

# CSV 컬럼 인덱스 (0~37): 품목명,품목 영문명,품목일련번호,허가/신고구분,취소상태,취소일자,변경일자,업체명,업체 영문명,허가일자,업체허가번호,전문일반,성상,표준코드,원료성분,영문성분명,효능효과,용법용량,주의사항,첨부문서,저장방법,유효기간,재심사대상,재심사기간,포장단위,보험코드,마약류분류,완제원료구분,신약여부,업종구분,변경내용,총량,주성분명,첨가제명,ATC코드,사업자번호,희귀의약품여부,위탁제조업체
N_COLS = 38

DB_COLUMNS = [
    "item_seq", "item_name", "item_eng_name", "entp_name", "entp_eng_name",
    "bizrno", "permit_no", "permit_type", "cancel_status", "cancel_date",
    "change_date", "permit_date", "etc_otc_code", "narcotic_class",
    "finished_raw_type", "new_drug_yn", "biz_type", "rare_drug_yn", "consign_entp",
    "raw_ingredients", "ingr_name_eng", "main_ingr_name", "additive_name",
    "atc_code", "total_qty", "efficacy", "dosage", "caution", "change_history",
    "appearance", "storage_method", "validity_period", "package_unit",
    "attach_doc", "reexam_target", "reexam_period", "std_code", "insur_code",
]

PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))
INSERT_SQL = f"""
    INSERT INTO drug_permit_detail ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
"""


def _clean(val):
    if val is None or (isinstance(val, str) and val.strip() == ""):
        return None
    return val.strip() if isinstance(val, str) else val


def _row_to_values(row):
    """CSV row(38 cols) → DB values (38 cols) 순서 매핑"""
    def v(i):
        return _clean(row[i]) if i < len(row) else None
    return [
        v(2),   # item_seq
        v(0),   # item_name
        v(1),   # item_eng_name
        v(7),   # entp_name
        v(8),   # entp_eng_name
        v(35),  # bizrno
        v(10),  # permit_no
        v(3),   # permit_type
        v(4),   # cancel_status
        v(5),   # cancel_date
        v(6),   # change_date
        v(9),   # permit_date
        v(11),  # etc_otc_code
        v(26),  # narcotic_class
        v(27),  # finished_raw_type
        v(28),  # new_drug_yn
        v(29),  # biz_type
        v(36),  # rare_drug_yn
        v(37),  # consign_entp
        v(14),  # raw_ingredients
        v(15),  # ingr_name_eng
        v(32),  # main_ingr_name
        v(33),  # additive_name
        v(34),  # atc_code
        v(31),  # total_qty
        v(16),  # efficacy
        v(17),  # dosage
        v(18),  # caution
        v(30),  # change_history
        v(12),  # appearance
        v(20),  # storage_method
        v(21),  # validity_period
        v(24),  # package_unit
        v(19),  # attach_doc
        v(22),  # reexam_target
        v(23),  # reexam_period
        v(13),  # std_code
        v(25),  # insur_code
    ]


def main():
    if not os.path.exists(CSV_PATH):
        print(f"파일을 찾을 수 없습니다: {CSV_PATH}")
        return

    print(f"1. CSV 읽는 중: {CSV_PATH}")
    rows = []
    skipped_null = 0
    skipped_cols = 0
    with open(CSV_PATH, "r", encoding="utf-8-sig") as f:
        reader = csv.reader(f)
        header = next(reader)
        for row in reader:
            if len(row) < N_COLS:
                skipped_cols += 1
                continue
            item_seq = _clean(row[2])
            item_name = _clean(row[0])
            if not item_seq or not item_name:
                skipped_null += 1
                continue
            rows.append(_row_to_values(row))

    total = len(rows)
    print(f"   -> 적재 대상 {total}건 (item_seq/item_name 누락: {skipped_null}, 컬럼 부족: {skipped_cols}건 건너뜀)")

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
