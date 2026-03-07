import pandas as pd
import psycopg
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

CSV_FILE = "../한국의약품안전관리원_병용금기약물_20240625.csv"

def load_data(limit=None):
    print(f"Loading {CSV_FILE}...")
    
    # 1. CSV 읽기
    try:
        df = pd.read_csv(CSV_FILE, encoding='cp949', dtype=str)
    except Exception as e:
        print(f"Failed to read CSV: {e}")
        return

    # 2. 컬럼명 매핑
    rename_map = {
        '성분명1': 'ingredient_name1',
        '성분코드1': 'ingredient_code1',
        '제품코드1': 'product_code1',
        '제품명1': 'product_name1',
        '업체명1': 'company_name1',
        '성분명2': 'ingredient_name2',
        '성분코드2': 'ingredient_code2',
        '제품코드2': 'product_code2',
        '제품명2': 'product_name2',
        '업체명2': 'company_name2',
        '공고번호': 'notice_no',
        '공고일자': 'notice_date',
        '금기사유': 'reason'
    }
    df = df.rename(columns=rename_map)
    
    # 필요한 컬럼만 남기기 (DB에 없는 컬럼 제외)
    db_cols = list(rename_map.values())
    df = df[db_cols]
    
    # NaN 처리
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
                
                # executemany용 데이터 튜플 리스트 생성
                # DataFrame의 순서가 db_cols 순서와 일치함
                data_to_insert = [tuple(x) for x in df.to_numpy()]

                placeholders = ",".join(["%s"] * len(db_cols))
                columns = ",".join(db_cols)
                
                insert_query = f"""
                    INSERT INTO contraindications ({columns}) 
                    VALUES ({placeholders})
                """

                cur.executemany(insert_query, data_to_insert)
                conn.commit()
                print(f"Successfully inserted {len(data_to_insert)} rows.")

    except Exception as e:
        print(f"DB Error: {e}")

if __name__ == "__main__":
    load_data()
