import psycopg
import json
from typing import List, Optional
from app.core.database import get_db_connection


def search_embeddings(
    query_embedding: List[float],
    sources: Optional[List[str]] = None,
    top_k: int = 10,
    min_similarity: float = 0.0,
) -> List[dict]:
    """
    knowledge_embeddings에서 유사도 검색.
    sources: 검색할 source 목록 (예: ['drug_easy_info', 'ingredient_efficacy']). None이면 전체.
    cosine similarity 기준 상위 top_k건 반환.
    """
    # pgvector cosine distance: 1 - (embedding <=> query) = similarity
    if sources:
        sql = """
            SELECT content, metadata, source, category,
                   1 - (embedding <=> %s::vector) AS similarity
            FROM knowledge_embeddings
            WHERE source = ANY(%s) AND (1 - (embedding <=> %s::vector)) >= %s
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """
        params = (query_embedding, sources, query_embedding, min_similarity, query_embedding, top_k)
    else:
        sql = """
            SELECT content, metadata, source, category,
                   1 - (embedding <=> %s::vector) AS similarity
            FROM knowledge_embeddings
            WHERE (1 - (embedding <=> %s::vector)) >= %s
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """
        params = (query_embedding, query_embedding, min_similarity, query_embedding, top_k)

    with get_db_connection() as conn:
        with conn.cursor() as cur:
            # embedding을 PostgreSQL vector 형식 문자열로 변환
            vec_str = "[" + ",".join(str(float(x)) for x in query_embedding) + "]"
            if sources:
                cur.execute(sql, (vec_str, sources, vec_str, min_similarity, vec_str, top_k))
            else:
                cur.execute(sql, (vec_str, vec_str, min_similarity, vec_str, top_k))
            rows = cur.fetchall()

    results = []
    for r in rows:
        meta = r.get("metadata") or {}
        if isinstance(meta, str):
            meta = json.loads(meta) if meta else {}
        results.append({
            "content": r.get("content", ""),
            "metadata": meta,
            "source": r.get("source"),
            "category": r.get("category"),
            "similarity": float(r["similarity"]) if r.get("similarity") is not None else 0.0,
        })
    return results


def clear_embeddings_by_source(source: str):
    """
    knowledge_embeddings에서 지정된 source의 데이터를 삭제합니다.
    재적재 전 기존 데이터 정리용.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM knowledge_embeddings WHERE source = %s", (source,))
        conn.commit()

def save_embedding(content: str, embedding: list, metadata: dict, source: str = 'drug_easy_info', category: str = 'drug_interaction'):
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
                    source,
                    category,
                    json.dumps(metadata)
                )
            )
        conn.commit()
