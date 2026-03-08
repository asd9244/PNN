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
    drug_ingredients와 조인하여 성분명(ingredient_names)을 포함합니다.
    상호작용/주의사항/효능이 있는 데이터만 조회합니다.
    """
    query = """
    SELECT 
        dei.item_seq,
        dei.item_name,
        dei.entp_name,
        dei.efcy_qesitm,
        dei.use_method_qesitm,
        dei.atpn_warn_qesitm,
        dei.atpn_qesitm,
        dei.intrc_qesitm,
        dei.se_qesitm,
        dei.deposit_method_qesitm,
        COALESCE(STRING_AGG(DISTINCT di.mtral_nm, ', ') FILTER (WHERE di.mtral_nm IS NOT NULL), '') AS ingredient_names
    FROM drug_easy_info dei
    LEFT JOIN drugs d ON dei.item_seq = d.item_seq
    LEFT JOIN drug_ingredients di ON d.id = di.drug_id
    WHERE dei.intrc_qesitm IS NOT NULL 
       OR dei.atpn_qesitm IS NOT NULL
       OR dei.efcy_qesitm IS NOT NULL
    GROUP BY dei.id, dei.item_seq, dei.item_name, dei.entp_name,
             dei.efcy_qesitm, dei.use_method_qesitm, dei.atpn_warn_qesitm,
             dei.atpn_qesitm, dei.intrc_qesitm, dei.se_qesitm, dei.deposit_method_qesitm;
    """

    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(query)
            results = cur.fetchall()
            return results


def fetch_ingredient_efficacy() -> List[Dict[str, Any]]:
    """
    ingredient_efficacy 테이블에서 임베딩에 필요한 데이터를 조회합니다.
    RAG 지식 베이스 보강용 (성분 + 약효분류).
    """
    query = """
    SELECT gnl_nm_cd, gnl_nm, meft_div_no, div_nm, fomn_tp_nm, injc_pth_nm, iqty_txt, unit
    FROM ingredient_efficacy
    """
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(query)
            results = cur.fetchall()
            return results
