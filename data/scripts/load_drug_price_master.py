"""
건강보험심사평가원_약가마스터_의약품표준코드_20251031.csv → drug_price_master 테이블 적재 스크립트
심평원의 일반명코드(주성분코드) 등을 `drugs_master`와 연결하기 위해 활용합니다.
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

CSV_PATH = os.path.join(os.path.dirname(__file__), "../csv_data/건강보험심사평가원_약가마스터_의약품표준코드_20251031.csv")

# CSV 한글 헤더 → DB 영문 컬럼 매핑 (DictReader를 위해 키는 실제 CSV 헤더명과 동일하게 설정)
COLUMN_MAP = {
    "품목기준코드": "item_seq",
    "한글상품명": "item_name",
    "업체명": "entp_name",
    "약품규격": "pkg_spec",
    "제형구분": "formulation",
    "포장형태": "pkg_form",
    "대표코드": "rep_code",
    "표준코드": "standard_code",
    "제품코드(개정후)": "insur_code",
    "일반명코드(성분명코드)": "main_ingr_code",
    "국제표준코드(ATC코드)": "atc_code",
    "취소일자": "cancel_date",
    "비고": "remark"
}

DB_COLUMNS = list(COLUMN_MAP.values())
PLACEHOLDERS = ", ".join(["%s"] * len(DB_COLUMNS))

# item_seq가 중복될 수 있는 1:N 구조 (한 품목당 여러 표준코드가 존재함)
# 따라서 CONFLICT 체크 없이 단순 Insert 하거나, 고유 식별자가 필요하다면 standard_code로 처리해야 함.
# 여기서는 1:N 그대로 적재합니다.
INSERT_SQL = f"""
    INSERT INTO drug_price_master ({", ".join(DB_COLUMNS)})
    VALUES ({PLACEHOLDERS})
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

    with open(CSV_PATH, "r", encoding="utf-8-sig") as f:  # BOM 제거
        reader = csv.DictReader(f)
        
        for line_no, row in enumerate(reader, start=2):
            # item_seq 값이 없으면 건너뜀
            if not _clean(row.get("품목기준코드")):
                skipped_null += 1
                continue
            
            # COLUMN_MAP에 정의된 컬럼 순서대로 값을 추출
            values = []
            for csv_header in COLUMN_MAP.keys():
                val = _clean(row.get(csv_header))
                # 지수 표현식으로 된 코드들(예: 8.800630e+12)을 정상적인 숫자 문자열로 변환
                if csv_header in ["대표코드", "표준코드"] and val:
                    try:
                        # 부동소수점으로 변환 후 정수로 바꾸고 다시 문자열로 (e.g., "8.80063E+12" -> 8800630000000 -> "8800630000000")
                        # CSV 원본 데이터가 '8.80658E+12' 형태의 텍스트일 수 있으므로 처리
                        val = str(int(float(val)))
                    except ValueError:
                        pass # 변환 실패시 원본 값 유지
                values.append(val)
            
            rows.append(tuple(values))

    total = len(rows)
    print(f"   -> 총 {total}건 로드 완료 (item_seq 누락: {skipped_null}건 건너뜀)")

    if total == 0:
        print("적재할 데이터가 없습니다.")
        return

    print("2. DB 적재 중...")
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            # 매번 테이블 비우고 시작 (Idempotent)
            cur.execute("TRUNCATE TABLE drug_price_master RESTART IDENTITY;")
            print("   -> 기존 테이블 초기화 완료")
            
            # 대량 Insert (execute_values 대신 executemany 사용)
            cur.executemany(INSERT_SQL, rows)
            
        conn.commit()

    print(f"   -> 적재 완료: {total}건")

if __name__ == "__main__":
    main()
