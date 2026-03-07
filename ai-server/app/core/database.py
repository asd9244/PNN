import psycopg
from psycopg.rows import dict_row
from app.core.config import settings
from typing import List, Dict, Any

def get_db_connection():
    """
    PostgreSQL 데이터베이스 연결을 생성하고 반환합니다.
    settings.DATABASE_URL을 사용합니다.
    """
    try:
        conn = psycopg.connect(settings.DATABASE_URL, row_factory=dict_row)
        return conn
    except Exception as e:
        print(f"Database connection error: {e}")
        raise e

def fetch_drug_easy_info() -> List[Dict[str, Any]]:
    """
    drug_easy_info 테이블에서 임베딩에 필요한 데이터를 조회합니다.
    상호작용 정보(intrc_qesitm) 또는 주의사항(atpn_qesitm)이 있는 데이터만 조회합니다.
    """
    query = """
    SELECT 
        item_seq,
        item_name,
        entp_name,
        efcy_qesitm,
        use_method_qesitm,
        atpn_warn_qesitm,
        atpn_qesitm,
        intrc_qesitm,
        se_qesitm,
        deposit_method_qesitm
    FROM drug_easy_info
    WHERE intrc_qesitm IS NOT NULL 
       OR atpn_qesitm IS NOT NULL
       OR efcy_qesitm IS NOT NULL;
    """
    
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(query)
            results = cur.fetchall()
            return results
