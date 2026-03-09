import psycopg
from psycopg.rows import dict_row
from app.core.config import settings


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
