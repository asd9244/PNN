import pandas as pd
import psycopg
from psycopg import sql
from dotenv import load_dotenv
import os

# .env 로드
load_dotenv(dotenv_path="../../ai-server/.env")

# DB 접속 정보
DB_HOST = "localhost"
DB_PORT = "5432"
DB_NAME = "pnn-db"
DB_USER = "postgres"
DB_PASS = "1234"

# CSV 파일 경로
CSV_FILE = "../건강보험심사평가원_약가마스터_의약품표준코드_20251031.csv"

def load_data(limit=None):
    print(f"Loading {CSV_FILE}...")
    
    # 1. CSV 읽기 (cp949)
    # 필요한 컬럼만 선택해서 읽기 (메모리 절약)
    use_cols = [
        '한글상품명', '업체명', '약품규격', '제형구분', '포장형태', 
        '표준코드', '품목허가일자', '전문일반구분', '대표코드', 
        '제품코드(개정후)', '일반명코드(성분명코드)', '국제표준코드(ATC코드)'
    ]
    
    try:
        df = pd.read_csv(CSV_FILE, encoding='cp949', dtype=str, usecols=use_cols)
    except Exception as e:
        print(f"Failed to read CSV: {e}")
        return

    # 2. 컬럼명 매핑 (CSV 한글명 -> DB 영문명)
    rename_map = {
        '한글상품명': 'item_name',
        '업체명': 'entp_name',
        '약품규격': 'drug_spec',
        '제형구분': 'form_type',
        '포장형태': 'pkg_type',
        '표준코드': 'std_code',
        '품목허가일자': 'permit_date',
        '전문일반구분': 'etc_otc_type',
        '대표코드': 'represent_code',
        '제품코드(개정후)': 'bar_code',
        '일반명코드(성분명코드)': 'ingr_code',
        '국제표준코드(ATC코드)': 'atc_code'
    }
    df = df.rename(columns=rename_map)
    
    # NaN 값을 None으로 변환 (DB NULL 처리를 위해)
    df = df.where(pd.notnull(df), None)

    print(f"Total rows in CSV: {len(df)}")
    
    if limit:
        df = df.head(limit)
        print(f"Running in TEST mode with {limit} rows.")

    # 3. DB 적재
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    
    try:
        with psycopg.connect(conn_str) as conn:
            with conn.cursor() as cur:
                print("Connected to DB.")
                
                # 데이터 리스트 변환
                data_to_insert = [
                    (
                        row['item_name'], row['entp_name'], row['drug_spec'], row['form_type'], row['pkg_type'],
                        row['std_code'], row['permit_date'], row['etc_otc_type'], row['represent_code'],
                        row['bar_code'], row['ingr_code'], row['atc_code']
                    )
                    for _, row in df.iterrows()
                ]

                # Bulk Insert 쿼리
                insert_query = """
                    INSERT INTO drug_price_master (
                        item_name, entp_name, drug_spec, form_type, pkg_type, 
                        std_code, permit_date, etc_otc_type, represent_code, 
                        bar_code, ingr_code, atc_code
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON CONFLICT (std_code) DO NOTHING
                """

                # 실행
                cur.executemany(insert_query, data_to_insert)
                conn.commit()
                print(f"Successfully inserted {len(data_to_insert)} rows.")

    except Exception as e:
        print(f"DB Error: {e}")

if __name__ == "__main__":
    # 테스트용으로 1000개만 먼저 실행해보고 싶으면 load_data(1000)
    # 전체 실행하려면 load_data()
    load_data() 
