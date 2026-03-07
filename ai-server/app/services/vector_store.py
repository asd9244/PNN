import psycopg
import json
from app.core.database import get_db_connection

def save_embedding(content: str, embedding: list, metadata: dict):
    """
    knowledge_embeddings 테이블에 임베딩 데이터를 저장합니다.
    """
    insert_query = """
    INSERT INTO knowledge_embeddings (content, embedding, source, category, metadata)
    VALUES (%s, %s, %s, %s, %s)
    """
    
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                insert_query,
                (
                    content,
                    embedding,
                    'drug_easy_info',
                    'drug_interaction',
                    json.dumps(metadata)
                )
            )
        conn.commit()
